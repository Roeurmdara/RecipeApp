package com.example.Recipe;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private MyRecipeAdapter myRecipeAdapter;
    private List<Recipe> myRecipeList;
    private ProgressBar progressBar;
    private TextView tvNoRecipes, tvRecipeCount;

    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMyRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoRecipes = view.findViewById(R.id.tvNoRecipes);
        tvRecipeCount = view.findViewById(R.id.tvRecipeCount);

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        databaseReference = database.getReference("recipes");

        myRecipeList = new ArrayList<>();
        myRecipeAdapter = new MyRecipeAdapter(getContext(), myRecipeList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(myRecipeAdapter);

        loadRecipes();

        return view;
    }

    private void loadRecipes() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRecipes.setVisibility(View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            progressBar.setVisibility(View.GONE);
            tvNoRecipes.setVisibility(View.VISIBLE);
            tvNoRecipes.setText("Please log in to see your recipes.");
            return;
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myRecipeList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Recipe recipe = ds.getValue(Recipe.class);
                    if (recipe != null && currentUserId.equals(recipe.getUserId())) {
                        recipe.setId(ds.getKey()); // Set Firebase key as ID
                        myRecipeList.add(recipe);
                    }
                }

                if (!myRecipeList.isEmpty()) {
                    // Sort newest first
                    myRecipeList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    myRecipeAdapter.updateRecipes(myRecipeList);

                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoRecipes.setVisibility(View.GONE);

                    tvRecipeCount.setVisibility(View.VISIBLE);
                    tvRecipeCount.setText(myRecipeList.size() + " recipe" + (myRecipeList.size() > 1 ? "s" : ""));
                } else {
                    recyclerView.setVisibility(View.GONE);
                    tvNoRecipes.setVisibility(View.VISIBLE);
                    tvNoRecipes.setText("No recipes found.\nTap '+' to create one!");
                    tvRecipeCount.setVisibility(View.GONE);
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load recipes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes();
    }
}
