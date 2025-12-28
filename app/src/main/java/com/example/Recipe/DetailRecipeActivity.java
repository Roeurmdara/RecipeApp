package com.example.Recipe;

import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Paint;
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
    private TextView tvRecipeName, tvCategory, tvUserName, tvInstructions;
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
        tvUserName = findViewById(R.id.tvUserName);
        tvInstructions = findViewById(R.id.tvInstructions);
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
        progressBar.setVisibility(android.view.View.VISIBLE);

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
                        progressBar.setVisibility(android.view.View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(DetailRecipeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayRecipe(Recipe recipe) {
        // ===== Recipe Name & Category =====
        tvRecipeName.setText(recipe.getName());
        tvCategory.setText(recipe.getCategory());



        // ===== Instructions =====
        tvInstructions.setText(recipe.getInstructions());

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
        TextView tv = new TextView(this);
        tv.setText("• " + ingredient.getName() + " - " + ingredient.getQuantity());
        tv.setTextSize(16);
        tv.setPadding(0, 8, 0, 8);
        ingredientsContainer.addView(tv);
    }

    private void addStepView(Step step, int number) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, 8, 0, 16);

        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(number));
        tvNum.setPadding(12, 12, 12, 12);
        tvNum.setTextColor(0xFFFFFFFF);
        tvNum.setBackgroundResource(android.R.drawable.btn_default);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(step.getDescription());
        tvDesc.setTextSize(16);
        tvDesc.setPadding(16, 0, 0, 0);

        layout.addView(tvNum);
        layout.addView(tvDesc);
        stepsContainer.addView(layout);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
