package com.example.Recipe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.Recipe.model.Ingredient;
import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.Step;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SubmitRecipeFragment extends Fragment {

    private ImageView ivRecipeImage;
    private EditText etRecipeName, etInstructions, etServings, etCookingTime;
    private LinearLayout ingredientsContainer, stepsContainer;
    private Button btnUploadImage, btnAddIngredient, btnAddStep, btnSubmit;
    private ProgressBar progressBar;
    private Spinner spinnerCategory, spinnerDifficulty, spinnerCuisine;

    private Uri imageUri;
    private String base64Image; // Store image as Base64 string

    private final List<IngredientView> ingredientViews = new ArrayList<>();
    private final List<StepView> stepViews = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_submit_recipe, container, false);

        initViews(view);
        initFirebase();
        setupCategorySpinner();
        setupDifficultySpinner();
        setupCuisineSpinner();
        setupImagePicker();
        setupPermissionLauncher();
        setupListeners();

        addIngredientField();
        addStepField();

        return view;
    }

    private void initViews(View view) {
        ivRecipeImage = view.findViewById(R.id.ivRecipeImage);
        etRecipeName = view.findViewById(R.id.etRecipeName);
        etInstructions = view.findViewById(R.id.etInstructions);
        etServings = view.findViewById(R.id.etServings);
        etCookingTime = view.findViewById(R.id.etCookingTime); // New field

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        spinnerCuisine = view.findViewById(R.id.spinnerCuisine);

        ingredientsContainer = view.findViewById(R.id.ingredientsContainer);
        stepsContainer = view.findViewById(R.id.stepsContainer);

        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnAddIngredient = view.findViewById(R.id.btnAddIngredient);
        btnAddStep = view.findViewById(R.id.btnAddStep);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        progressBar = view.findViewById(R.id.progressBar);
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
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupDifficultySpinner() {
        String[] difficulties = {"Select Difficulty", "Easy", "Medium", "Hard"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                difficulties
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

    private void setupCuisineSpinner() {
        String[] cuisines = {
                "Select Cuisine (Optional)",
                "Italian","khmer", "Chinese", "Mexican" , "Indian", "Japanese",
                "French", "American", "Mediterranean", "Korean", "Vietnamese",
                "Spanish", "Greek", "Middle Eastern", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cuisines
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCuisine.setAdapter(adapter);
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openImagePicker();
                    else Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
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
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int maxWidth = 800;
            int maxHeight = 800;
            if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                float scale = Math.min((float) maxWidth / bitmap.getWidth(),
                        (float) maxHeight / bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap,
                        Math.round(bitmap.getWidth() * scale),
                        Math.round(bitmap.getHeight() * scale), true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Toast.makeText(getContext(), "Image ready to upload", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void checkPermissionAndOpenPicker() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void setupListeners() {
        btnUploadImage.setOnClickListener(v -> checkPermissionAndOpenPicker());
        btnAddIngredient.setOnClickListener(v -> addIngredientField());
        btnAddStep.setOnClickListener(v -> addStepField());
        btnSubmit.setOnClickListener(v -> submitRecipe());
    }

    // ================== INGREDIENTS ==================
    private void addIngredientField() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.ingredient_item, ingredientsContainer, false);

        EditText name = view.findViewById(R.id.etIngredientName);
        EditText qty = view.findViewById(R.id.etQuantity);
        Button remove = view.findViewById(R.id.btnRemoveIngredient);

        remove.setOnClickListener(v -> {
            ingredientsContainer.removeView(view);
            ingredientViews.removeIf(i -> i.view == view);
        });

        ingredientViews.add(new IngredientView(view, name, qty));
        ingredientsContainer.addView(view);
    }

    static class IngredientView {
        View view;
        EditText name, qty;
        IngredientView(View v, EditText n, EditText q) { view = v; name = n; qty = q; }
    }

    // ================== STEPS ==================
    private void addStepField() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.step_item, stepsContainer, false);

        EditText step = view.findViewById(R.id.etStepDescription);
        Button remove = view.findViewById(R.id.btnRemoveStep);

        remove.setOnClickListener(v -> {
            stepsContainer.removeView(view);
            stepViews.removeIf(s -> s.view == view);
        });

        stepViews.add(new StepView(view, step));
        stepsContainer.addView(view);
    }

    static class StepView {
        View view;
        EditText step;
        StepView(View v, EditText s) { view = v; step = s; }
    }

    // ================== SUBMIT ==================
    private void submitRecipe() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etRecipeName.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String servingsStr = etServings.getText().toString().trim();
        String difficulty = spinnerDifficulty.getSelectedItem().toString();
        String cuisine = spinnerCuisine.getSelectedItem().toString();
        String cookingTimeStr = etCookingTime.getText().toString().trim(); // New field

        if (name.isEmpty() || instructions.isEmpty() || base64Image == null
                || category.equals("Select Category")) {
            Toast.makeText(getContext(), "Fill all required fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (difficulty.equals("Select Difficulty")) {
            Toast.makeText(getContext(), "Please select a difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        int servings = 0;
        try {
            servings = Integer.parseInt(servingsStr);
            if (servings <= 0 || servings > 99) {
                Toast.makeText(getContext(), "Servings must be between 1 and 99", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid servings number", Toast.LENGTH_SHORT).show();
            return;
        }

        int cookingTime = 0;
        try {
            cookingTime = Integer.parseInt(cookingTimeStr);
            if (cookingTime < 0 || cookingTime > 1440) { // max 24 hours
                Toast.makeText(getContext(), "Enter a valid cooking time in minutes", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid cooking time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cuisine.equals("Select Cuisine (Optional)")) cuisine = "";

        List<Ingredient> ingredients = new ArrayList<>();
        for (IngredientView i : ingredientViews) {
            String ingName = i.name.getText().toString().trim();
            String ingQty = i.qty.getText().toString().trim();
            if (!ingName.isEmpty()) ingredients.add(new Ingredient(ingName, ingQty));
        }
        if (ingredients.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Step> steps = new ArrayList<>();
        for (StepView s : stepViews) {
            String stepDesc = s.step.getText().toString().trim();
            if (!stepDesc.isEmpty()) steps.add(new Step(stepDesc));
        }
        if (steps.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one step", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        saveRecipe(name, category, user, ingredients, steps, instructions,
                servings, difficulty, cuisine, cookingTime);
    }

    private void saveRecipe(String name, String category, FirebaseUser user,
                            List<Ingredient> ingredients, List<Step> steps,
                            String instructions, int servings, String difficulty,
                            String cuisine, int cookingTime) {

        String username = user.getDisplayName() != null
                ? user.getDisplayName()
                : user.getEmail().split("@")[0];

        Recipe recipe = new Recipe(
                name, base64Image, category, username, user.getUid(),
                ingredients, steps, instructions, servings, difficulty, cuisine
        );
        recipe.setCookingTime(cookingTime);

        String id = databaseReference.push().getKey();
        recipe.setId(id);

        databaseReference.child(id).setValue(recipe)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(getContext(), "Recipe submitted successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearForm() {
        etRecipeName.setText("");
        etInstructions.setText("");
        etServings.setText("");
        etCookingTime.setText("");
        spinnerCategory.setSelection(0);
        spinnerDifficulty.setSelection(0);
        spinnerCuisine.setSelection(0);
        ivRecipeImage.setImageResource(0);
        imageUri = null;
        base64Image = null;

        ingredientsContainer.removeAllViews();
        stepsContainer.removeAllViews();
        ingredientViews.clear();
        stepViews.clear();

        addIngredientField();
        addStepField();
    }
}
