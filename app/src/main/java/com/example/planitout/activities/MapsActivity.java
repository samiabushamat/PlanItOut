package com.example.planitout.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.planitout.BuildConfig;
import com.example.planitout.R;
import com.example.planitout.models.EventModel;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.checkerframework.checker.units.qual.C;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private EditText locationAddress;
    private TextView locationText;
    private FirebaseFirestore db;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    String key = BuildConfig.GOOGLE_API_KEY;

    HashMap<Marker, String> markerEventMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationAddress = findViewById(R.id.addressSearch);
        locationText = findViewById(R.id.locationText);

        Places.initialize(getApplicationContext(), key);
        db = FirebaseFirestore.getInstance();

        locationAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.FORMATTED_ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                        fieldList).build(MapsActivity.this);
                startActivityForResult(intent, 100);
            }
        });

        // initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // check if intent with location detail is received
        Intent intent = getIntent();
        if (intent.hasExtra("eventLocationLatLng")) {
            // extract LatLng and move camera
            LatLng eventLocation = intent.getParcelableExtra("eventLocationLatLng");
            String eventLocationName = intent.getStringExtra("eventLocationName");

            if (eventLocation != null) {
                updateMapWithLocation(eventLocation, eventLocationName);
            } else {
                checkLocationPermission();
            }
        }

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            String address = place.getFormattedAddress();
            LatLng latLng = place.getLocation();

            locationText.setText(address);
            updateMapWithLocation(latLng, address);
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(getApplicationContext(), status.getStatusMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Check for location permissions
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get the last known location
            getLastLocation();
        }
    }

    // Get the last known location
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    updateMap();
                } else {
                    // If the last location is null, request a fresh location
                    Toast.makeText(MapsActivity.this, "Getting fresh location...", Toast.LENGTH_SHORT).show();
                    requestNewLocationData();
                }
            }
        });
    }

    // Request a fresh location update
    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        updateMap();
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                }
            }
        }, Looper.getMainLooper());
    }

    // Update the map with the current location
    private void updateMap() {
        if (mMap != null && currentLocation != null) {
            LatLng location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private void updateMapWithLocation(LatLng location, String address) {
        if (mMap != null && location != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the last known location
                getLastLocation();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Location access is not available!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Called when the map is ready to be used
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable the My Location layer if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Update the map with the current location
        if (currentLocation != null) {
            updateMap();
        }

        loadEventMarkers();

        mMap.setOnMarkerClickListener(marker -> {
            String eventId = markerEventMap.get(marker);

            if (eventId != null) {
                checkEventCreator(eventId);
            }
            return true;
        });
    }

    private void checkEventCreator(String eventId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("events").document(eventId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                EventModel event = task.getResult().toObject(EventModel.class);

                if (event != null) {
                    if (event.getCreatedBy().equals(currentUserId)) {
                        // Open EditEventActivity if the user is the creator
                        Intent intent = new Intent(MapsActivity.this, EditEventActivity.class);
                        intent.putExtra("eventId", eventId);
                        startActivity(intent);
                    } else {
                        // Open EventDetailActivity for other users
                        Intent intent = new Intent(MapsActivity.this, EventDetailActivity.class);
                        intent.putExtra("eventId", eventId);
                        startActivity(intent);
                    }
                }
            } else {
                Toast.makeText(MapsActivity.this, "Failed to load event details.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void loadEventMarkers() {
        CollectionReference eventsRef = db.collection("events");

        eventsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    EventModel event = document.toObject(EventModel.class);
                    String placeID = event.getLocationId();

                    if (placeID != null) {
                        List<Place.Field> placeFields = Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.LOCATION);
                        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeID, placeFields);

                        Places.createClient(this).fetchPlace(request).addOnSuccessListener(response -> {
                            Place place = response.getPlace();
                            LatLng latLng = place.getLocation();
                            String locationName = place.getDisplayName();

                            if (latLng != null) {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(locationName));

                                if (marker != null) {
                                    markerEventMap.put(marker, event.getEventId());
                                }
                            }
                        }).addOnFailureListener(exception -> {
                            Toast.makeText(this, "Failed to fetch location for event: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                    }
                }
            } else {
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }
}