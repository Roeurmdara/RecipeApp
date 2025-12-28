package com.example.Recipe.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private String imageUrl;
    private String category;
    private String userName;
    private String userId;
    private List<Ingredient> ingredients;
    private List<Step> steps;
    private String instructions;
    private long timestamp;

    private Map<String, Boolean> likes;
    private int likeCount;
    private int downloadCount;

    public Recipe() {
        likes = new HashMap<>();
        likeCount = 0;
        downloadCount = 0;
    }

    public Recipe(String name, String imageUrl, String category, String userName,
                  String userId, List<Ingredient> ingredients, List<Step> steps,
                  String instructions) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.category = category;
        this.userName = userName;
        this.userId = userId;
        this.ingredients = ingredients;
        this.steps = steps;
        this.instructions = instructions;
        this.timestamp = System.currentTimeMillis();
        this.likes = new HashMap<>();
        this.likeCount = 0;
        this.downloadCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }

    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Boolean> getLikes() { return likes != null ? likes : new HashMap<>(); }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public boolean isLikedBy(String userId) {
        return likes != null && likes.containsKey(userId) && likes.get(userId);
    }
}
