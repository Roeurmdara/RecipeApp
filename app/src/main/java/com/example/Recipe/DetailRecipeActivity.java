package com.example.Recipe;

import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Recipe.model.Ingredient;
import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.Step;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

public class DetailRecipeActivity extends AppCompatActivity {

    private ImageView ivRecipeImage;
    private TextView tvRecipeName, tvCategory, tvServings, tvDifficulty, tvCuisine, tvInstructions, tvCookingTime;
    private LinearLayout ingredientsContainer, stepsContainer;
    private ProgressBar progressBar;

    private DatabaseReference databaseReference;
    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_recipe);

        initViews();
        initFirebase();

        String recipeJson = getIntent().getStringExtra("RECIPE_OBJECT");
        recipeId = getIntent().getStringExtra("recipeId");

        if (recipeJson != null) {
            Recipe recipe = new Gson().fromJson(recipeJson, Recipe.class);
            displayRecipe(recipe);
        } else if (recipeId != null) {
            loadRecipeFromFirebase();
        } else {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        ivRecipeImage = findViewById(R.id.ivRecipeImage);
        tvRecipeName = findViewById(R.id.tvRecipeName);
        tvCategory = findViewById(R.id.tvCategory);
        tvServings = findViewById(R.id.tvServings);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvCuisine = findViewById(R.id.tvCuisine);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvCookingTime = findViewById(R.id.tvCookingTime); // NEW
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        stepsContainer = findViewById(R.id.stepsContainer);
        progressBar = findViewById(R.id.progressBar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("recipes");
    }

    private void loadRecipeFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        databaseReference.child(recipeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Recipe recipe = snapshot.getValue(Recipe.class);
                        if (recipe != null) {
                            displayRecipe(recipe);
                        } else {
                            Toast.makeText(DetailRecipeActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailRecipeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayRecipe(Recipe recipe) {

        // ===== Recipe Name & Category =====
        tvRecipeName.setText(recipe.getName() != null ? recipe.getName() : "Unnamed Recipe");
        tvCategory.setText(recipe.getCategory() != null ? recipe.getCategory() : "Unknown");

        // ===== Servings =====
        if (recipe.getServings() > 0) {
            tvServings.setText(String.valueOf(recipe.getServings()));
        } else {
            tvServings.setText("N/A");
        }

        // ===== Difficulty =====
        if (recipe.getDifficulty() != null && !recipe.getDifficulty().isEmpty()) {
            tvDifficulty.setText(recipe.getDifficulty());
        } else {
            tvDifficulty.setText("N/A");
        }

        // ===== Cooking Time =====
        if (recipe.getCookingTime() > 0) {
            tvCookingTime.setText(recipe.getCookingTime() + " min");
            tvCookingTime.setVisibility(View.VISIBLE);
        } else {
            tvCookingTime.setVisibility(View.GONE);
        }

        // ===== Cuisine =====
        if (recipe.getCuisine() != null && !recipe.getCuisine().isEmpty()) {
            tvCuisine.setText(recipe.getCuisine() + " Cuisine");
            tvCuisine.setVisibility(View.VISIBLE);
        } else {
            tvCuisine.setVisibility(View.GONE);
        }

        // ===== Instructions =====
        tvInstructions.setText(
                recipe.getInstructions() != null
                        ? recipe.getInstructions()
                        : "No instructions provided"
        );



        // ===== Load Image (Base64) =====
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(recipe.getImageUrl(), Base64.DEFAULT);
                ivRecipeImage.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            } catch (Exception e) {
                ivRecipeImage.setBackgroundColor(0xFFE0E0E0);
            }
        }

        // ===== Ingredients =====
        ingredientsContainer.removeAllViews();
        if (recipe.getIngredients() != null) {
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                addIngredientView(recipe.getIngredients().get(i), i + 1);
            }
        }

        // ===== Steps =====
        stepsContainer.removeAllViews();
        if (recipe.getSteps() != null) {
            for (int i = 0; i < recipe.getSteps().size(); i++) {
                addStepView(recipe.getSteps().get(i), i + 1);
            }
        }
    }

    private void addIngredientView(Ingredient ingredient, int number) {

        // Parent layout (horizontal)
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        // Left text: number + name
        TextView tvName = new TextView(this);
        tvName.setText(number + ". " + ingredient.getName() + " ");
        tvName.setTextSize(16);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        // Right text: quantity
        TextView tvQty = new TextView(this);
        tvQty.setText(ingredient.getQuantity());
        tvQty.setTextSize(16);
        tvQty.setTypeface(null, android.graphics.Typeface.BOLD); // BOLD
        tvQty.setTextColor(0xFF424242); // Pure black
        tvQty.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        tvQty.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Add views
        row.addView(tvName);
        row.addView(tvQty);

        ingredientsContainer.addView(row);

        // Divider line
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        divider.setBackgroundColor(0xFFDDDDDD);

        ingredientsContainer.addView(divider);
    }


    private void addStepView(Step step, int number) {

        // Parent container (vertical)
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 12, 0, 24);

        // Step title row (number + title)
        TextView tvTitle = new TextView(this);
        tvTitle.setText(number + ". " + step.getDescription());
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null);


        container.addView(tvTitle);

        // Divider (optional)
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        divider.setBackgroundColor(0xFFE0E0E0);
        divider.setPadding(0, 12, 0, 0);

        container.addView(divider);

        stepsContainer.addView(container);
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
