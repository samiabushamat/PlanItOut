package com.example.planitout.models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
public class SharedViewModel extends AndroidViewModel {
    private final MutableLiveData<Boolean> refreshCreatedEvents = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> refreshUpcomingEvents = new MutableLiveData<>(false);
    public SharedViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getRefreshCreatedEvents() {
        return refreshCreatedEvents;
    }

    public LiveData<Boolean> getRefreshUpcomingEvents() {
        return refreshUpcomingEvents;
    }

    public void triggerRefreshCreatedEvents() {
        Log.d("SharedViewModel", "triggerRefreshCreatedEvents: Signal sent to refresh CreatedEventsFragment");
        refreshCreatedEvents.setValue(true);
    }

    public void triggerRefreshUpcomingEvents() {
        refreshUpcomingEvents.setValue(true);
    }

    public void clearRefreshCreatedEvents() {
        Log.d("SharedViewModel", "clearRefreshSignal: Refresh signal cleared");
        refreshCreatedEvents.setValue(false);
    }

    public void clearRefreshUpcomingEvents() {
        refreshUpcomingEvents.setValue(false);
    }

}
