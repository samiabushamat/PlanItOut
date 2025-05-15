package com.example.planitout.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.planitout.BuildConfig;
import com.example.planitout.PlanItOutApp;
import com.example.planitout.activities.EditEventActivity;
import com.example.planitout.activities.MainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planitout.R;
import com.example.planitout.adapters.EventAdapter;
import com.example.planitout.models.Event;
import com.google.android.libraries.places.api.model.AddressComponent;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.planitout.models.SharedViewModel;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

/**
 * Fragment that displays the list of user-created events.
 */
public class CreatedEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private String userId;
    private PlacesClient placesClient;
    private SharedViewModel sharedViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
     String key = BuildConfig.GOOGLE_API_KEY;

    public CreatedEventsFragment() {
        // Required empty public constructor
    }

    public static CreatedEventsFragment newInstance(String userId) {
        CreatedEventsFragment fragment = new CreatedEventsFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_created_events, container, false);
        db = FirebaseFirestore.getInstance();
        eventList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recyclerView_created);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(getContext(), eventList, this);

        recyclerView.setAdapter(eventAdapter);

        PlanItOutApp app = (PlanItOutApp) requireActivity().getApplication();
        sharedViewModel = new ViewModelProvider(
                app,
                ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(SharedViewModel.class);

        observeRefreshSignal();

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), key);
        }
        placesClient = Places.createClient(requireContext());

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutCreated);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadCreatedEvents();
        });

        // Retrieve userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }

        // Load created events for this user
        loadCreatedEvents();

        return view;
    }

    public void launchEditEvent(String eventId) {
        Intent intent = new Intent(getContext(), EditEventActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    private void loadCreatedEvents() {
        Log.d("CreatedEventsFragment", "loadCreatedEvents: Refresh triggered. Fetching latest created events.");

        db.collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        if (task.getResult().isEmpty()) {
                            Log.d("CreatedEventsFragment", "No events found. Notifying adapter.");
                            eventAdapter.notifyDataSetChanged();
                            return;
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String thisEventId = document.getId();
                            String name = document.getString("name");
                            String createdBy = document.getString("createdBy");
                            String dateTime = document.getString("dateTime");
                            String locationID = document.getString("locationId");
                            Boolean isPrivate = document.getBoolean("private");
                            Boolean isInPerson = document.getBoolean("inPerson");

                            fetchLocationInfo(thisEventId, locationID, name, createdBy, dateTime, isPrivate, isInPerson);
                        }
                    }
                });
        swipeRefreshLayout.setRefreshing(false);

    }

    private void fetchLocationInfo(String id, String locationId, String name, String createdBy, String dateTime, Boolean isPrivate, Boolean isInPerson) {
        if (locationId == null || locationId.isEmpty()) {
            Log.d("PlacesAPI", "Invalid locationId, using default value.");
            addEventToList(id, name, createdBy, dateTime, "Unknown Location", "Unknown Address", "Unknown City/State/ZIP", isPrivate, isInPerson);
            return;
        }

        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            String locationName = place.getName();
            String fullAddress = place.getAddress();
            String streetNumber = "", route = "", city = "", state = "", zip = "";

            // Extracting components from the address
            if (place.getAddressComponents() != null) {
                List<AddressComponent> components = place.getAddressComponents().asList();
                for (AddressComponent component : components) {
                    if (component.getTypes().contains("street_number"))
                        streetNumber = component.getName();
                    if (component.getTypes().contains("route")) route = component.getName();
                    if (component.getTypes().contains("locality")) city = component.getName();
                    if (component.getTypes().contains("administrative_area_level_1"))
                        state = component.getName();
                    if (component.getTypes().contains("postal_code")) zip = component.getName();
                }
            }

            final String finalStreetAddress = streetNumber.isEmpty() ? route : streetNumber + " " + route;
            final String finalCityStateZip = (!city.isEmpty() ? city + ", " : "") +
                    (!state.isEmpty() ? state + " " : "") +
                    (!zip.isEmpty() ? zip : "");

            Log.d("PlacesAPI", "Fetched location: " + locationName + " | " + finalStreetAddress + " | " + finalCityStateZip);

            addEventToList(id, name, createdBy, dateTime, locationName, finalStreetAddress, finalCityStateZip, isPrivate, isInPerson);

        }).addOnFailureListener(e -> {
            Log.e("PlacesAPI", "Failed to fetch location", e);
            addEventToList(id, name, createdBy, dateTime, "Unknown Location", "Unknown Address", "Unknown City/State/ZIP", isPrivate, isInPerson);
        });
    }

    private void addEventToList(String eventId, String name, String createdBy, String dateTime, String locationName, String streetAddress, String cityStateZip, Boolean isPrivate, Boolean isInPerson) {
        Event event = new Event(eventId, name, createdBy, dateTime, locationName, streetAddress, cityStateZip, isPrivate, isInPerson);

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(document -> {
                    HashMap<String, String> attendeesMap = (HashMap<String, String>) document.get("attendees");

                    if (attendeesMap != null) {
                        event.setAttendees(attendeesMap);
                    } else {
                        event.setAttendees(new HashMap<>());
                    }

                    eventList.add(event);
                    eventAdapter.updateList(eventList);
                    eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch attendees", e);
                    eventList.add(event); // Add event without attendees
                    eventAdapter.updateList(eventList);
                    eventAdapter.notifyDataSetChanged();
                });
    }

    private void observeRefreshSignal() {
        sharedViewModel.getRefreshCreatedEvents().observe(getViewLifecycleOwner(), shouldRefresh -> {
            if (shouldRefresh != null && shouldRefresh) {
                Log.d("CreatedEventsFragment", "✅ Refreshing events...");
                loadCreatedEvents(); //refresh the events
                sharedViewModel.clearRefreshCreatedEvents(); //clear the refresh flag
            }
        });
    }
}