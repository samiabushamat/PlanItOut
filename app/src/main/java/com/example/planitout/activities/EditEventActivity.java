package com.example.planitout.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.example.planitout.BuildConfig;
import com.example.planitout.PlanItOutApp;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import com.example.planitout.models.SharedViewModel;

import com.example.planitout.R;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.widget.Toast;

public class EditEventActivity extends AppCompatActivity {

    private EditText eventNameInput, eventDescriptionInput;
    private TextView eventDateTime, eventLocation;
    private SwitchCompat switchInPerson, switchPrivate;
    private ImageView eventImage;
    private Button saveChangesButton;
    private String originalName, originalDescription, originalImageUrl, originalDateTime, originalLocation;
    private boolean originalInPerson, originalPrivate;
    private HashMap<String, String> originalAttendees = new HashMap<>();
    private String createdBy = null;
    private String imageUrl = null;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String eventId;
    private static final int LOCATION_PICK_REQUEST = 100;
    private String updatedLocationId = null;
    private String originalLocationId;
    private LinearLayout addImageCard;
    private static final int IMAGE_PICK_REQUEST = 1001;
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute;
    String key = BuildConfig.GOOGLE_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        db = FirebaseFirestore.getInstance();

        //initializing Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), key);
        }

        //bind views
        eventNameInput = findViewById(R.id.eventName);
        eventDescriptionInput = findViewById(R.id.eventDescription);
        eventDateTime = findViewById(R.id.eventDateTime);
        eventLocation = findViewById(R.id.eventLocation);
        switchInPerson = findViewById(R.id.switchInPerson);
        switchPrivate = findViewById(R.id.switchPrivate);
        eventImage = findViewById(R.id.eventImage);
        saveChangesButton = findViewById(R.id.createEventBtn);
        ImageView addImageIcon = findViewById(R.id.addImageIcon);
        TextView addImageText = findViewById(R.id.addImageText);
        TextView viewTitle = findViewById(R.id.viewTitle);
        addImageCard = findViewById(R.id.addEventImage);

        viewTitle.setText("Edit Event");
        saveChangesButton.setText("Save Changes");

        //set click listeners
        eventLocation.setOnClickListener(v -> openPlacePicker());
        eventDateTime.setOnClickListener(v -> showDatePicker());

        addImageCard.setOnClickListener(v -> openImagePicker());

        //get eventId from intent
        eventId = getIntent().getStringExtra("eventId");

        loadEventDetails();
        saveChangesButton.setOnClickListener(v -> updateEvent());
    }

    /*
    Function to fetch the requested event details to save and display them.
     */
    private void loadEventDetails() {
        db.collection("events").document(eventId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                originalImageUrl = document.getString("imageUrl");
                originalName = document.getString("name");
                originalDescription = document.getString("description");
                originalLocationId = document.getString("locationId");
                originalDateTime = document.getString("dateTime");
                originalInPerson = document.getBoolean("inPerson") != null && document.getBoolean("inPerson");
                originalPrivate = document.getBoolean("private") != null && document.getBoolean("private");
                originalAttendees = (HashMap<String, String>) document.get("attendees");
                createdBy = document.getString("createdBy");

                eventNameInput.setText(originalName);
                eventDescriptionInput.setText(originalDescription);
                eventLocation.setText(originalLocation);
                eventDateTime.setText(originalDateTime);
                switchInPerson.setChecked(originalInPerson);
                switchPrivate.setChecked(originalPrivate);

                if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                    loadEventImage(originalImageUrl);
                }

                String locationId = document.getString("locationId");
                if (locationId != null && !locationId.isEmpty()) {
                    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.ADDRESS);
                    PlacesClient placesClient = Places.createClient(this);
                    FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, fields);

                    placesClient.fetchPlace(request)
                            .addOnSuccessListener(response -> {
                                Place place = response.getPlace();
                                eventLocation.setText(place.getAddress());
                            })
                            .addOnFailureListener(e -> {
                                eventLocation.setText("Location not found");
                                Log.e("EditEventActivity", "Failed to fetch place", e);
                            });
                }
            }
        });
    }

    // Function to load event image into the ImageView
    private void loadEventImage(String imageUrl) {
        Log.d("EditEventActivity", "Loading image: " + imageUrl);
        CardView imageCard = findViewById(R.id.roundedCardView);

        ImageView addImage = findViewById(R.id.addImageIcon);
        TextView addImageText = findViewById(R.id.addImageText);

        addImage.setVisibility(View.GONE);
        addImageText.setVisibility(View.GONE);
        imageCard.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(imageUrl)
                .into(eventImage);
    }

    /*
    Function that saves all the data currently entered into the text boxes and updates database
     */
    private void updateEvent() {
        String name = eventNameInput.getText().toString().trim();
        String description = eventDescriptionInput.getText().toString().trim();
        String location = eventLocation.getText().toString().trim();
        String dateTime = eventDateTime.getText().toString().trim();
        boolean isInPerson = switchInPerson.isChecked();
        boolean isPrivate = switchPrivate.isChecked();

        //checking if data has changed
        String finalLocationId = (updatedLocationId != null) ? updatedLocationId : originalLocationId;
        if (name.equals(originalName) &&
                description.equals(originalDescription) &&
                location.equals(originalLocation) &&
                dateTime.equals(originalDateTime) &&
                isInPerson == originalInPerson &&
                isPrivate == originalPrivate &&
                finalLocationId.equals(originalLocationId)) {
            finish();
            return;
        }

        //proceed with update
        if (imageUri != null) {
            uploadImageAndSaveEvent(name, description, dateTime, isInPerson, isPrivate, location, finalLocationId);
        } else {
            saveEventUpdate(name, description, dateTime, isInPerson, isPrivate, location, finalLocationId, originalImageUrl);
        }

    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.ID
        );

        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(EditEventActivity.this);
        startActivityForResult(intent, LOCATION_PICK_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == LOCATION_PICK_REQUEST) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                updatedLocationId = place.getId();
                eventLocation.setText(place.getAddress());
            } else if (requestCode == IMAGE_PICK_REQUEST) {
                imageUri = data.getData();

                CardView imageCard = findViewById(R.id.roundedCardView);
                ImageView addImage = findViewById(R.id.addImageIcon);
                TextView addImageText = findViewById(R.id.addImageText);

                addImage.setVisibility(View.GONE);
                addImageText.setVisibility(View.GONE);
                imageCard.setVisibility(View.VISIBLE);

                eventImage.setImageURI(imageUri);

            }
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    showTimePicker();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int hour12 = (hourOfDay == 0) ? 12 : (hourOfDay > 12 ? hourOfDay - 12 : hourOfDay);

                    String formattedDateTime = String.format("%02d-%02d-%d %02d:%02d %s",
                            selectedDay, selectedMonth + 1, selectedYear,
                            hour12, selectedMinute, amPm);

                    eventDateTime.setText(formattedDateTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false);
        timePickerDialog.show();
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }
    private void uploadImageAndSaveEvent(String name, String description, String dateTime,
                                         boolean isInPerson, boolean isPrivate,
                                         String location, String locationId) {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child(eventId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveEventUpdate(name, description, dateTime, isInPerson, isPrivate, location, locationId, imageUrl);
                    Log.d("EditEventActivity", "Image URL: " + imageUrl);
                    loadEventImage(imageUrl);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void saveEventUpdate(String name, String description, String dateTime,
                                 boolean isInPerson, boolean isPrivate,
                                 String location, String locationId, String imageUrl) {

        db.collection("events").document(eventId)
                .update(
                        "name", name,
                        "description", description,
                        "dateTime", dateTime,
                        "location", location,
                        "locationId", locationId,
                        "imageUrl", imageUrl,
                        "inPerson", isInPerson,
                        "private", isPrivate
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();

                    //notifying attendees and triggering refresh on created events fragment
                    notifyAttendees(eventId, name, createdBy);

                    PlanItOutApp app = (PlanItOutApp) getApplication();
                    SharedViewModel viewModel = new ViewModelProvider(app,
                            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
                    ).get(SharedViewModel.class);

                    viewModel.triggerRefreshCreatedEvents();
                    finish();
                })

                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /*
     Function to put notification in database to notify attendees of event details being changed
      */
    private void notifyAttendees(String eventId, String eventTitle, String currentUserUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DatabaseReference realtimeDb = FirebaseDatabase.getInstance().getReference();

        db.collection("events").document(eventId).get().addOnSuccessListener(documentSnapshot -> {
            Map<String, String> attendees = (Map<String, String>) documentSnapshot.get("attendees");

            if (attendees != null) {
                for (Map.Entry<String, String> entry : attendees.entrySet()) {
                    String uid = entry.getKey();
                    String username = entry.getValue();

                    if (!uid.equals(currentUserUid)) {
                        Log.d("NotifyAttendees", "Sending notification to UID: " + uid);

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("message", "The event '" + eventTitle + "' has been updated.");
                        notification.put("timestamp", ServerValue.TIMESTAMP);

                        realtimeDb.child("users")
                                .child(uid)
                                .child("notifications")
                                .push()
                                .setValue(notification)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("NotifyAttendees", "Notification added for UID: " + uid))
                                .addOnFailureListener(e ->
                                        Log.e("NotifyAttendees", "Failed for UID: " + uid, e));
                    }
                }
            }
        });
    }
}


