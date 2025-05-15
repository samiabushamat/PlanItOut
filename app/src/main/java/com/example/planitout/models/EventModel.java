package com.example.planitout.models;

import java.util.HashMap;
import java.util.Map;

public class EventModel {
    private String eventId;
    private String name;
    private String description;
    private String dateTime;
    private String locationID;
    private boolean isInPerson;
    private boolean isPrivate;
    private String createdBy;
    private String imageUrl;
    private Map<String, String> attendees; // UID → username

    // Empty constructor for Firebase
    public EventModel() {
        attendees = new HashMap<>();
    }

    public EventModel(String eventId, String name, String description, String dateTime, String locationID,
                      boolean isInPerson, boolean isPrivate, String createdBy, String imageUrl, Map<String, String> attendees) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.dateTime = dateTime;
        this.locationID = locationID;
        this.isInPerson = isInPerson;
        this.isPrivate = isPrivate;
        this.createdBy = createdBy;
        this.imageUrl = imageUrl;
        this.attendees = attendees;
    }

    public String getEventId() {
        return eventId;
    }
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateTime() {
        return dateTime;
    }
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getLocationId() {
        return locationID;
    }
    public void setLocationId(String locationId) {
        this.locationID = locationId;
    }

    public boolean isInPerson() {
        return isInPerson;
    }
    public void setInPerson(boolean inPerson) {
        isInPerson = inPerson;
    }

    public boolean isPrivate() {
        return isPrivate;
    }
    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Map<String, String> getAttendees() {
        return attendees;
    }
    public void setAttendees(Map<String, String> attendees) {
        this.attendees = attendees;
    }

    public void addAttendee(String uid, String username) {
        if (attendees == null) attendees = new HashMap<>();
        attendees.put(uid, username);
    }

    public void removeAttendee(String uid) {
        if (attendees != null) {
            attendees.remove(uid);
        }
    }
}