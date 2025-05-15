package com.example.planitout.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.planitout.R;
import com.example.planitout.models.Event;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingEventsAdapter extends RecyclerView.Adapter<UpcomingEventsAdapter.UpcomingEventViewHolder> {
    private List<Event> eventList;
    private Context context;
    private OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(String eventId);
    }

    public UpcomingEventsAdapter(Context context, List<Event> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.onEventClickListener = listener;
    }

    @NonNull
    @Override
    public UpcomingEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.upcomming_event_card, parent, false);
        return new UpcomingEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingEventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.eventTitle.setText(event.getName());
        holder.eventCreatedBy.setText("Created by: " + event.getCreatedBy());
        holder.eventDate.setText(formatDate(event.getDateTime()));
        holder.eventTime.setText(formatTime(event.getDateTime()));
        holder.eventStreet.setText(event.getAddress());
        holder.eventCityStateZip.setText(event.getCityStateZip());

        // set click listener to start EventDetailActivity
        holder.itemView.setOnClickListener(v -> {
            if (onEventClickListener != null) {
                onEventClickListener.onEventClick(event.getEventId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class UpcomingEventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle, eventCreatedBy, eventDate, eventTime, eventStreet, eventCityStateZip;

        public UpcomingEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.upcomingEventTitle);
            eventCreatedBy = itemView.findViewById(R.id.upcomingEventCreatedBy);
            eventDate = itemView.findViewById(R.id.upcomingEventDate);
            eventTime = itemView.findViewById(R.id.upcomingEventTime);
            eventStreet = itemView.findViewById(R.id.upcomingEventStreet);
            eventCityStateZip = itemView.findViewById(R.id.upcomingEventCityStateZip);
        }
    }


    /**
     * Converts a database formatted string into a readable date format.
     */
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

    /**
     * Converts a database formatted string into a readable time format.
     */
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

    public void updateList(List<Event> filteredList) {
        this.eventList.clear();
        this.eventList.addAll(filteredList);
        notifyDataSetChanged();
    }
}
