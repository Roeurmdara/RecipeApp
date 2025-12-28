package com.example.Recipe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private List<Recipe> filteredRecipeList;
    private ProgressBar progressBar;
    private Spinner spinnerFilter;

    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initFirebase();
        setupRecyclerView();
        setupFilter();
        loadRecipes();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);
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

    private void setupRecyclerView() {
        recipeList = new ArrayList<>();
        filteredRecipeList = new ArrayList<>();

        // Correct constructor: Context, List<Recipe>, FirebaseAuth
        recipeAdapter = new RecipeAdapter(getContext(), filteredRecipeList, FirebaseAuth.getInstance());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recipeAdapter);
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
        progressBar.setVisibility(View.VISIBLE);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Recipe recipe = dataSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        recipeList.add(recipe);
                    }
                }

                // Sort by timestamp (newest first)
                recipeList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                // Apply current filter
                String currentFilter = spinnerFilter.getSelectedItem().toString();
                filterRecipes(currentFilter);

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load recipes: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRecipes(String category) {
        filteredRecipeList.clear();

        if (category.equals("All Recipes")) {
            filteredRecipeList.addAll(recipeList);
        } else {
            for (Recipe recipe : recipeList) {
                if (recipe.getCategory() != null && recipe.getCategory().equals(category)) {
                    filteredRecipeList.add(recipe);
                }
            }
        }

        // Update adapter with filtered list
        recipeAdapter.updateRecipes(filteredRecipeList);

        // Show message if no recipes found
        if (filteredRecipeList.isEmpty()) {
            Toast.makeText(getContext(), "No recipes found in this category",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
