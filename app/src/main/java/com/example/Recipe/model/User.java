package com.example.Recipe.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    private String userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private String bio;
    private long joinedDate;

    // Followers and Following
    private Map<String, Boolean> followers;
    private Map<String, Boolean> following;
    private int followerCount;
    private int followingCount;
    private int recipeCount;

    // Required by Firebase
    public User() {
        followers = new HashMap<>();
        following = new HashMap<>();
        followerCount = 0;
        followingCount = 0;
        recipeCount = 0;
        joinedDate = System.currentTimeMillis();
    }

    // Constructor
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.followers = new HashMap<>();
        this.following = new HashMap<>();
        this.followerCount = 0;
        this.followingCount = 0;
        this.recipeCount = 0;
        this.joinedDate = System.currentTimeMillis();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getBio() { return bio; }
    public long getJoinedDate() { return joinedDate; }

    public Map<String, Boolean> getFollowers() {
        return followers != null ? followers : new HashMap<>();
    }
    public Map<String, Boolean> getFollowing() {
        return following != null ? following : new HashMap<>();
    }

    public int getFollowerCount() { return followerCount; }
    public int getFollowingCount() { return followingCount; }
    public int getRecipeCount() { return recipeCount; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setJoinedDate(long joinedDate) { this.joinedDate = joinedDate; }

    public void setFollowers(Map<String, Boolean> followers) { this.followers = followers; }
    public void setFollowing(Map<String, Boolean> following) { this.following = following; }

    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }

    // Helper methods
    public boolean isFollowedBy(String uid) {
        return followers != null && followers.containsKey(uid) && followers.get(uid);
    }

    public boolean isFollowing(String uid) {
        return following != null && following.containsKey(uid) && following.get(uid);
    }
}