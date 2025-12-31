package com.example.Recipe;

import com.example.Recipe.model.Ingredient;
import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.Step;

import java.util.List;

public class DownloadedRecipe {
    private String id;
    private String name;
    private String imageUrl;
    private String category;
    private String ownerName; // corresponds to userName in Recipe
    private String ownerUid;  // corresponds to userId in Recipe
    private List<Ingredient> ingredients;
    private List<Step> steps;
    private String instructions;

    // New fields
    private int servings;
    private String difficulty;
    private String cuisine;
    private int cookingTime; // in minutes

    // ==================== Constructors ====================
    public DownloadedRecipe() {
        this.servings = 0;
        this.difficulty = "";
        this.cuisine = "";
        this.cookingTime = 0;
    }

    public DownloadedRecipe(String id, String name, String imageUrl, String category,
                            String ownerName, String ownerUid, List<Ingredient> ingredients,
                            List<Step> steps, String instructions, int servings,
                            String difficulty, String cuisine, int cookingTime) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.category = category;
        this.ownerName = ownerName;
        this.ownerUid = ownerUid;
        this.ingredients = ingredients;
        this.steps = steps;
        this.instructions = instructions;
        this.servings = servings;
        this.difficulty = difficulty;
        this.cuisine = cuisine;
        this.cookingTime = cookingTime;
    }

    public DownloadedRecipe(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.imageUrl = recipe.getImageUrl();
        this.category = recipe.getCategory();
        this.ownerName = recipe.getUserName();
        this.ownerUid = recipe.getUserId();
        this.ingredients = recipe.getIngredients();
        this.steps = recipe.getSteps();
        this.instructions = recipe.getInstructions();
        this.servings = recipe.getServings();
        this.difficulty = recipe.getDifficulty();
        this.cuisine = recipe.getCuisine();
        this.cookingTime = recipe.getCookingTime();
    }

    // ==================== Getters ====================
    public String getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerUid() { return ownerUid; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public List<Step> getSteps() { return steps; }
    public String getInstructions() { return instructions; }
    public int getServings() { return servings; }
    public String getDifficulty() { return difficulty; }
    public String getCuisine() { return cuisine; }
    public int getCookingTime() { return cookingTime; }

    // ==================== Setters ====================
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(String category) { this.category = category; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
    public void setSteps(List<Step> steps) { this.steps = steps; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setServings(int servings) { this.servings = servings; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public void setCookingTime(int cookingTime) { this.cookingTime = cookingTime; }

    // ==================== Conversion ====================
    /**
     * Converts this DownloadedRecipe object into a Recipe object.
     * Useful for displaying favorites or any section requiring Recipe objects.
     */
    public Recipe toRecipe() {
        Recipe recipe = new Recipe(
                this.name,
                this.imageUrl,
                this.category,
                this.ownerName,
                this.ownerUid,
                this.ingredients,
                this.steps,
                this.instructions,
                this.servings,
                this.difficulty,
                this.cuisine
        );
        recipe.setId(this.id);
        recipe.setCookingTime(this.cookingTime);
        return recipe;
    }
}
