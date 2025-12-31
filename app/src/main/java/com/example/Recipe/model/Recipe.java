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

    // New fields
    private int servings;
    private String difficulty;
    private String cuisine;
    private int cookingTime; // in minutes

    private Map<String, Boolean> likes;
    private int likeCount;
    private int downloadCount;

    // ==================== Constructors ====================
    public Recipe() {
        likes = new HashMap<>();
        likeCount = 0;
        downloadCount = 0;
        servings = 0;
        difficulty = "";
        cuisine = "";
        cookingTime = 0;
    }

    public Recipe(String name, String imageUrl, String category, String userName,
                  String userId, List<Ingredient> ingredients, List<Step> steps,
                  String instructions, int servings, String difficulty, String cuisine) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.category = category;
        this.userName = userName;
        this.userId = userId;
        this.ingredients = ingredients;
        this.steps = steps;
        this.instructions = instructions;
        this.servings = servings;
        this.difficulty = difficulty;
        this.cuisine = cuisine;
        this.cookingTime = 0; // default
        this.timestamp = System.currentTimeMillis();
        this.likes = new HashMap<>();
        this.likeCount = 0;
        this.downloadCount = 0;
    }

    // ==================== Getters ====================
    public String getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public String getUserName() { return userName; }
    public String getUserId() { return userId; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public List<Step> getSteps() { return steps; }
    public String getInstructions() { return instructions; }
    public long getTimestamp() { return timestamp; }

    public int getServings() { return servings; }
    public String getDifficulty() { return difficulty; }
    public String getCuisine() { return cuisine; }
    public int getCookingTime() { return cookingTime; }

    public Map<String, Boolean> getLikes() { return likes != null ? likes : new HashMap<>(); }
    public int getLikeCount() { return likeCount; }
    public int getDownloadCount() { return downloadCount; }

    // ==================== Setters ====================
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(String category) { this.category = category; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
    public void setSteps(List<Step> steps) { this.steps = steps; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public void setServings(int servings) { this.servings = servings; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public void setCookingTime(int cookingTime) { this.cookingTime = cookingTime; }

    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    // ==================== Helpers ====================
    public boolean isLikedBy(String userId) {
        return likes != null && likes.containsKey(userId) && likes.get(userId);
    }
}
