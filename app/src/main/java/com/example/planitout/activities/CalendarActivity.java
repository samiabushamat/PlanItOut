package com.example.planitout.activities;

// Import statements
import static android.app.PendingIntent.getActivity;
import static com.kizitonwose.calendar.core.ExtensionsKt.firstDayOfWeekFromLocale;
import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.planitout.BuildConfig;
import com.example.planitout.R;
import com.example.planitout.ViewContainer.DayViewContainer;
import com.example.planitout.adapters.CalendarEventAdapter;
import com.example.planitout.models.CalendarEvent;
import com.example.planitout.models.DotIndicator;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;

// Calendar class
public class CalendarActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<CalendarEvent> eventList;
    private List<CalendarEvent> dayEvents;
    private List<CalendarEvent> createdEvents;
    private List<CalendarEvent> attendeeEvents;
    private CalendarEventAdapter eventAdapter;
    private CalendarView calendarView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<DotIndicator> dotIndicators;
    private TextView monthTextView;
    private LocalDate selectedDate = LocalDate.now();
    String key = BuildConfig.GOOGLE_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        recyclerView = findViewById(R.id.recyclerView_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        dayEvents = new ArrayList<>();
        createdEvents = new ArrayList<>();
        attendeeEvents = new ArrayList<>();
        dotIndicators = new ArrayList<>();
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadEventsFromFirestore();
        calendarView = findViewById(R.id.simpleCalendarView);
        monthTextView = findViewById(R.id.monthTextView);
        recyclerView.setAdapter(eventAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                eventList.clear();
                createdEvents.clear();
                attendeeEvents.clear();
                dayEvents.clear();
                dotIndicators.clear();
                loadEventsFromFirestore();

                LocalDate today = LocalDate.now();
                YearMonth currentMonth = YearMonth.now();
                calendarView.scrollToMonth(currentMonth);

                selectDate(today);
                populateEventList(
                        String.valueOf(today.getYear()),
                        String.valueOf(today.getMonthValue()),
                        String.valueOf(today.getDayOfMonth())
                );

                eventAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        calendarView.setMonthScrollListener(month -> {
            int year = month.getYearMonth().getYear();

            String formatted = month.getYearMonth().getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, Locale.getDefault()) + " " + year;

            monthTextView.setText(formatted);
            return null;
        });

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            private Context CalendarActivity;
            private CalendarDay selectedDay = null;

            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));

                if (day.getPosition() == DayPosition.MonthDate) {
                    container.textView.setVisibility(View.VISIBLE);
                    container.dotCreated.setVisibility(View.GONE);
                    container.dotAttendee.setVisibility(View.GONE);
                    container.dotBoth.setVisibility(View.GONE);

                    for (DotIndicator di : dotIndicators) {
                        if (di.date.equals(day.getDate())) {
                            if (di.attendeeOrCreator == 2) {
                                container.dotBoth.setVisibility(View.VISIBLE);
                            } else if (di.attendeeOrCreator == 1) {
                                container.dotCreated.setVisibility(View.VISIBLE);
                            } else {
                                container.dotAttendee.setVisibility(View.VISIBLE);
                            }
                            break;
                        }
                    }

                    boolean isSelected = day.getDate().equals(selectedDate);
                    container.getView().setSelected(isSelected);

                    container.getView().setOnClickListener(v -> {
                        selectedDate = day.getDate();
                        selectedDay = day;
                        calendarView.notifyCalendarChanged();
                        calendarView.notifyDayChanged(day);

                        dayEvents.clear();

                        populateEventList(
                                String.valueOf(day.getDate().getYear()),
                                String.valueOf(day.getDate().getMonthValue()),
                                String.valueOf(day.getDate().getDayOfMonth())
                        );

                        updateEventAdapter();
                    });

                } else {
                    container.textView.setVisibility(View.INVISIBLE);
                    container.dotCreated.setVisibility(View.GONE);
                    container.dotAttendee.setVisibility(View.GONE);
                    container.dotBoth.setVisibility(View.GONE);
                    container.getView().setOnClickListener(null);
                    container.getView().setSelected(false);
                }
            }

            private void updateEventAdapter() {
                if (eventAdapter == null) {
                    eventAdapter = new CalendarEventAdapter(CalendarActivity.this, dayEvents, eventId -> {
                        Intent intent = new Intent(CalendarActivity.this, EventDetailActivity.class);
                        intent.putExtra("eventId", eventId);
                        startActivity(intent);
                    });
                    recyclerView.setAdapter(eventAdapter);
                } else {
                    eventAdapter.notifyDataSetChanged();
                }
            }
        });

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        DayOfWeek firstDayOfWeek = firstDayOfWeekFromLocale();
        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        // select today's date and load events
        LocalDate today = LocalDate.now();
        selectDate(today);

        populateEventList(
                String.valueOf(today.getYear()),
                String.valueOf(today.getMonthValue()),
                String.valueOf(today.getDayOfMonth())
        );
        updateEventAdapter();
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        calendarView.notifyCalendarChanged();

        // this will force the calendar to refresh and show the selection
        calendarView.post(() -> {
            // find the CalendarDay that corresponds to our date
            CalendarDay day = new CalendarDay(date, DayPosition.MonthDate);
            calendarView.notifyDayChanged(day);
        });
    }

    private void populateEventList(String selectedYear, String selectedMonth, String selectedDay) {
        dayEvents.clear();

        String[] monthNames = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        int monthIndex = Integer.parseInt(selectedMonth) - 1;
        String monthName = monthNames[monthIndex];

        // Ensure day has two digits
        if (selectedDay.length() == 1) selectedDay = "0" + selectedDay;

        // Format selected date for comparison
        String selectedFormattedDate = monthName + ", " + selectedDay + " " + selectedYear;

        // Loop through the event list and add relevant events for the selected day
        for (CalendarEvent event : eventList) {
            String fullDateTime = event.getDate();
            String[] split = fullDateTime.split(" ")[0].split("-");
            String day = split[0];
            String month = split[1];
            String year = split[2];
            String CreatedBy = event.getCreatedBy();

            LocalTime startTime = event.getStartTime();

            int eventMonthIndex = Integer.parseInt(month) - 1;
            String eventMonthName = monthNames[eventMonthIndex];

            if (day.length() == 1) day = "0" + day;

            // Format the event date for comparison
            String eventFormattedDate = eventMonthName + ", " + day + " " + year;

            // If the event date matches the selected date, add it to the day's events
            if (eventFormattedDate.equals(selectedFormattedDate)) {

                // Add event to the day's event list with dynamic flags
                dayEvents.add(new CalendarEvent(
                        event.getId(),
                        event.getTitle(),
                        year,
                        day,
                        month,
                        eventFormattedDate,
                        event.getLocation(),
                        startTime,
                        CreatedBy
                ));

            }
        }
        updateEventAdapter();
    }

    private void updateEventAdapter() {
        if (eventAdapter == null) {
            eventAdapter = new CalendarEventAdapter(CalendarActivity.this, dayEvents, eventId -> {
                Intent intent = new Intent(CalendarActivity.this, EventDetailActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            });
            recyclerView.setAdapter(eventAdapter);
        } else {
            eventAdapter.notifyDataSetChanged();
        }
    }

    private void loadEventsFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(CalendarActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentUsername = dataSnapshot.child("username").getValue(String.class);

                if (currentUsername == null) {
                    Toast.makeText(CalendarActivity.this, "Username is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();


                db.collection("events")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Map<String, Object> attendeesMap = (Map<String, Object>) doc.get("attendees");
                                if (attendeesMap != null && attendeesMap.containsKey(uid)) {
                                    processEventDocument(doc, false, true);
                                }
                            }
                        });

                db.collection("events")
                        .whereEqualTo("createdBy", uid)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    processEventDocument(document,true,false);
                                }
                            } else {
                                Toast.makeText(CalendarActivity.this, "Error fetching created events: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }

            private void processEventDocument(QueryDocumentSnapshot document,boolean isCreatedByUser,boolean isAttendedByUser) {
                if (document == null) {
                    Log.e("CalendarActivity", "Document is null");
                    return;
                }

                String name = document.getString("name");
                String locationId = document.getString("locationId");
                String dateTime = document.getString("dateTime");
                String createdBy = document.getString("createdBy");
                String id = document.getId();

                if (dateTime == null || name == null) return;

                String[] dateTimeParts = dateTime.split(" ");
                String[] splitDate = dateTime.split(" ")[0].split("-");
                String day = splitDate[0];
                String month = splitDate[1];
                String year = splitDate[2];
                String timePart = dateTimeParts[1] + " " + dateTimeParts[2]; // "01:30 PM"

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                LocalTime startTime = LocalTime.parse(timePart, timeFormatter);

                if (!Places.isInitialized()) {
                    Places.initialize(getApplicationContext(), key);
                }

                if (locationId != null && !locationId.isEmpty()) {
                    PlacesClient placesClient = Places.createClient(CalendarActivity.this);

                    List<Place.Field> placeFields = Arrays.asList(
                            Place.Field.NAME,
                            Place.Field.ADDRESS,
                            Place.Field.LAT_LNG
                    );

                    FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, placeFields);

                    placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                        Place place = response.getPlace();
                        String formattedLocationName = place.getName() + ", " + place.getAddress();

                        eventList.add(new CalendarEvent(id, name, year, day, month, dateTime, formattedLocationName, startTime,createdBy));
                        if(isCreatedByUser) {
                            createdEvents.add(new CalendarEvent(id, name, year, day, month, dateTime, formattedLocationName, startTime,createdBy));
                        }
                        if (isAttendedByUser) {
                            attendeeEvents.add(new CalendarEvent(id, name, year, day, month, dateTime, formattedLocationName, startTime,createdBy));
                        }
                    }).addOnFailureListener(exception -> {
                        if(isCreatedByUser) {
                            createdEvents.add(new CalendarEvent(id, name, year, day, month, dateTime, "Unknown Location", startTime,createdBy));
                        } else if (isAttendedByUser) {
                            attendeeEvents.add(new CalendarEvent(id, name, year, day, month, dateTime, "Unknown Location", startTime,createdBy));
                        }
                        eventList.add(new CalendarEvent(id, name, year, day, month, dateTime, "Unknown Location", startTime,createdBy));
                    });

                    LocalDate eventDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));

                    int role = isCreatedByUser ? 1 : 0;

                    DotIndicator existing = null;
                    for (DotIndicator di : dotIndicators) {
                        if (di.date.equals(eventDate)) {
                            existing = di;
                            break;
                        }
                    }
                    if (existing != null) {
                        // Upgrade role to "both" if necessary
                        if ((existing.attendeeOrCreator == 0 && role == 1) || (existing.attendeeOrCreator == 1 && role == 0)) {
                            existing.attendeeOrCreator = 2;
                        }
                    } else {
                        dotIndicators.add(new DotIndicator(id, role, eventDate));
                    }
                    runOnUiThread(() -> {
                        calendarView.notifyCalendarChanged();
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CalendarActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}