package com.example.planitout.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    private String eventId;
    private String name;
    private String createdBy;
    private String dateTime;
    private String location;
    private String streetAddress;
    private String cityStateZip;
    private Boolean isPrivate;
    private Boolean isInPerson;
    private Map<String, String> attendees;
    public Event() {
        this.attendees = new HashMap<>();
    }


    public Event(String eventId, String name, String createdBy, String dateTime, String location,String streetAddress,String cityStateZip, Boolean isPrivate, Boolean isInPerson) {
        this.eventId = eventId;
        this.name = name;
        this.createdBy = createdBy;
        this.dateTime = dateTime;
        this.cityStateZip = cityStateZip;
        this.streetAddress = streetAddress;
        this.location = location;
        this.isPrivate = isPrivate;
        this.isInPerson = isInPerson;
        this.attendees = new HashMap<>();
    }

    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getCreatedBy() { return createdBy; }
    public String getDateTime() { return dateTime; }

    public String getLocation() { return location; }

    public String getAddress() {return streetAddress;}
    public String getCityStateZip() {return cityStateZip;}

    public Boolean getIsPrivate() { return isPrivate; }
    public Boolean getIsInPerson() { return isInPerson; }

    public Map<String, String> getAttendees() {
        return attendees;
    }
    public void setAttendees(Map<String, String> attendees) {
        this.attendees = attendees;
    }
    public void addAttendee(String uid, String username) {
        if (!attendees.containsKey(uid)) {
            attendees.put(uid, username);
        }
    }
}
