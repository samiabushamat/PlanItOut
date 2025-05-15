package com.example.planitout.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.planitout.R;
import com.example.planitout.adapters.ViewPagerAdapter;
import com.example.planitout.fragments.CreatedEventsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.planitout.utils.NotificationManager;


public class MainActivity extends AppCompatActivity {
    private View tabUpcoming, tabCreated;
    private TextView tab_title;
    private ViewPager2 viewPager;
    private CreatedEventsFragment createdEventsFragment;
    private ImageView profileButton;
    private BottomNavigationView bottomNavigationView;
    ImageButton notificationButton;
    private NotificationManager notifManager;

    private PopupWindow notificationPopup;
    private String userID;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
    private Date now;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize UI components
        tabUpcoming = findViewById(R.id.tab_upcoming);
        tabCreated = findViewById(R.id.tab_created);
        tab_title = findViewById(R.id.tab_title);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        profileButton = findViewById(R.id.profile);
        notificationButton = findViewById(R.id.notification_icon);

        //get the logged-in user's UID and username
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    
        now = new Date();   
        checkTodayEvents(userID);

        //start notification badge listening
        notifManager = NotificationManager.getInstance(userID);
        notifManager.startListeningForBadge(this, notificationButton);
        notificationButton.setOnClickListener(view -> {
            notifManager.showNotificationPopup(this, findViewById(android.R.id.content), userID);
        });

        //create adapter and pass the fragment
        createdEventsFragment = CreatedEventsFragment.newInstance(userID);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this, userID, createdEventsFragment);
        viewPager.setAdapter(adapter);

        //tab click handlers
        tabUpcoming.setOnClickListener(view -> switchTab(true));
        tabCreated.setOnClickListener(view -> switchTab(false));

        //disable default animation
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        //bottom nav menu button handlers
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemID = item.getItemId();
            item.setChecked(false);

            if (itemID == R.id.nav_calendar) {
                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
            } else if (itemID == R.id.nav_add_event) {
                Intent intent = new Intent(MainActivity.this, CreateActivity.class);
                startActivity(intent);
            } else if (itemID == R.id.nav_map) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }

            return false;
        });

        // handle tab switching using ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switchTab(position == 0);
            }
        });

        //profile button handler
        profileButton.setOnClickListener(profile -> {
            Intent intent = new Intent(MainActivity.this, ProfileViewActivity.class);
            intent.putExtra("userID", userID);
            startActivity(intent);
        });
    }

    /*
     Function that checks if any events in the database that user is signed up for is today
      */
    private void checkTodayEvents(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        firestore.collection("events").get().addOnSuccessListener(querySnapshot -> {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                String eventName = doc.getString("name");
                String dateTime = doc.getString("dateTime");
                Map<String, String> attendees = new HashMap<>();

                try {
                    Date eventDate = sdf.parse(dateTime);
                    if (eventDate != null && eventDate.before(now)) {
                        doc.getReference().delete();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (eventName != null && dateTime != null && attendees != null && attendees.containsKey(userId)) {
                    if (isEventToday(dateTime)) {
                        String message = "Reminder: '" + eventName + "' is happening today!";

                        //check if the same message already exists in the user's notifications
                        userRef.child("notifications").orderByChild("message").equalTo(message)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (!snapshot.exists()) {
                                            Map<String, Object> notif = new HashMap<>();
                                            notif.put("message", message);
                                            notif.put("timestamp", System.currentTimeMillis());

                                            userRef.child("notifications").push().setValue(notif);
                                            Log.d("EventToday", "✅ Sent: " + message);
                                        } else {
                                            Log.d("EventToday", "⏭️ Skipped duplicate: " + message);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("EventToday", "❌ Error checking notifications: " + error.getMessage());
                                    }
                                });
                    }
                }
            }
        });
    }
    /*
    Helper function for checkTodayEvents
     */
    private boolean isEventToday(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
        sdf.setLenient(false);

        try {
            Date eventDate = sdf.parse(dateTimeStr);
            Calendar eventCal = Calendar.getInstance();
            eventCal.setTime(eventDate);

            Calendar todayCal = Calendar.getInstance();
            return eventCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    eventCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            Log.e("checkTodayEvents", "Failed to parse date: " + dateTimeStr, e);
            return false;
        }
    }

    private void switchTab(boolean isUpcoming) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tab_title, "alpha", 1f, 0f);
        fadeOut.setDuration(200);

        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //change the title text when faded out
                if (isUpcoming) {
                    tab_title.setText("Upcoming Events");
                } else {
                    tab_title.setText("Created Events");
                }

                //fade in the new title
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tab_title, "alpha", 0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.start();
            }
        });

        fadeOut.start();
        tabUpcoming.setSelected(isUpcoming);
        tabCreated.setSelected(!isUpcoming);

        //fade animation for dots
        ObjectAnimator fadeGlow = ObjectAnimator.ofFloat(isUpcoming ? tabUpcoming : tabCreated, "alpha", 0.5f, 1f);
        fadeGlow.setDuration(300);
        fadeGlow.start();

        viewPager.setCurrentItem(isUpcoming ? 0 : 1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notifManager != null) {
            notifManager.stopListening();
        }
    }
}
