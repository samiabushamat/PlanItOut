package com.example.planitout.activities;

import androidx.appcompat.app.AppCompatActivity;

import com.example.planitout.PlanItOutApp;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.planitout.R;
import com.example.planitout.models.Event;
import com.example.planitout.models.EventModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import com.example.planitout.BuildConfig;

public class EventDetailActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageReference;
    private EventModel event;
    private ImageView eventImageDisplay;
    private String eventLocation, eventType, attendeesText, locationName;
    private LatLng locationLatLng;
    private TextView eventTitleDisplay, eventTimeDisplay, eventLocDisplay, eventDescDisplay, eventAttendeesDisplay, eventTypesDisplay;
    private Button registerButton, openMapsButton;
    private Boolean isRegistered = false;
    private Boolean isInPerson, isPrivate;
    String key = BuildConfig.GOOGLE_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        Places.initialize(getApplicationContext(), key);

        eventTitleDisplay = findViewById(R.id.eventTitle);
        eventTimeDisplay = findViewById(R.id.eventTime);
        eventLocDisplay = findViewById(R.id.eventLocation);
        eventImageDisplay = findViewById(R.id.eventImage);
        eventDescDisplay = findViewById(R.id.eventDescription);
        eventAttendeesDisplay = findViewById(R.id.peopleAttending);
        eventTypesDisplay = findViewById(R.id.eventType);

        // fetch event details from firestore
        fetchEventDetails();

        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> registerForEvent());

        openMapsButton = findViewById(R.id.openMapsButton);
        openMapsButton.setOnClickListener(v -> openMaps());

    }
    private void fetchEventDetails() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = documentSnapshot.toObject(EventModel.class);
                        if (event != null) {
                            eventTitleDisplay.setText(event.getName());
                            eventTimeDisplay.setText("Time: " + event.getDateTime());
                            eventDescDisplay.setText(event.getDescription());

                            eventLocation = event.getLocationId();
                            fetchPlaceDetails(eventLocation);

                            isInPerson = event.isInPerson();
                            isPrivate = event.isPrivate();

                            if (isPrivate) {
                                if (isInPerson)
                                    eventType = "Event Type: Private, In-Person Event";
                                else
                                    eventType = "Event Type: Private, Remote Event";
                            } else {
                                if (isInPerson)
                                    eventType = "Event Type: Public, In-Person Event";
                                else
                                    eventType = "Event Type: Public, Remote Event";
                            }

                            eventTypesDisplay.setText(eventType);

                            if (event.getImageUrl() != null) {
                                loadImage(event.getImageUrl(), eventImageDisplay);
                            }

                            Map<String, String> attendees = event.getAttendees();
                            attendeesText = "Registered Attendees: " + ((attendees != null) ? attendees.size() : 0);
                            eventAttendeesDisplay.setText(attendeesText);

                            // check if user is already registered
                            String userId = auth.getCurrentUser().getUid();
                            if (attendees != null && attendees.containsKey(userId)) {
                                isRegistered = true;
                                registerButton.setText("Unregister For Event");
                            }

                        }
                    } else {
                        Toast.makeText(this, "Event details not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        Places.createClient(this).fetchPlace(request)
                .addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();
                        locationName = place.getDisplayName();
                        String placeAddress = place.getFormattedAddress();
                        locationLatLng = place.getLocation();
                        eventLocDisplay.setText("Location: \n" + locationName + "\n " + placeAddress);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(EventDetailActivity.this, "Failed to fetch location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream in = new java.net.URL(imageUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                new Handler(Looper.getMainLooper()).post(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void notifyCreatorOfAttendanceChange(String creatorUid, String username, String eventName, int type) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(creatorUid)
                .child("notifications");

        String message;
        switch (type) {
            case 1:
                message = username + " has registered for '" + eventName + "'.";
                break;
            case 2:
                message = username + " has unregistered from '" + eventName + "'.";
                break;
            default:
                Log.w("NotifyAttendance", "❗ Unknown notification type: " + type);
                return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        notifRef.push().setValue(notification)
                .addOnSuccessListener(aVoid ->
                        Log.d("NotifyAttendance", "✅ Notification sent to creator: " + creatorUid))
                .addOnFailureListener(e ->
                        Log.e("NotifyAttendance", "❌ Failed to notify creator: " + creatorUid, e));
    }


    private void registerForEvent() {
        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || userId == null) {
            Toast.makeText(this, "Error: Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentUsername = ((PlanItOutApp) getApplicationContext()).getCurrentUsername();
                    if (documentSnapshot.exists()) {
                        // Fetch attendees map instead of list
                        @SuppressWarnings("unchecked")
                        Map<String, String> attendees = (Map<String, String>) documentSnapshot.get("attendees");
                        if (attendees == null)
                            attendees = new HashMap<>();

                        if (attendees.containsKey(userId)) {
                            // user already registered, unregister them from the event
                            attendees.remove(userId);
                            isRegistered = false;
                            registerButton.setText("Register For Event");
                            notifyCreatorOfAttendanceChange(event.getCreatedBy(), currentUsername, event.getName(), 2);

                        } else {
                            // register the user using global variable for the username
                            String username = ((PlanItOutApp) getApplication()).getCurrentUsername();
                            attendees.put(userId, username);
                            isRegistered = true;
                            registerButton.setText("Unregister For Event");
                            notifyCreatorOfAttendanceChange(event.getCreatedBy(), currentUsername, event.getName(), 1);
                        }

                        // update Firestore database
                        Map<String, String> finalAttendees = attendees;
                        db.collection("events").document(eventId)
                                .update("attendees", attendees)
                                .addOnSuccessListener(aVoid -> {
                                    attendeesText = "Registered Attendees: " + finalAttendees.size();
                                    eventAttendeesDisplay.setText(attendeesText);
                                    Toast.makeText(this, isRegistered ? "Registered for event" : "Unregistered for event", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update registration", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to access event data", Toast.LENGTH_SHORT).show());
    }

    private void openMaps() {
        if (locationLatLng == null) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = "geo:" + locationLatLng.latitude + "," + locationLatLng.longitude +
                "?q=" + locationLatLng.latitude + "," + locationLatLng.longitude +
                "(" + locationName + ")";

        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }
}