package com.example.planitout.activities;
import static android.app.ProgressDialog.show;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.planitout.BuildConfig;
import com.example.planitout.R;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;

import com.example.planitout.adapters.CalendarEventAdapter;
import com.example.planitout.adapters.EventAdapter;
import com.example.planitout.fragments.CreatedEventsFragment;
import com.example.planitout.models.CalendarEvent;
import com.example.planitout.models.DotIndicator;
import com.example.planitout.models.Event;
import com.example.planitout.models.EventModel;
import com.example.planitout.models.UserModel;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.GenericTypeIndicator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FriendProfileViewActivity extends AppCompatActivity {
    private TextView editTextBio, editTextLocation;
    private final Collection<String> prefs = Arrays.asList("social", "professional", "entertainment", "sports", "tech", "seasonal", "charity", "other");
    private final List<String> availablePreferences = new ArrayList<>(prefs);
    private List<String> selectedPreferences = new ArrayList<>();
    private ImageView profilePictureView;
    private TextView tagText, usernameView;
    String userID;
    private static final int IMAGE_PICK_REQUEST = 1001;
    private Uri imageUri;
    DatabaseReference userRef;
    private List<UserModel> friendsList = new ArrayList<>();
    private DatabaseReference databaseReference;
    private UserModel user;
    private String username;
    private List<CalendarEvent> createdEvents = new ArrayList<>();
    private List<CalendarEvent> attendingEvents = new ArrayList<>();
    private List<CalendarEvent> eventList = new ArrayList<>();
    private RecyclerView recyclerView;
    private CalendarEventAdapter eventAdapter;
    String key = BuildConfig.GOOGLE_API_KEY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_profile_view);

        usernameView = findViewById(R.id.usernameView);
        username = "";
        userID = getIntent().getStringExtra("userID");

        loadUserName(userID);
        usernameView.setText(username);

        // Unfriend button
        ImageButton unfriendButton = findViewById(R.id.unfriendButton);
        unfriendButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Unfriend")
                    .setMessage("Are you sure you want to remove this friend?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        String currentUid = FirebaseAuth.getInstance().getUid();
                        String unfriendedUid = getIntent().getStringExtra("userID");
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");
                        //remove from current user's friends list
                        dbRef.child(currentUid).child("friends").child(unfriendedUid).removeValue();
                        //remove from the other user's friends list
                        dbRef.child(unfriendedUid).child("friends").child(currentUid).removeValue()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("unfriendedUid", unfriendedUid);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                });

                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Profile picture
        profilePictureView = findViewById(R.id.profilePicture);

        // Friends Button
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Bio
        editTextBio = findViewById(R.id.editTextBio);
        editTextBio.setEnabled(false);
        editTextBio.setFocusable(false);

        // Location
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextLocation.setEnabled(false);
        editTextLocation.setFocusable(false);

        // Event Adapter
        recyclerView = findViewById(R.id.recyclerView_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadEventsFromFirestore();
        eventList = new ArrayList<>();
        eventList.addAll(createdEvents);
        eventList.addAll(attendingEvents);

        eventAdapter = new CalendarEventAdapter(this, eventList, eventId -> {
            Intent intent = new Intent(FriendProfileViewActivity.this, EventDetailActivity.class);
            intent.putExtra("eventId",eventId);
            startActivity(intent);
        });
        eventAdapter.sortEventsByDateAndTime();
        recyclerView.setAdapter(eventAdapter);


        if (userID != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userID);

            //load bio
            userRef.child("bio").get().addOnSuccessListener(dataSnapshot -> {
                String bio = dataSnapshot.getValue(String.class);
                if (bio != null) {
                    editTextBio.setText(bio);
                } else {
                    editTextBio.setHint("Bio:");
                }
            });

            //load location
            userRef.child("location").get().addOnSuccessListener(dataSnapshot -> {
                String location = dataSnapshot.getValue(String.class);
                if (location != null) {
                    editTextLocation.setText(location);
                } else {
                    editTextLocation.setHint("Location:");
                }
            });


            //load profile picture
            userRef.child("profilePicture").get().addOnSuccessListener(dataSnapshot -> {
                String profilePicture = dataSnapshot.getValue(String.class);
                if (profilePicture != null) {
                    loadImage(profilePicture, profilePictureView);
                } else {
                    profilePictureView.setImageResource(R.drawable.baseline_account_circle_24);
                }
            });


        }
    }


    private void loadUserName(String userID) {
        FirebaseDatabase.getInstance().getReference("users").child(userID).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        user = dataSnapshot.getValue(UserModel.class);
                        if (user != null) {
                            usernameView.setText(user.getUsername());
                        } else {
                            usernameView.setText("Unknown User");
                        }
                    } else {
                        usernameView.setText("User not found");
                    }
                })
                .addOnFailureListener(e -> Log.e("FriendProfileView", "Error loading user name", e));
    }

    private void loadImage(String imageUrl, ImageView target) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.baseline_account_circle_24)
                .circleCrop()
                .into(target);
    }


    private void loadEventsFromFirestore() {

        String uid = userID;
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(FriendProfileViewActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentUsername = dataSnapshot.child("username").getValue(String.class);

                if (currentUsername == null) {
                    Toast.makeText(FriendProfileViewActivity.this, "Username is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();


                db.collection("events")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Map<String, Object> attendeesMap = (Map<String, Object>) doc.get("attendees");
                                if (attendeesMap != null && attendeesMap.containsKey(uid)) {
                                    Log.d("FriendProfileViewActivity", "User is attending event: " + doc.getId());
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

                                    Log.d("FriendProfileViewActivity", "User created event: " + document.getId());
                                    processEventDocument(document, true, false);
                                }
                            } else {
                                Toast.makeText(FriendProfileViewActivity.this, "Error fetching created events: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }

            private void processEventDocument(QueryDocumentSnapshot document, boolean isCreatedByUser, boolean isAttendedByUser) {
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
                    PlacesClient placesClient = Places.createClient(FriendProfileViewActivity.this);

                    List<Place.Field> placeFields = Arrays.asList(
                            Place.Field.NAME,
                            Place.Field.ADDRESS,
                            Place.Field.LAT_LNG
                    );

                    FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, placeFields);

                    placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                        Place place = response.getPlace();
                        String formattedLocationName = place.getName() + ", " + place.getAddress();

                        CalendarEvent event = new CalendarEvent(id, name, year, day, month, dateTime, formattedLocationName, startTime, createdBy);
                        eventList.add(event);
                        if (isCreatedByUser) createdEvents.add(event);
                        if (isAttendedByUser) attendingEvents.add(event);

                        eventAdapter.notifyDataSetChanged(); // 🔥 HERE
                    }).addOnFailureListener(exception -> {
                        CalendarEvent fallback = new CalendarEvent(id, name, year, day, month, dateTime, "Unknown Location", startTime, createdBy);
                        eventList.add(fallback);
                        if (isCreatedByUser) createdEvents.add(fallback);
                        if (isAttendedByUser) attendingEvents.add(fallback);

                        eventAdapter.notifyDataSetChanged(); // 🔥 HERE

                    });

                    LocalDate eventDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(FriendProfileViewActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}