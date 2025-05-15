package com.example.planitout.activities;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import com.bumptech.glide.Glide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.planitout.activities.FriendsPopup;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import com.example.planitout.R;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.EditText;
import android.view.View;
import com.example.planitout.PlanItOutApp;
import com.example.planitout.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.view.LayoutInflater;
import android.widget.PopupMenu;


import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.database.GenericTypeIndicator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ProfileViewActivity extends AppCompatActivity {
    private ImageButton buttonEditBio , buttonEditLocation;
    private EditText editTextBio, editTextLocation;
    private final Collection<String> prefs = Arrays.asList("social","professional","entertainment","sports","tech","seasonal","charity","other");
    private final List<String> availablePreferences = new ArrayList<>(prefs);
    private List<String> selectedPreferences = new ArrayList<>();
    private FlexboxLayout tagContainer;
    private Button buttonAddPreference, buttonEditProfilePicture;
    private ImageView removeTag, profilePictureView;
    private TextView tagText, usernameView;
    String userID;
    private static final int IMAGE_PICK_REQUEST = 1001;
    private Uri imageUri;
    DatabaseReference userRef;
    private Button friendsButton, logoutButton;
    private List<UserModel> friendsList = new ArrayList<>();
    private DatabaseReference databaseReference;
    private ActivityResultLauncher<Intent> friendProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_view);

        usernameView = findViewById(R.id.usernameView);
        String username;
        userID = getIntent().getStringExtra("userID");
        if (userID == null) {
            username = "mbaig66"; // fallback for testing
        } else {
            username = ((PlanItOutApp) getApplication()).getCurrentUsername();
        }
        usernameView.setText(username);

        // Profile picture
        profilePictureView = findViewById(R.id.profilePicture);
        buttonEditProfilePicture = findViewById(R.id.buttonEditProfilePicture);
        buttonEditProfilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_REQUEST);
        });


        // Friends Button
        databaseReference = FirebaseDatabase.getInstance().getReference();
        friendsButton = findViewById(R.id.myFriends);
        logoutButton = findViewById(R.id.logoutButton);

        if (userID != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userID);
            loadFriends();
        }
        friendsButton.setOnClickListener(v -> {
            new FriendsPopup(ProfileViewActivity.this, userRef, friendsList, friendProfileLauncher).show();
        });

        friendProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String unfriendedUid = result.getData().getStringExtra("unfriendedUid");
                        if (unfriendedUid != null) {
                            for (int i = 0; i < friendsList.size(); i++) {
                                if (friendsList.get(i).getUid().equals(unfriendedUid)) {
                                    friendsList.remove(i);
                                    break;
                                }
                            }
                        }
                    }
                }
        );

        logoutButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileViewActivity.this, R.style.CustomAlertDialog);

            View customView = getLayoutInflater().inflate(R.layout.custom_logout_dialog, null);
            builder.setView(customView);

            AlertDialog dialog = builder.create();

            customView.findViewById(R.id.btn_yes).setOnClickListener(buttonView -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(ProfileViewActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileViewActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                dialog.dismiss();
            });

            customView.findViewById(R.id.btn_no).setOnClickListener(buttonView -> {
                dialog.dismiss();
            });

            dialog.show();
        });

        // Bio
        editTextBio = findViewById(R.id.editTextBio);
        buttonEditBio = findViewById(R.id.buttonEditBio);
        editTextBio.setEnabled(false);
        editTextBio.setFocusable(false);
        buttonEditBio.setOnClickListener(v -> editBio());

        // Location
        editTextLocation = findViewById(R.id.editTextLocation);
        buttonEditLocation = findViewById(R.id.buttonEditLocation);
        editTextLocation.setEnabled(false);
        editTextLocation.setFocusable(false);
        buttonEditLocation.setOnClickListener(v -> editLocation());

        // Preferences
        tagContainer = findViewById(R.id.tagContainer);
        buttonAddPreference = findViewById(R.id.buttonAddPreference);
        buttonAddPreference.setOnClickListener(v -> showPreferencePopup());

        if (userID != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userID);

            //load bio
            userRef.child("bio").get().addOnSuccessListener(dataSnapshot -> {
                String bio = dataSnapshot.getValue(String.class);
                if (bio != null) {
                    editTextBio.setText(bio);
                } else {
                    editTextBio.setHint("Add a bio");
                }
            });

            //load location
            userRef.child("location").get().addOnSuccessListener(dataSnapshot -> {
                String location = dataSnapshot.getValue(String.class);
                if (location != null) {
                    editTextLocation.setText(location);
                } else {
                    editTextLocation.setHint("Add a location");
                }
            });

            //load preferences
            userRef.child("preferences").get().addOnSuccessListener(dataSnapshot -> {
                GenericTypeIndicator<List<String>> typeIndicator = new GenericTypeIndicator<List<String>>() {};
                List<String> preferences = dataSnapshot.getValue(typeIndicator);
                if (preferences != null) {
                    for (String pref : preferences) {
                        selectedPreferences.add(pref);
                        addTag(pref);

                    }
                }
            });

            //load profile picture
            userRef.child("profilePicture").get().addOnSuccessListener(dataSnapshot -> {
                String profilePicture = dataSnapshot.getValue(String.class);
                if (profilePicture != null) {
                    loadImage(profilePicture, profilePictureView);
                } else {
                    profilePictureView.setImageResource(R.drawable.baseline_account_circle_24);
                }
            });


        }
    }


    public void editBio() {
        boolean isEditing = editTextBio.isEnabled();
        if (!isEditing) {
            editTextBio.setEnabled(true);
            editTextBio.setFocusableInTouchMode(true);
            editTextBio.requestFocus();
            buttonEditBio.setImageResource(R.drawable.ic_save);
        } else {
            editTextBio.setEnabled(false);
            editTextBio.setFocusable(false);
            buttonEditBio.setImageResource(R.drawable.ic_edit);
            userRef.child("bio").setValue(editTextBio.getText().toString());
        }
    }

    public void editLocation() {
        boolean isEditing = editTextLocation.isEnabled();

        if (!isEditing) {
            // Switch to Edit Mode
            editTextLocation.setEnabled(true);
            editTextLocation.setFocusableInTouchMode(true);
            editTextLocation.requestFocus();
            buttonEditLocation.setImageResource(R.drawable.ic_save);
        } else {
            // Save Changes and Switch to View Mode
            editTextLocation.setEnabled(false);
            editTextLocation.setFocusable(false);
            buttonEditLocation.setImageResource(R.drawable.ic_edit);

            String updatedLocation = editTextLocation.getText().toString();
            //upload to firebase db
            userRef.child("location").setValue(updatedLocation);
        }
    }


    public void showPreferencePopup(){
        PopupMenu popup = new PopupMenu(this, buttonAddPreference);
        availablePreferences.forEach(pref -> popup.getMenu().add(pref));

        popup.setOnMenuItemClickListener(item -> {
            if (!selectedPreferences.contains(item.getTitle())) {
                selectedPreferences.add(item.getTitle().toString());
                addTag(item.getTitle().toString());
                userRef.child("preferences").setValue(selectedPreferences);
            }
            return true;
        });
        popup.show();
    }


    public void loadFriends() {
        // Load friends from Firebase
        userRef.child("friends").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                GenericTypeIndicator<Map<String, Boolean>> typeIndicator = new GenericTypeIndicator<Map<String, Boolean>>() {};
                Map<String, Boolean> friendMap = dataSnapshot.getValue(typeIndicator);

                if (friendMap != null) {
                    for (String friendId : friendMap.keySet()) {
                        DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("users").child(friendId);
                        friendRef.get().addOnSuccessListener(friendSnapshot -> {
                            UserModel friend = friendSnapshot.getValue(UserModel.class);
                            if (friend != null) {
                                friend.setUid(friendId);
                                friendsList.add(friend);
                            }
                        });
                    }
                }
            }
        });

    }

    private void addTag(String tag) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View tagView = inflater.inflate(R.layout.preference_tag, tagContainer, false);
        tagText = tagView.findViewById(R.id.tagText);
        tagText.setText(tag);
        removeTag = tagView.findViewById(R.id.removeTag);

        removeTag.setOnClickListener(v -> {
            tagContainer.removeView(tagView);
            selectedPreferences.remove(tag);
            //upload to firebase
            userRef.child("preferences").setValue(selectedPreferences);
        });
        tagContainer.addView(tagView);
    }

    private void loadImage(String imageUrl, ImageView target) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.baseline_account_circle_24)
                .circleCrop()
                .into(target);
    }

    private void updateProfilePicture() {
        if (imageUri != null) {
            String fileName = "profile_pictures/" + userID + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            userRef.child("profilePicture").setValue(downloadUrl);
                            loadImage(downloadUrl, profilePictureView);
                            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .into(profilePictureView);

            // save new picture to database
            updateProfilePicture();
        }
    }


}