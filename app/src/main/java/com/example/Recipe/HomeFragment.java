package com.example.Recipe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Recipe.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewKhmerFood;
    private RecyclerView recyclerViewAllRecipes;
    private RecipeAdapter khmerFoodAdapter;
    private RecipeAdapter allRecipesAdapter;
    private List<Recipe> recipeList;
    private List<Recipe> filteredAllRecipesList;
    private List<Recipe> khmerFoodList;
    private ProgressBar progressBar;
    private Spinner spinnerFilter;
    private LinearLayout khmerFoodSection;
    private LinearLayout allRecipesSection;

    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            initViews(view);
            initFirebase();
            setupKhmerFoodRecyclerView();
            setupAllRecipesRecyclerView();
            setupFilter();
            loadRecipes();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error initializing: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void initViews(View view) {
        recyclerViewKhmerFood = view.findViewById(R.id.recyclerViewKhmerFood);
        recyclerViewAllRecipes = view.findViewById(R.id.recyclerViewAllRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);
        khmerFoodSection = view.findViewById(R.id.khmerFoodSection);
        allRecipesSection = view.findViewById(R.id.allRecipesSection);

        if (recyclerViewKhmerFood == null || recyclerViewAllRecipes == null) {
            throw new RuntimeException("RecyclerViews not found in layout!");
        }
    }

    private void initFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
            );
            databaseReference = database.getReference("recipes");
        } catch (Exception e) {
            Toast.makeText(getContext(), "Firebase initialization failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupKhmerFoodRecyclerView() {
        khmerFoodList = new ArrayList<>();
        khmerFoodAdapter = new RecipeAdapter(getContext(), khmerFoodList, FirebaseAuth.getInstance());

        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );
        recyclerViewKhmerFood.setLayoutManager(horizontalLayoutManager);
        recyclerViewKhmerFood.setAdapter(khmerFoodAdapter);
    }

    private void setupAllRecipesRecyclerView() {
        filteredAllRecipesList = new ArrayList<>();
        allRecipesAdapter = new RecipeAdapter(getContext(), filteredAllRecipesList, FirebaseAuth.getInstance());

        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );
        recyclerViewAllRecipes.setLayoutManager(horizontalLayoutManager);
        recyclerViewAllRecipes.setAdapter(allRecipesAdapter);
    }

    private void setupFilter() {
        String[] filters = {
                "All Recipes",
                "Breakfast",
                "Lunch",
                "Dinner",
                "Dessert",
                "Appetizer",
                "Snack",
                "Beverage",
                "Salad",
                "Soup",
                "Vegetarian",
                "Vegan",
                "Seafood",
                "Meat",
                "Pasta",
                "Baking"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                filters
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                filterRecipes(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadRecipes() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    recipeList = new ArrayList<>();
                    khmerFoodList.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Recipe recipe = dataSnapshot.getValue(Recipe.class);
                        if (recipe != null) {
                            recipeList.add(recipe);

                            // Separate Khmer cuisine recipes
                            if (recipe.getCuisine() != null &&
                                    recipe.getCuisine().equalsIgnoreCase("Khmer")) {
                                khmerFoodList.add(recipe);
                            }
                        }
                    }

                    // Sort by timestamp (newest first)
                    if (recipeList.size() > 0) {
                        recipeList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    }
                    if (khmerFoodList.size() > 0) {
                        khmerFoodList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    }

                    // Update Khmer Food section visibility
                    if (!khmerFoodList.isEmpty()) {
                        khmerFoodSection.setVisibility(View.VISIBLE);
                        khmerFoodAdapter.updateRecipes(khmerFoodList);
                    } else {
                        khmerFoodSection.setVisibility(View.GONE);
                    }

                    // Apply current filter to All Recipes section
                    String currentFilter = spinnerFilter.getSelectedItem().toString();
                    filterRecipes(currentFilter);

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error loading recipes: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Failed to load recipes: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRecipes(String category) {
        try {
            filteredAllRecipesList.clear();

            if (recipeList == null) {
                recipeList = new ArrayList<>();
            }

            if (category.equals("All Recipes")) {
                // Show all recipes
                filteredAllRecipesList.addAll(recipeList);
            } else {
                // Filter by selected category
                for (Recipe recipe : recipeList) {
                    if (recipe.getCategory() != null && recipe.getCategory().equals(category)) {
                        filteredAllRecipesList.add(recipe);
                    }
                }
            }

            // Update All Recipes section visibility
            if (!filteredAllRecipesList.isEmpty()) {
                allRecipesSection.setVisibility(View.VISIBLE);
                allRecipesAdapter.updateRecipes(filteredAllRecipesList);
            } else {
                allRecipesSection.setVisibility(View.GONE);
                if (!category.equals("All Recipes")) {
                    Toast.makeText(getContext(), "No recipes found in this category",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error filtering: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}