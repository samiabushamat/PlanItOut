package com.example.planitout.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;
import androidx.activity.result.ActivityResultLauncher;
import com.bumptech.glide.Glide;
import com.example.planitout.PlanItOutApp;
import com.example.planitout.R;
import com.example.planitout.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsPopup {
    private final Context context;
    private final DatabaseReference userRef;
    private final List<UserModel> friendsList;
    private final LayoutInflater inflater;
    private View dialogView;
    private final ActivityResultLauncher<Intent> profileLauncher;

    public FriendsPopup(Context context, DatabaseReference userRef, List<UserModel> friendsList, ActivityResultLauncher<Intent> profileLauncher) {
        this.context = context;
        this.userRef = userRef;
        this.friendsList = friendsList;
        this.profileLauncher = profileLauncher;
        this.inflater = LayoutInflater.from(context);
    }


    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        dialogView = inflater.inflate(R.layout.dialog_friends, null);
        builder.setView(dialogView);

        ListView attendeesListView = dialogView.findViewById(R.id.friendsListView);
        ListView searchResultsList = dialogView.findViewById(R.id.searchResultsList);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        EditText searchInput = dialogView.findViewById(R.id.searchFriendInput);

        ImageButton addFriendButton = dialogView.findViewById(R.id.addFriendButton);
        ImageButton backToFriendsButton = dialogView.findViewById(R.id.backToFriendsButton);
        View friendsListSection = dialogView.findViewById(R.id.friendsListSection);
        View addFriendSection = dialogView.findViewById(R.id.addFriendSection);
        View popupTitleBar = dialogView.findViewById(R.id.popupTitleBar);
        View searchTitleBar = dialogView.findViewById(R.id.searchTitleBar);

        addFriendButton.setOnClickListener(v -> {
            popupTitleBar.setVisibility(View.GONE);
            searchTitleBar.setVisibility(View.VISIBLE);
            friendsListSection.setVisibility(View.GONE);
            addFriendSection.setVisibility(View.VISIBLE);
        });

        backToFriendsButton.setOnClickListener(v -> {
            popupTitleBar.setVisibility(View.VISIBLE);
            searchTitleBar.setVisibility(View.GONE);
            addFriendSection.setVisibility(View.GONE);
            friendsListSection.setVisibility(View.VISIBLE);
        });

        FriendAdapter adapter = new FriendAdapter();
        attendeesListView.setAdapter(adapter);

        List<UserModel> searchResults = new ArrayList<>();
        SearchAdapter searchAdapter = new SearchAdapter(searchResults);
        searchResultsList.setAdapter(searchAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                searchResults.clear();

                if (!query.isEmpty()) {
                    FirebaseDatabase.getInstance().getReference("users")
                            .orderByChild("username")
                            .startAt(query)
                            .endAt(query + "\uf8ff")
                            .limitToFirst(10)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                for (DataSnapshot userSnap : snapshot.getChildren()) {
                                    UserModel foundUser = userSnap.getValue(UserModel.class);
                                    if (!FirebaseAuth.getInstance().getUid().equals(foundUser.getUid())) {
                                        foundUser.setUid(userSnap.getKey());
                                        searchResults.add(foundUser);
                                    }
                                }
                                searchAdapter.notifyDataSetChanged();
                            });
                } else {
                    searchAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private class FriendAdapter extends ArrayAdapter<UserModel> {
        public FriendAdapter() {
            super(context, R.layout.dialog_friends_item, friendsList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = inflater.inflate(R.layout.dialog_friends_item, parent, false);
            }

            UserModel user = getItem(position);
            TextView friendName = itemView.findViewById(R.id.friendName);
            ImageView friendProfilePic = itemView.findViewById(R.id.friendProfilePic);

            if (user != null) {
                friendName.setText(user.getUsername());

                Glide.with(context)
                        .load(user.getProfilePicture())
                        .placeholder(R.drawable.baseline_account_circle_24)
                        .circleCrop()
                        .into(friendProfilePic);

                itemView.setOnClickListener(v -> {
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                Intent intent = new Intent(context, FriendProfileViewActivity.class);
                                intent.putExtra("userID", user.getUid());
                                profileLauncher.launch(intent);
                            })
                            .start();
                });
            }

            return itemView;
        }
    }

    private class SearchAdapter extends ArrayAdapter<UserModel> {
        private final List<UserModel> searchResults;

        public SearchAdapter(List<UserModel> searchResults) {
            super(context, R.layout.dialog_friends_item, searchResults);
            this.searchResults = searchResults;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = inflater.inflate(R.layout.dialog_friends_item, parent, false);
            }

            UserModel user = getItem(position);
            TextView friendName = itemView.findViewById(R.id.friendName);
            ImageView friendProfilePic = itemView.findViewById(R.id.friendProfilePic);

            if (user != null) {
                friendName.setText(user.getUsername());

                Glide.with(context)
                        .load(user.getProfilePicture())
                        .placeholder(R.drawable.baseline_account_circle_24)
                        .circleCrop()
                        .into(friendProfilePic);

                itemView.setOnClickListener(v -> {
                    //Log.d("FRIEND_REQUEST", "Click triggered for: " + user.getUsername());
                    String currentUid = FirebaseAuth.getInstance().getUid();
                    if (user.getUid().equals(currentUid)) {
                        Toast.makeText(context, "You can't add yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (UserModel friend : friendsList) {
                        if (friend.getUid().equals(user.getUid())) {
                            Toast.makeText(context, "You're already friends with this user!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(user.getUid())
                            .child("notifications");
                    //check for existing friend request first
                    notifRef.orderByChild("fromUid")
                            .equalTo(currentUid)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                //Log.d("FRIEND_REQUEST", "Snapshot size: " + snapshot.getChildrenCount());
                                boolean alreadyRequested = false;
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    if ("friend_request".equals(snap.child("type").getValue(String.class))) {
                                        Log.d("FRIEND_REQUEST", "already requested ");
                                        alreadyRequested = true;
                                        break;
                                    }
                                }
                                if (alreadyRequested) {
                                    Toast.makeText(context, "Friend request already sent.", Toast.LENGTH_SHORT).show();
                                } else {
                                    DatabaseReference newNotifRef = notifRef.push();

                                    Map<String, Object> request = new HashMap<>();
                                    request.put("type", "friend_request");
                                    request.put("fromUid", currentUid);
                                    String fromUsername = ((PlanItOutApp) context.getApplicationContext()).getCurrentUsername();
                                    request.put("fromUsername", fromUsername);
                                    request.put("timestamp", System.currentTimeMillis());

                                    newNotifRef.setValue(request).addOnSuccessListener(aVoid -> {
                                        Log.d("FRIEND_REQUEST", "Sending friend request to: " + user.getUsername() + " (UID: " + user.getUid() + ")");
                                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });

                });

            }

            return itemView;
        }
    }
}
