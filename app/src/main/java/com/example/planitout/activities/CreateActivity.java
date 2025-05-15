package com.example.planitout.activities;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.planitout.BuildConfig;
import com.example.planitout.PlanItOutApp;
import com.example.planitout.models.EventModel;
import com.example.planitout.models.SharedViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AccessibilityOptions;
import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.example.planitout.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateActivity extends AppCompatActivity {

    private SwitchCompat switchInPerson, switchPrivate;
    private EditText eventNameInput, eventDescriptionInput;
    private TextView eventLocation, eventDateTime, addImageText;
    private ImageView eventImage, addImageIcon;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private CardView roundedCard;
    private LinearLayout addEventImage;
    private Button createEventButton;
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute;
    private Map<String, String> attendees;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageReference;

    private static final int LOCATION_PICK_REQUEST = 100;
    private static final int IMAGE_PICK_REQUEST = 1001;

    private String locationID = "";
    String key = BuildConfig.GOOGLE_API_KEY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("event_images");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), key);
        }

        attendees = new HashMap<>();

        addEventImage = findViewById(R.id.addEventImage);
        eventNameInput = findViewById(R.id.eventName);
        eventDescriptionInput = findViewById(R.id.eventDescription);
        eventLocation = findViewById(R.id.eventLocation);
        eventImage = findViewById(R.id.eventImage);
        eventDateTime = findViewById(R.id.eventDateTime);
        switchInPerson = findViewById(R.id.switchInPerson);
        switchPrivate = findViewById(R.id.switchPrivate);
        createEventButton = findViewById(R.id.createEventBtn);

        addEventImage.setOnClickListener(v -> addImage());
        eventDateTime.setOnClickListener(v -> showDatePicker());
        eventLocation.setOnClickListener(v -> openPlacePicker());
        createEventButton.setOnClickListener(v -> createEvent());
    }

    private void addImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.ID
        );

        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(getApplicationContext());
        startActivityForResult(intent, LOCATION_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == LOCATION_PICK_REQUEST) {
                handleLocationResult(data);
            } else if (requestCode == IMAGE_PICK_REQUEST) {
                handleImageResult(data);
            }
        }
    }

    private void handleLocationResult(Intent data) {
        Place place = Autocomplete.getPlaceFromIntent(data);

        String locationName = place.getName();
        String locationAddress = place.getAddress();
        LatLng locationLatLng = place.getLatLng();
        locationID = place.getId();

        if (locationAddress == null) {
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
        } else {
            eventLocation.setText(locationAddress);
        }
    }

    private void handleImageResult(Intent data) {
        imageUri = data.getData();

        if (imageUri != null) {
            CardView imageCard = findViewById(R.id.roundedCardView);

            ImageView addImage = findViewById(R.id.addImageIcon);
            TextView addImageText = findViewById(R.id.addImageText);
            addImage.setVisibility(View.GONE);
            addImageText.setVisibility(View.GONE);
            imageCard.setVisibility(View.VISIBLE);
            eventImage.setImageURI(imageUri);

            try {
                Bitmap bitmap;
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                eventImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Unable to upload image to server.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            this.selectedYear = selectedYear;
            this.selectedMonth = selectedMonth;
            this.selectedDay = selectedDay;

            showTimePicker();
        }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        boolean isPM = calendar.get(Calendar.AM_PM) == Calendar.PM;

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            this.selectedHour = selectedHour;
            this.selectedMinute = selectedMinute;

            String amPm = selectedHour >= 12 ? "PM" : "AM";
            int hour12 = (selectedHour == 0) ? 12 : (selectedHour > 12 ? selectedHour - 12 : selectedHour);

            String formattedDateTime = String.format("%02d-%02d-%d %02d:%02d %s", selectedDay, selectedMonth + 1, selectedYear, hour12, selectedMinute, amPm);

            eventDateTime.setText(formattedDateTime);
        }, hour, minute, false);

        timePickerDialog.show();
    }

    private void createEvent() {
        String name = eventNameInput.getText().toString().trim();
        String description = eventDescriptionInput.getText().toString().trim();
        String dateTime = eventDateTime.getText().toString().trim();
        boolean isInPerson = switchInPerson.isChecked();
        boolean isPrivate = switchPrivate.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter an event name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description of the event", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dateTime.isEmpty()) {
            Toast.makeText(this, "Please select a date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (locationID.isEmpty() && isInPerson) {
            Toast.makeText(this, "Please select a location or set event as remote", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to create an event.", Toast.LENGTH_SHORT).show();
            return;
        }
        String createdBy = currentUser.getUid();
        String eventId = UUID.randomUUID().toString();

        if (imageUri != null) {
            uploadImageToFirebase(eventId, name, description, dateTime, isInPerson, isPrivate, createdBy);
        } else {
            saveEventToFirestore(eventId, name, description, dateTime, isInPerson, isPrivate, createdBy, null);
        }
    }

    private void uploadImageToFirebase(String eventId, String name, String description, String dateTime, boolean isInPerson, boolean isPrivate, String createdBy) {
        StorageReference fileReference = storageReference.child(eventId + ".jpg");
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveEventToFirestore(eventId, name, description, dateTime, isInPerson, isPrivate, createdBy, imageUrl);
                }))
                .addOnFailureListener(e -> Toast.makeText(CreateActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveEventToFirestore(String eventId, String name, String description, String dateTime, boolean isInPerson, boolean isPrivate, String createdBy, String imageUrl) {
        EventModel event = new EventModel(eventId, name, description, dateTime, locationID, isInPerson, isPrivate, createdBy, imageUrl, attendees);

        db.collection("events").document(eventId).set(event)
                .addOnSuccessListener(aVoid -> {
                    //refresh signal
                    PlanItOutApp app = (PlanItOutApp) getApplication();
                    SharedViewModel sharedViewModel = new ViewModelProvider(
                            app,
                            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
                    ).get(SharedViewModel.class);

                    sharedViewModel.triggerRefreshCreatedEvents();

                    Toast.makeText(CreateActivity.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(CreateActivity.this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
