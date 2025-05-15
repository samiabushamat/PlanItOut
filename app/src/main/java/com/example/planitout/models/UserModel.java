package com.example.planitout.models;

public class UserModel {
    String name, username,  email, password,uid, profilePicture;
    public UserModel() {}
    public UserModel(String name, String username, String email, String password,String uid ,String profilePicture) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
        this.uid = uid;
        this.profilePicture = profilePicture;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public String getProfilePicture() { return profilePicture;}
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture;}
    public void setEmail(String email) {
        this.email = email;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
}
