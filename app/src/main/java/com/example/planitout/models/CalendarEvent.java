package com.example.planitout.models;
import java.time.LocalTime;
public class CalendarEvent {
    private String id;
    private String title;

    private String year;
    private String day;
    private String month;

    private String date;
    private String location ;
    private LocalTime startTime;
    private String createdBy;
    public CalendarEvent() {}

    public CalendarEvent(String id, String title,String year, String day, String month, String date, String location ,LocalTime startTime,String createdBy) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.day = day;
        this.month = month;
        this.date = date;
        this.location = location;
        this.startTime = startTime;
        this.createdBy = createdBy;

    }

    public String getId() {
        return id;
    }

    public String getTitle() { return title; }
    public String getYear() { return year;}
    public String getDay() { return day;}

    public String getMonth() { return month;}

    public String getDate() { return date; }
    public String getLocation() { return location; }
    public LocalTime getStartTime() {return startTime;}

    public String getEventId() {return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setYear(String year) {
        this.year = year;
    }
    public void setDay(String day) {
        this.day = day;
    }
    public void setMonth(String month) {
        this.month = month;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

}