package com.example.Recipe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Recipe.model.Recipe;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private ProgressBar progressBar;
    private TextView tvNoRecipes;

    private final List<Recipe> likedRecipes = new ArrayList<>();
    private final List<Recipe> downloadedRecipes = new ArrayList<>();

    private FirebaseAuth firebaseAuth;
    private DatabaseReference recipesRef;
    private Gson gson;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        initViews(view);
        initFirebase();
        setupRecyclerView();
        setupTabs();

        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoRecipes = view.findViewById(R.id.tvNoRecipes);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        gson = new Gson();

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        recipesRef = database.getReference("recipes");
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(requireContext(), likedRecipes, firebaseAuth);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recipeAdapter);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Liked"));
        tabLayout.addTab(tabLayout.newTab().setText("Downloaded"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showLikedRecipes();
                } else {
                    showDownloadedRecipes();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        showLikedRecipes(); // default
    }

    // =========================
    // LIKED RECIPES (Firebase)
    // =========================
    private void showLikedRecipes() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            showEmpty("Please login to see liked recipes");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvNoRecipes.setVisibility(View.GONE);

        recipesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                likedRecipes.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Recipe recipe = snap.getValue(Recipe.class);

                    if (recipe != null && recipe.isLikedBy(user.getUid())) {
                        likedRecipes.add(recipe);
                    }
                }

                likedRecipes.sort(
                        (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp())
                );

                recipeAdapter.updateRecipes(likedRecipes);

                progressBar.setVisibility(View.GONE);

                if (likedRecipes.isEmpty()) {
                    showEmpty(

                                    "Tap the ⭐ button to save them here!"
                    );
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================
    // DOWNLOADED RECIPES (SharedPrefs)
    // ==================================
    private void showDownloadedRecipes() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            showEmpty("Please login to see downloaded recipes");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvNoRecipes.setVisibility(View.GONE);

        downloadedRecipes.clear();

        SharedPreferences prefs = requireContext().getSharedPreferences(
                "RecipePrefs_" + user.getUid(),
                Context.MODE_PRIVATE
        );

        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("recipe_")) {
                try {
                    String json = (String) entry.getValue();
                    DownloadedRecipe downloaded =
                            gson.fromJson(json, DownloadedRecipe.class);
                    downloadedRecipes.add(downloaded.toRecipe());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        downloadedRecipes.sort(
                (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp())
        );

        recipeAdapter.updateRecipes(downloadedRecipes);

        progressBar.setVisibility(View.GONE);

        if (downloadedRecipes.isEmpty()) {
            showEmpty(

                            "Tap the ⬇ button to save recipes offline!"
            );
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvNoRecipes.setText(message);
        tvNoRecipes.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (tabLayout.getSelectedTabPosition() == 0) {
            showLikedRecipes();
        } else {
            showDownloadedRecipes();
        }
    }
}