package com.example.planitout.adapters;

import android.app.AlertDialog;
import android.content.Context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planitout.PlanItOutApp;


import com.example.planitout.R;
import com.example.planitout.fragments.CreatedEventsFragment;
import com.example.planitout.models.Event;
import com.example.planitout.models.SharedViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> eventList;
    private final Context context;
    private final CreatedEventsFragment parentFragment;

    public EventAdapter(Context context, List<Event> eventList, CreatedEventsFragment parentFragment) {
        this.context = context;
        this.eventList = eventList != null ? new ArrayList<>(eventList) : new ArrayList<>();
        this.parentFragment = parentFragment;
        sortEventsByDate();
    }

    private void sortEventsByDate() {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
        eventList.sort((event1, event2) -> {
            try {
                Date date1 = format.parse(event1.getDateTime());
                Date date2 = format.parse(event2.getDateTime());
                return date1.compareTo(date2);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        String eventId = event.getEventId();
        holder.eventName.setText(event.getName());
        holder.eventDate.setText(formatDate(event.getDateTime()));
        holder.eventTime.setText(formatTime(event.getDateTime()));
        holder.eventLocation.setText(event.getLocation());
        holder.eventStreet.setText(event.getAddress());
        holder.eventCityStateZip.setText(event.getCityStateZip());

        holder.viewAttendeesButton.setOnClickListener(v -> showAttendeesPopup(event.getAttendees(),eventId));
        holder.editEventButton.setOnClickListener(v -> {
            parentFragment.launchEditEvent(event.getEventId());
        });

        setupDeleteButton(holder.itemView, event.getEventId());
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventDate, eventTime, eventLocation, eventStreet, eventCityStateZip;
        Button viewAttendeesButton, editEventButton;
        ImageButton deleteButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventTime = itemView.findViewById(R.id.eventTime);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            eventStreet = itemView.findViewById(R.id.eventStreet);
            eventCityStateZip = itemView.findViewById(R.id.eventCityStateZip);
            viewAttendeesButton = itemView.findViewById(R.id.viewAttendeesButton);
            editEventButton = itemView.findViewById(R.id.editEventButton);
            deleteButton = itemView.findViewById(R.id.deleteEventButton);
        }
    }

    private String formatDate(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateTime;
        }
    }

    private String formatTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateTime;
        }
    }
    private void showAttendeesPopup(Map<String, String> attendees, String eventId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_attendees, null);
        builder.setView(dialogView);
        ListView attendeesListView = dialogView.findViewById(R.id.attendeesListView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        List<String> attendeeKeys = new ArrayList<>(attendees.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.dialog_attendee_item, attendeeKeys) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView = LayoutInflater.from(context).inflate(R.layout.dialog_attendee_item, parent, false);
                TextView attendeeName = itemView.findViewById(R.id.attendeeName);
                ImageButton deleteButton = itemView.findViewById(R.id.deleteAttendeeButton);
                String key = getItem(position);
                String name = attendees.get(key);
                attendeeName.setText(name);
                deleteButton.setOnClickListener(v -> {
                    attendees.remove(key);
                    attendeeKeys.remove(position);
                    notifyDataSetChanged();

                    FirebaseFirestore.getInstance().collection("events")
                            .document(eventId)
                            .update("attendees", attendees)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Attendee removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to remove attendee", Toast.LENGTH_SHORT).show();
                            });
                });
                return itemView;
            }
        };
        attendeesListView.setAdapter(adapter);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void setupDeleteButton(View itemView, String eventId) {
        PlanItOutApp app = (PlanItOutApp) parentFragment.requireActivity().getApplication();
        SharedViewModel viewModel = new ViewModelProvider(
                app,
                ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(SharedViewModel.class);

        ImageButton deleteButton = itemView.findViewById(R.id.deleteEventButton);
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("events")
                                .document(eventId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(itemView.getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                    viewModel.triggerRefreshCreatedEvents();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(itemView.getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
    public void updateList(List<Event> newList) {
        eventList.clear();
        eventList.addAll(newList);
        notifyDataSetChanged();
    }

}
