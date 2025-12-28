package com.example.Recipe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.Recipe.model.Ingredient;
import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.Step;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity {

    private ImageView ivRecipeImage;
    private EditText etRecipeName, etInstructions;
    private LinearLayout ingredientsContainer, stepsContainer;
    private Button btnUploadImage, btnAddIngredient, btnAddStep, btnSubmit;
    private ProgressBar progressBar;
    private Spinner spinnerCategory;

    private Uri imageUri;
    private String base64Image;
    private String recipeId;
    private Recipe originalRecipe;

    private final List<IngredientView> ingredientViews = new ArrayList<>();
    private final List<StepView> stepViews = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_submit_recipe);

        recipeId = getIntent().getStringExtra("RECIPE_ID");
        if (recipeId == null) {
            Toast.makeText(this, "Recipe ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initFirebase();
        setupCategorySpinner();
        setupImagePicker();
        setupPermissionLauncher();
        setupListeners();

        // Load existing recipe data
        loadRecipeData();
    }

    private void initViews() {
        ivRecipeImage = findViewById(R.id.ivRecipeImage);
        etRecipeName = findViewById(R.id.etRecipeName);
        etInstructions = findViewById(R.id.etInstructions);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        stepsContainer = findViewById(R.id.stepsContainer);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnAddStep = findViewById(R.id.btnAddStep);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        // Change button text to "Update Recipe"
        btnSubmit.setText("Update Recipe");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Recipe");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        databaseReference = database.getReference("recipes");
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "Select Category", "Breakfast", "Lunch", "Dinner",
                "Dessert", "Snack", "Beverage", "Soup", "Salad", "Vegetarian"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadRecipeData() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        databaseReference.child(recipeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalRecipe = snapshot.getValue(Recipe.class);

                if (originalRecipe != null) {
                    // Pre-fill all fields with existing data
                    etRecipeName.setText(originalRecipe.getName());
                    etInstructions.setText(originalRecipe.getInstructions());

                    // Set category
                    String recipeCategory = originalRecipe.getCategory();
                    if (recipeCategory != null) {
                        for (int i = 0; i < spinnerCategory.getCount(); i++) {
                            if (spinnerCategory.getItemAtPosition(i).toString().equals(recipeCategory)) {
                                spinnerCategory.setSelection(i);
                                break;
                            }
                        }
                    }

                    // Set image
                    base64Image = originalRecipe.getImageUrl();
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivRecipeImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Load ingredients
                    if (originalRecipe.getIngredients() != null) {
                        for (Ingredient ingredient : originalRecipe.getIngredients()) {
                            addIngredientField(ingredient.getName(), ingredient.getQuantity());
                        }
                    } else {
                        addIngredientField("", "");
                    }

                    // Load steps
                    if (originalRecipe.getSteps() != null) {
                        for (Step step : originalRecipe.getSteps()) {
                            addStepField(step.getDescription());
                        }
                    } else {
                        addStepField("");
                    }
                } else {
                    Toast.makeText(EditRecipeActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                    finish();
                }

                progressBar.setVisibility(android.view.View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(EditRecipeActivity.this, "Failed to load: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openImagePicker();
                    else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        ivRecipeImage.setImageURI(uri);
                        convertImageToBase64(uri);
                    }
                }
        );
    }

    private void convertImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int maxWidth = 800;
            int maxHeight = 800;

            if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                float scale = Math.min(
                        (float) maxWidth / bitmap.getWidth(),
                        (float) maxHeight / bitmap.getHeight()
                );

                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Toast.makeText(this, "Image updated", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void checkPermissionAndOpenPicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void setupListeners() {
        btnUploadImage.setOnClickListener(v -> checkPermissionAndOpenPicker());
        btnAddIngredient.setOnClickListener(v -> addIngredientField("", ""));
        btnAddStep.setOnClickListener(v -> addStepField(""));
        btnSubmit.setOnClickListener(v -> updateRecipe());
    }

    private void addIngredientField(String ingredientName, String quantity) {
        android.view.View view = getLayoutInflater().inflate(R.layout.ingredient_item, ingredientsContainer, false);

        EditText name = view.findViewById(R.id.etIngredientName);
        EditText qty = view.findViewById(R.id.etQuantity);
        Button remove = view.findViewById(R.id.btnRemoveIngredient);

        // Pre-fill data
        name.setText(ingredientName);
        qty.setText(quantity);

        remove.setOnClickListener(v -> {
            ingredientsContainer.removeView(view);
            ingredientViews.removeIf(i -> i.view == view);
        });

        ingredientViews.add(new IngredientView(view, name, qty));
        ingredientsContainer.addView(view);
    }

    static class IngredientView {
        android.view.View view;
        EditText name, qty;
        IngredientView(android.view.View v, EditText n, EditText q) { view = v; name = n; qty = q; }
    }

    private void addStepField(String stepDescription) {
        android.view.View view = getLayoutInflater().inflate(R.layout.step_item, stepsContainer, false);

        EditText step = view.findViewById(R.id.etStepDescription);
        Button remove = view.findViewById(R.id.btnRemoveStep);

        // Pre-fill data
        step.setText(stepDescription);

        remove.setOnClickListener(v -> {
            stepsContainer.removeView(view);
            stepViews.removeIf(s -> s.view == view);
        });

        stepViews.add(new StepView(view, step));
        stepsContainer.addView(view);
    }

    static class StepView {
        android.view.View view;
        EditText step;
        StepView(android.view.View v, EditText s) { view = v; step = s; }
    }

    private void updateRecipe() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current values (use old values if not changed)
        String name = etRecipeName.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // Use original data if user didn't change it
        if (name.isEmpty()) name = originalRecipe.getName();
        if (instructions.isEmpty()) instructions = originalRecipe.getInstructions();
        if (category.equals("Select Category")) category = originalRecipe.getCategory();
        if (base64Image == null) base64Image = originalRecipe.getImageUrl();

        // Collect ingredients (keep old if none entered)
        List<Ingredient> ingredients = new ArrayList<>();
        for (IngredientView i : ingredientViews) {
            String ingName = i.name.getText().toString().trim();
            String ingQty = i.qty.getText().toString().trim();
            if (!ingName.isEmpty()) {
                ingredients.add(new Ingredient(ingName, ingQty));
            }
        }
        if (ingredients.isEmpty() && originalRecipe.getIngredients() != null) {
            ingredients = originalRecipe.getIngredients();
        }

        // Collect steps (keep old if none entered)
        List<Step> steps = new ArrayList<>();
        for (StepView s : stepViews) {
            String stepDesc = s.step.getText().toString().trim();
            if (!stepDesc.isEmpty()) {
                steps.add(new Step(stepDesc));
            }
        }
        if (steps.isEmpty() && originalRecipe.getSteps() != null) {
            steps = originalRecipe.getSteps();
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnSubmit.setEnabled(false);

        saveUpdatedRecipe(name, category, user, ingredients, steps, instructions);
    }

    private void saveUpdatedRecipe(String name, String category, FirebaseUser user,
                                   List<Ingredient> ingredients, List<Step> steps,
                                   String instructions) {

        String username = user.getDisplayName() != null
                ? user.getDisplayName()
                : user.getEmail().split("@")[0];

        Recipe recipe = new Recipe(
                name, base64Image, category, username, user.getUid(),
                ingredients, steps, instructions
        );

        recipe.setId(recipeId);
        // Keep original timestamp
        if (originalRecipe != null) {
            recipe.setTimestamp(originalRecipe.getTimestamp());
        }

        databaseReference.child(recipeId).setValue(recipe)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Recipe updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}