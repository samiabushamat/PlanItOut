package com.example.planitout.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planitout.R;
import com.example.planitout.models.CalendarEvent;
import com.example.planitout.models.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.ViewHolder> {
    private  List<CalendarEvent> eventList = Collections.emptyList();
    private  OnEventClickListener onEventClickListener;
    private Context context;

    public interface OnEventClickListener {
        void onEventClick(String eventId);
    }

    public CalendarEventAdapter(Context context, List<CalendarEvent> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.onEventClickListener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_event_card, parent, false);
        return new ViewHolder(view, onEventClickListener, eventList);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarEvent event = eventList.get(position);
        holder.eventTitle.setText(event.getTitle());
        holder.eventDate.setText(event.getDate());
        holder.eventLocation.setText(event.getLocation());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String formattedTime = event.getStartTime().format(formatter);
        holder.eventTime.setText("Time: " + formattedTime);
        holder.itemView.setOnClickListener(v -> {
            if (onEventClickListener != null) {
                onEventClickListener.onEventClick(event.getEventId());
            }
        });

    }


    public void sortEventsByDateAndTime() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

        eventList.sort((event1, event2) -> {
            try {
                LocalDate date1 = LocalDate.parse(event1.getDate(), dateFormatter);
                LocalDate date2 = LocalDate.parse(event2.getDate(), dateFormatter);

                // First compare by date
                int dateComparison = date1.compareTo(date2);
                if (dateComparison != 0) return dateComparison;

                // If dates are equal, compare by time
                LocalTime time1 = event1.getStartTime();
                LocalTime time2 = event2.getStartTime();

                return time1.compareTo(time2);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle, eventDate, eventLocation, eventTime;

        public ViewHolder(@NonNull View itemView, OnEventClickListener listener, List<CalendarEvent> eventList) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            eventTime = itemView.findViewById(R.id.eventTime);
        }
    }
}