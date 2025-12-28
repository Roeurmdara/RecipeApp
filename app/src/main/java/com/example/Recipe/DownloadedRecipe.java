package com.example.Recipe;

import com.example.Recipe.model.Ingredient;
import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.Step;

import java.util.List;

// IMPORTANT: Ensure that the 'Recipe', 'Ingredient', and 'Step' classes
// are defined in the same 'com.example.Recipe' package.
// If they are in a different package (e.g., 'com.example.Recipe.models'),
// you would need to add explicit import statements here like:
// import com.example.Recipe.models.Ingredient;
// import com.example.Recipe.models.Step;
// import com.example.Recipe.models.Recipe;


public class DownloadedRecipe {
    private String id;
    private String name;
    private String imageUrl;
    private String category;
    private String ownerName; // Corresponds to 'userName' in the Recipe class
    private String ownerUid;  // Corresponds to 'userId' in the Recipe class
    private List<Ingredient> ingredients;
    private List<Step> steps;
    private String instructions;

    // 1. REQUIRED: Public empty constructor for Firebase Realtime Database and Gson deserialization
    public DownloadedRecipe() {
        // Default constructor required for calls to DataSnapshot.getValue(DownloadedRecipe.class) or Gson deserialization
    }

    // 2. Constructor with all individual fields (useful for manual creation or full data parsing)
    public DownloadedRecipe(String id, String name, String imageUrl, String category,
                            String ownerName, String ownerUid, List<Ingredient> ingredients,
                            List<Step> steps, String instructions) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.category = category;
        this.ownerName = ownerName;
        this.ownerUid = ownerUid;
        this.ingredients = ingredients;
        this.steps = steps;
        this.instructions = instructions;
    }

    // 3. REQUIRED: Constructor to create a DownloadedRecipe from a Recipe object
    // This resolves the "no suitable constructor found for DownloadedRecipe(Recipe)" error.
    public DownloadedRecipe(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.imageUrl = recipe.getImageUrl();
        this.category = recipe.getCategory();
        // FIX: Using recipe.getUserName() and recipe.getUserId()
        // This resolves the "cannot find symbol method getUsername()" / "getOwnerName()" error.
        this.ownerName = recipe.getUserName();
        this.ownerUid = recipe.getUserId();
        this.ingredients = recipe.getIngredients();
        this.steps = recipe.getSteps();
        this.instructions = recipe.getInstructions();
    }


    // 4. REQUIRED: Public getters for Firebase Realtime Database and Gson serialization/deserialization
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getOwnerName() { // Getter for the 'ownerName' field of DownloadedRecipe
        return ownerName;
    }

    public String getOwnerUid() { // Getter for the 'ownerUid' field of DownloadedRecipe
        return ownerUid;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public String getInstructions() {
        return instructions;
    }

    // 5. Optional: Public setters (useful if you need to update individual fields after object creation)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    // 6. REQUIRED: Method to convert DownloadedRecipe back to a Recipe object
    // This resolves the "cannot find symbol method toRecipe()" error.
    /**
     * Converts this DownloadedRecipe object into a Recipe object.
     * This is necessary because FavoritesFragment is expecting a List<Recipe>.
     * Assumes the Recipe class has a suitable constructor and setId() method.
     *
     * @return A new Recipe object populated with the data from this DownloadedRecipe.
     */
    public Recipe toRecipe() {
        // Create a new Recipe object using the constructor that takes all fields EXCEPT id.
        // The id is usually set separately after creation, matching how it's done in SubmitRecipeFragment.
        Recipe recipe = new Recipe(
                this.name,
                this.imageUrl,
                this.category,
                this.ownerName,  // Matches 'userName' parameter in Recipe constructor
                this.ownerUid,   // Matches 'userId' parameter in Recipe constructor
                this.ingredients,
                this.steps,
                this.instructions
        );
        // Set the ID separately
        recipe.setId(this.id);
        return recipe;
    }
}