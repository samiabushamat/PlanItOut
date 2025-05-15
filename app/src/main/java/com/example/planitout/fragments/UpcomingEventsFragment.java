    package com.example.planitout.fragments;
    import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

    import android.Manifest;
    import android.app.AlertDialog;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.graphics.Typeface;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.RadioGroup;
    import android.widget.Spinner;
    import android.widget.TextView;
    import android.widget.Toast;
    
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.widget.SearchView;
    import androidx.core.content.ContextCompat;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.planitout.BuildConfig;
    import com.example.planitout.R;
    import com.example.planitout.activities.EventDetailActivity;
    import com.example.planitout.adapters.UpcomingEventsAdapter;
    import com.example.planitout.models.Event;
    import com.google.android.flexbox.FlexboxLayout;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationServices;
    import com.google.android.gms.maps.model.LatLng;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.google.android.libraries.places.api.model.Place;
    import com.google.android.libraries.places.api.model.AddressComponent;
    import com.google.android.libraries.places.api.net.FetchPlaceRequest;
    import com.google.android.libraries.places.api.net.PlacesClient;
    
    import java.text.ParseException;
    import java.text.SimpleDateFormat;
    import java.time.DayOfWeek;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;
    
    /**
     * Fragment that displays the list of upcoming events.
     */
    public class UpcomingEventsFragment extends Fragment {
        private RecyclerView recyclerView;
        private UpcomingEventsAdapter upcomingEventsAdapter;
        private List<Event> eventList, originalEventList;;
        private PlacesClient placesClient;
        private FirebaseFirestore db;
        private String currentUserId, createdBy;
        private SearchView searchView;
        private ImageButton filterButton;
        private FusedLocationProviderClient fusedLocationClient;
        private double userLat, userLng;
        private SwipeRefreshLayout swipeRefreshLayout;

        private FlexboxLayout selectedFiltersContainer;
        private List<String> selectedFilters = new ArrayList<>();
        private String currentDateFilter = "All Dates";
        private String currentLocationFilter = "Anywhere";
        private double eventLat, eventLng;
        String key = BuildConfig.GOOGLE_API_KEY;
    
        public UpcomingEventsFragment() {
            //required empty public constructor
        }
        public UpcomingEventsFragment(String userId) {
            this.currentUserId = userId;
        }
    
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            getUserLocation();
        }
    
        private void getUserLocation() {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
    
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                    }
                }).addOnFailureListener(e -> Log.e("Location", "Failed to get location", e));
            } else {
                Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        }
    
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_upcoming_events, container, false);

            recyclerView = view.findViewById(R.id.recyclerView_upcoming);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutUpcoming);
            selectedFiltersContainer = view.findViewById(R.id.selected_filters_container);
            searchView = view.findViewById(R.id.search_view);
            filterButton = view.findViewById(R.id.filter_button);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchEvents(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchEvents(newText);
                    return false;
                }
            });

            filterButton.setOnClickListener(v -> showFilterMenu());

            eventList = new ArrayList<>();
            upcomingEventsAdapter = new UpcomingEventsAdapter(getContext(), eventList, eventId -> {
                // Start EventDetailActivity with eventId as an extra
                Intent intent = new Intent(getActivity(), EventDetailActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            });

            recyclerView.setAdapter(upcomingEventsAdapter);

            // initialize Places API
            if (!com.google.android.libraries.places.api.Places.isInitialized()) {
                com.google.android.libraries.places.api.Places.initialize(requireContext(), key);
            }

            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadUpcomingEvents();
                swipeRefreshLayout.setRefreshing(false);
            });

            placesClient = com.google.android.libraries.places.api.Places.createClient(requireContext());
            db = FirebaseFirestore.getInstance();

            loadUpcomingEvents();
            return view;
        }

        private void showFilterMenu() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog);
    
            View filterView = getLayoutInflater().inflate(R.layout.dialog_filter_events, null);
            builder.setView(filterView);
    
            // Get references to UI elements
            RadioGroup dateGroup = filterView.findViewById(R.id.radioGroup_date);
            RadioGroup locationGroup = filterView.findViewById(R.id.radioGroup_location);
    
            // Set current selections
            switch (currentDateFilter) {
                case "Today": dateGroup.check(R.id.radio_today); break;
                case "This Week": dateGroup.check(R.id.radio_this_week); break;
                case "This Month": dateGroup.check(R.id.radio_this_month); break;
                default: dateGroup.check(R.id.radio_all); break;
            }
    
            switch (currentLocationFilter) {
                case "Within 10 miles": locationGroup.check(R.id.radio_10_miles); break;
                case "Within 30 miles": locationGroup.check(R.id.radio_30_miles); break;
                case "Remote": locationGroup.check(R.id.radio_remote); break;
                default: locationGroup.check(R.id.radio_anywhere); break;
            }
    
            builder.setPositiveButton("Apply", (dialog, which) -> {
                currentDateFilter = getDateFilterFromId(dateGroup.getCheckedRadioButtonId());
                currentLocationFilter = getLocationFilterFromId(locationGroup.getCheckedRadioButtonId());
    
                updateFilterChips();
                applyFilters();
            });
    
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
    
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(d -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
    
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                positiveButton.setTypeface(null, Typeface.BOLD);
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                negativeButton.setTypeface(null, Typeface.BOLD);
            });
    
            dialog.show();
        }
    
        private String getDateFilterFromId(int radioButtonId) {
            if (radioButtonId == R.id.radio_today) return "Today";
            if (radioButtonId == R.id.radio_this_week) return "This Week";
            if (radioButtonId == R.id.radio_this_month) return "This Month";
            return "All Dates";
        }
    
        private String getLocationFilterFromId(int radioButtonId) {
            if (radioButtonId == R.id.radio_10_miles) return "Within 10 miles";
            if (radioButtonId == R.id.radio_30_miles) return "Within 30 miles";
            if (radioButtonId == R.id.radio_remote) return "Remote";
            return "Anywhere";
        }
    
        private void updateFilterChips() {
            selectedFiltersContainer.removeAllViews();
    
            // Only show non-default filters as chips
            if (!currentDateFilter.equals("All Dates")) {
                addFilterChip(currentDateFilter);
            }
            if (!currentLocationFilter.equals("Anywhere")) {
                addFilterChip(currentLocationFilter);
            }
        }
    
        private void addFilterChip(String filterText) {
            View filterView = LayoutInflater.from(getContext())
                    .inflate(R.layout.filter_chip, selectedFiltersContainer, false);
            TextView chipText = filterView.findViewById(R.id.chip_text);
            chipText.setText(filterText);
    
            filterView.setOnClickListener(v -> {
                // Remove the filter when chip is clicked
                if (filterText.equals(currentDateFilter)) {
                    currentDateFilter = "All Dates";
                } else if (filterText.equals(currentLocationFilter)) {
                    currentLocationFilter = "Anywhere";
                }
    
                updateFilterChips();
                applyFilters();
            });
    
            selectedFiltersContainer.addView(filterView);
        }
    
        private void applyFilters() {
            List<Event> filteredList = new ArrayList<>();
    
            for (Event event : originalEventList) {
                boolean dateMatches = checkDateFilter(event);
                boolean locationMatches = checkLocationFilter(event);
    
                if (dateMatches && locationMatches) {
                    filteredList.add(event);
                }
            }
    
            upcomingEventsAdapter.updateList(filteredList);
        }
    
        private boolean checkDateFilter(Event event) {
            switch (currentDateFilter) {
                case "Today": return isToday(event.getDateTime());
                case "This Week": return isThisWeek(event.getDateTime());
                case "This Month": return isThisMonth(event.getDateTime());
                default: return true; // "All Dates"
            }
        }
    
        private boolean checkLocationFilter(Event event) {
            if (!event.getIsInPerson()) {
                return currentLocationFilter.equals("Remote") ||
                        currentLocationFilter.equals("Anywhere");
            }
    
            switch (currentLocationFilter) {
                case "Remote": return false;
                case "Within 10 miles":
                    return isWithinDistance(event, 10);
                case "Within 30 miles":
                    return isWithinDistance(event, 30);
                default: return true; // "Anywhere"
            }
        }
    
        private boolean isWithinDistance(Event event, double maxDistanceMiles) {
            // convert miles to kilometers (since haversine uses km)
            double maxDistanceKm = maxDistanceMiles * 1.60934;
    
            String locationId = event.getLocation();
            setEventLatLng(locationId);
    
            // calculate distance using Haversine formula
            double distance = haversine(userLat, userLng, eventLat, eventLng);
            return distance <= maxDistanceKm;
        }
    
        private void setEventLatLng(String locationId) {
            if (locationId == null || locationId.isEmpty()) {
                Log.e("LocationError", "Invalid locationId: null or empty.");
                eventLat = 0.0;
                eventLng = 0.0;
                return;
            }

            Log.d("LocationId", locationId);
            FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, Arrays.asList(Place.Field.LOCATION));

            placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                LatLng latLng = response.getPlace().getLocation();
                if (latLng != null) {
                    eventLat = latLng.latitude;
                    eventLng = latLng.longitude;
                }
            }).addOnFailureListener(e -> {
                Log.e("Error", "Failed to fetch location", e);
            });
        }
    
        private double haversine(double lat1, double lon1, double lat2, double lon2) {
            // Haversine formula implementation
            double R = 6371; // Earth radius in km
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            lat1 = Math.toRadians(lat1);
            lat2 = Math.toRadians(lat2);
    
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return R * c;
        }
    
        private boolean isToday(String eventDateTime) {
            // convert eventDateTime to a Date object
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
    
            // check if the event date is today
            try {
                Date eventDate = dateFormat.parse(eventDateTime);
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventDate);
    
                Calendar todayCal = Calendar.getInstance();
    
                return (eventCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        eventCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR));
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
    
        private boolean isThisWeek(String eventDateTime) {
            // convert eventDateTime to a Date object
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
    
            // check if the event date is this week
            try {
                Date eventDate = dateFormat.parse(eventDateTime);
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventDate);
    
                Calendar todayCal = Calendar.getInstance();
                int currentWeek = todayCal.get(Calendar.WEEK_OF_YEAR);
                int currentYear = todayCal.get(Calendar.YEAR);
    
                int eventWeek = eventCal.get(Calendar.WEEK_OF_YEAR);
                int eventYear = eventCal.get(Calendar.YEAR);
    
                return (eventYear == currentYear && eventWeek == currentWeek);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
    
        private boolean isThisMonth(String eventDateTime) {
            // Convert eventDateTime to a Date object
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
    
            try {
                Date eventDate = dateFormat.parse(eventDateTime);
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventDate);
    
                Calendar todayCal = Calendar.getInstance();
                int currentMonth = todayCal.get(Calendar.MONTH);
                int currentYear = todayCal.get(Calendar.YEAR);
    
                int eventMonth = eventCal.get(Calendar.MONTH);
                int eventYear = eventCal.get(Calendar.YEAR);
    
                return (eventYear == currentYear && eventMonth == currentMonth);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
    
        private void searchEvents(String query) {
            // clear the original list and add filtered events
            eventList.clear();
            if (originalEventList == null) {
                upcomingEventsAdapter.notifyDataSetChanged();
                return;
            }
            if (query.isEmpty()) {
                eventList.addAll(originalEventList);
            } else {
                String lowerCaseQuery = query.toLowerCase().trim();
                for (Event event : originalEventList) {
                    if (event.getName().toLowerCase().contains(lowerCaseQuery) ||
                            event.getAddress().toLowerCase().contains(lowerCaseQuery) ||
                            event.getDateTime().toLowerCase().contains(lowerCaseQuery)) {
                        eventList.add(event);
                    }
                }
            }
            upcomingEventsAdapter.notifyDataSetChanged();
        }
    
        private void loadUpcomingEvents() {

            db.collection("events")
                    .whereNotEqualTo("createdBy", currentUserId)  // Get events not created by the current user
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            eventList.clear();
                            originalEventList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String eventId = document.getId();
                                String name = document.getString("name");
                                String createdBy = document.getString("createdBy");
                                String dateTime = document.getString("dateTime");
                                String locationID = document.getString("locationId");
                                Boolean isInPerson = document.getBoolean("inPerson");
                                Boolean isPrivate = document.getBoolean("private");
    
                                Log.d("UpcomingEventsFragment", "Event: " + name + " | locationId: " + locationID);
    
                                // Fetch the username for the creator of the event
                                getUsername(createdBy, new UsernameCallback() {
                                    @Override
                                    public void onSuccess(String username) {
                                        // Once username is fetched, proceed to fetch location info
                                        fetchLocationInfo(eventId, locationID, name, username, dateTime, isPrivate, isInPerson);
                                    }
    
                                    @Override
                                    public void onFailure(String error) {
                                        // In case of failure, you can either use null or handle as needed
                                        fetchLocationInfo(eventId, locationID, name, null, dateTime, isPrivate, isInPerson);
                                    }
                                });
                            }
                        } else {
                            Log.e("UpcomingEventsFragment", "Error getting events", task.getException());
                        }
                    });
            swipeRefreshLayout.setRefreshing(false);

        }
    
        private void getUsername(String userId, final UsernameCallback callback) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("username");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Directly return the username or null if not found
                    String username = dataSnapshot.exists() ? dataSnapshot.getValue(String.class) : null;
                    callback.onSuccess(username);
                }
    
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Directly return null in case of error
                    callback.onSuccess(null);
                }
            });
        }
    
        public interface UsernameCallback {
            void onSuccess(String username);
            void onFailure(String error);
        }
    
        private void fetchLocationInfo(String eventId, String locationId, String name, String createdBy, String dateTime, Boolean isPrivate, Boolean isInPerson) {
            if (isInPerson && (locationId == null || locationId.isEmpty())) {
                Log.d("UpcomingEventsFragment", "Location ID is null or empty for event: " + name);
            } else if (!isInPerson && (locationId != null || !locationId.isEmpty())) {
                Event event = new Event(eventId, name, createdBy, dateTime, "", "", "", isPrivate, false);
                eventList.add(event);
                originalEventList.add(event);
                upcomingEventsAdapter.notifyDataSetChanged();
            }
    
            List<Place.Field> placeFields = Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.ADDRESS_COMPONENTS);
            FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, placeFields);
    
            placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                Place place = response.getPlace();
                String locationName = place.getDisplayName();
                String fullAddress = place.getFormattedAddress();
                String streetNumber = "", route = "", city = "", state = "", zip = "";
    
                if (place.getAddressComponents() != null) {
                    List<AddressComponent> components = place.getAddressComponents().asList();
                    for (AddressComponent component : components) {
                        if (component.getTypes().contains("street_number")) streetNumber = component.getName();
                        if (component.getTypes().contains("route")) route = component.getName();
                        if (component.getTypes().contains("locality")) city = component.getName();
                        if (component.getTypes().contains("administrative_area_level_1")) state = component.getName();
                        if (component.getTypes().contains("postal_code")) zip = component.getName();
                    }
                }
    
                final String finalStreetAddress = streetNumber.isEmpty() ? route : streetNumber + " " + route;
                final String finalCityStateZip = (!city.isEmpty() ? city + ", " : "") +
                        (!state.isEmpty() ? state + " " : "") +
                        (!zip.isEmpty() ? zip : "");
    
                // Add event details to the list
                Event event = new Event(eventId, name, createdBy, dateTime, locationId, finalStreetAddress, finalCityStateZip, isPrivate, isInPerson);
                eventList.add(event);
                originalEventList.add(event);
                upcomingEventsAdapter.notifyDataSetChanged();
            }).addOnFailureListener(e -> {
                Log.e("PlacesAPI", "Failed to fetch location", e);
                Event event = new Event(eventId, name, createdBy, dateTime, "Unknown Location", "Unknown Address", "Unknown City/State/ZIP", isPrivate, isInPerson);
                eventList.add(event);
                originalEventList.add(event);
                upcomingEventsAdapter.notifyDataSetChanged();
            });
        }
    }
