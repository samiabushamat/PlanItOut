package com.example.planitout.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class DotIndicator {
    String eventId;
    public int attendeeOrCreator; // 0 for attendee, 1 for creator, 2 for both
    public LocalDate date;

    public DotIndicator(String eventId, int attendeeOrCreator, LocalDate date) {
        this.eventId = eventId;
        this.attendeeOrCreator = attendeeOrCreator;
        this.date = date;
    }
}
