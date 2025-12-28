package com.example.Recipe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Recipe.model.Recipe;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MyRecipeAdapter extends RecyclerView.Adapter<MyRecipeAdapter.MyRecipeViewHolder> {

    private Context context;
    private List<Recipe> recipeList;
    private DatabaseReference databaseReference;

    public MyRecipeAdapter(Context context, List<Recipe> recipeList) {
        this.context = context;
        this.recipeList = recipeList;

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        this.databaseReference = database.getReference("recipes");
    }

    @NonNull
    @Override
    public MyRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_recipe_card, parent, false);
        return new MyRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyRecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        holder.tvRecipeName.setText(recipe.getName());
        holder.tvCategory.setText(recipe.getCategory());

        // Load Base64 image
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(recipe.getImageUrl(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivRecipeImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivRecipeImage.setImageDrawable(null);
                holder.ivRecipeImage.setBackgroundColor(0xFFE0E0E0);
            }
        } else {
            holder.ivRecipeImage.setImageDrawable(null);
            holder.ivRecipeImage.setBackgroundColor(0xFFE0E0E0);
        }

        // Click card to view details
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getId());
            context.startActivity(intent);
        });

        // Edit button - Opens EditRecipeActivity
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getId());
            context.startActivity(intent);
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation(recipe, position);
        });
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    private void showDeleteConfirmation(Recipe recipe, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete \"" + recipe.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteRecipe(recipe, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe(Recipe recipe, int position) {
        if (recipe.getId() == null) {
            Toast.makeText(context, "Error: Recipe ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(recipe.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, recipe.getName() + " deleted successfully",
                            Toast.LENGTH_SHORT).show();

                    // Note: No need to manually remove from list
                    // Firebase ValueEventListener in DiscoverFragment will auto-update
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    public static class MyRecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvCategory;
        Button btnEdit, btnDelete;

        public MyRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipeList = newRecipes;
        notifyDataSetChanged();
    }
}