package com.example.planitout.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.activity.result.ActivityResultLauncher;
import android.content.Intent;

import com.example.planitout.R;
import com.example.planitout.fragments.CreatedEventsFragment;
import com.example.planitout.fragments.UpcomingEventsFragment;

/**
 * Adapter for managing the ViewPager2 that switches between Upcoming and Created events.
 */
public class ViewPagerAdapter extends FragmentStateAdapter {
    private final String userId;
    private final CreatedEventsFragment createdEventsFragment;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String userId, CreatedEventsFragment createdEventsFragment) {
        super(fragmentActivity);
        this.userId = userId;
        this.createdEventsFragment = createdEventsFragment;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new UpcomingEventsFragment(userId);
        } else {
            return createdEventsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
