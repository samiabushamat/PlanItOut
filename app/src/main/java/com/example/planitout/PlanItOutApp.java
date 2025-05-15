package com.example.planitout;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

public class PlanItOutApp extends Application implements ViewModelStoreOwner {
    private ViewModelStore viewModelStore;
    private String currentUsername;

    @Override
    public void onCreate() {
        super.onCreate();
        viewModelStore = new ViewModelStore();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
}