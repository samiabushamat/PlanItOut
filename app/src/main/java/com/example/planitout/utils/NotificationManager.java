package com.example.planitout.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.planitout.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class NotificationManager {

    private static NotificationManager instance;
    private DatabaseReference notifRef;
    private ValueEventListener notifListener;
    private String userId;

    private NotificationManager(String userId) {
        this.userId = userId;
        this.notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");
    }

    public static synchronized NotificationManager getInstance(String userId) {
        if (instance == null) {
            instance = new NotificationManager(userId);
        }
        return instance;
    }
    /*
    Function to listen for live notifications to keep live badge count
     */
    public void startListeningForBadge(Context context, View notificationButton) {
        stopListening();

        notifListener = notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("BadgeListener", "Notification count = " + snapshot.getChildrenCount());
                ViewGroup parent = (ViewGroup) notificationButton.getParent();
                View existingBadge = parent.findViewWithTag("notifBadge");
                if (existingBadge != null) {
                    parent.removeView(existingBadge);
                }

                int count = (int) snapshot.getChildrenCount();
                if (count > 0) {
                    addBadge(context, parent, count);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("NotificationManager", "Error fetching notifications: " + error.getMessage());
            }
        });
    }

    public void stopListening() {
        if (notifListener != null && notifRef != null) {
            notifRef.removeEventListener(notifListener);
        }
    }

    private void addBadge(Context context, ViewGroup parent, int count) {
        TextView badge = new TextView(context);
        badge.setTag("notifBadge");
        badge.setText(String.format(Locale.getDefault(), "%d", count));
        badge.setTextColor(ContextCompat.getColor(context, R.color.white));
        badge.setTextSize(12);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackground(ContextCompat.getDrawable(context, R.drawable.badge_background));

        int badgeSize = (int) (24 * context.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(badgeSize, badgeSize);
        badge.setLayoutParams(params);

        parent.addView(badge);
    }
    /*
    Function to fetch notifications from database and display them
     */
    public void showNotificationPopup(Context context, View anchorView, String userId) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.notification_popup, null);
        LinearLayout notificationContainer = popupView.findViewById(R.id.notification_container);
        Button clearButton = popupView.findViewById(R.id.clearButton);

        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setElevation(20);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("notifications");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationContainer.removeAllViews();

                for (DataSnapshot notifSnapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> notifData = (Map<String, Object>) notifSnapshot.getValue();

                    if (notifData != null) {
                        String type = (String) notifData.get("type");

                        if ("friend_request".equals(type)) {
                            handleFriendRequest(context, notificationContainer, notifData, notifSnapshot, popupWindow, userId);
                        } else {
                            handleRegularNotification(context, notificationContainer, notifData);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(v -> {
            userRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    notificationContainer.removeAllViews();
                    TextView clearedText = new TextView(context);
                    clearedText.setText("Notifications cleared");
                    clearedText.setTextColor(Color.DKGRAY);
                    clearedText.setPadding(32, 32, 32, 32);
                    clearedText.setTextSize(16);
                    notificationContainer.addView(clearedText);
                } else {
                    Toast.makeText(context, "Failed to clear notifications.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        popupWindow.showAtLocation(anchorView, Gravity.TOP | Gravity.START, 50, 150);
    }
    /*
    Helper function to handle friend request dialog in notifications
     */
    private void handleFriendRequest(Context context, ViewGroup container, Map<String, Object> notifData, DataSnapshot notifSnapshot, PopupWindow popupWindow, String userId) {
        String fromUsername = (String) notifData.get("fromUsername");
        String fromUid = (String) notifData.get("fromUid");

        LinearLayout requestLayout = new LinearLayout(context);
        requestLayout.setOrientation(LinearLayout.VERTICAL);
        requestLayout.setPadding(24, 24, 24, 24);
        requestLayout.setBackgroundResource(R.drawable.notification_item_bg);

        TextView title = new TextView(context);
        title.setText(fromUsername + " wants to be your friend");
        title.setTextColor(ContextCompat.getColor(context, R.color.tanLight));
        title.setTextSize(15);

        LinearLayout buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);

        Button acceptBtn = new Button(context);
        acceptBtn.setText("Accept");
        acceptBtn.setBackgroundResource(R.drawable.button_accept);
        acceptBtn.setTextColor(ContextCompat.getColor(context, R.color.white));
        acceptBtn.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
            userRef.child(userId).child("friends").child(fromUid).setValue(true);
            userRef.child(fromUid).child("friends").child(userId).setValue(true);
            userRef.child(userId).child("notifications").child(notifSnapshot.getKey()).removeValue();
            popupWindow.dismiss();
        });

        Button declineBtn = new Button(context);
        declineBtn.setText("Decline");
        declineBtn.setBackgroundResource(R.drawable.button_decline);
        declineBtn.setTextColor(ContextCompat.getColor(context, R.color.white));
        declineBtn.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
            userRef.child(userId).child("notifications").child(notifSnapshot.getKey()).removeValue();
            popupWindow.dismiss();
        });

        buttonRow.addView(acceptBtn);
        buttonRow.addView(declineBtn);

        requestLayout.addView(title);
        requestLayout.addView(buttonRow);
        container.addView(requestLayout);
    }
    /*
    Helper function to handle regular notification fetching
     */
    private void handleRegularNotification(Context context, ViewGroup container, Map<String, Object> notifData) {
        String message = (String) notifData.get("message");
        Object timestampRaw = notifData.get("timestamp");

        String timeFormatted = "";
        if (timestampRaw instanceof Long) {
            long timestamp = (Long) timestampRaw;
            Date date = new Date(timestamp);
            DateFormat formatter = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            timeFormatted = formatter.format(date);
        }

        TextView notifText = new TextView(context);
        notifText.setText(message + "\n" + timeFormatted);
        notifText.setTextColor(ContextCompat.getColor(context, R.color.tanLight));
        notifText.setPadding(24, 16, 24, 16);
        notifText.setTextSize(15);
        notifText.setBackgroundResource(R.drawable.notification_item_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        notifText.setLayoutParams(params);

        container.addView(notifText);
    }


}
