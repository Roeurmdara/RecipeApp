package com.example.Recipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Recipe.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private Context context;
    private List<Recipe> recipeList;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private Gson gson;

    public RecipeAdapter(Context context, List<Recipe> recipeList, FirebaseAuth firebaseAuth) {
        this.context = context;
        this.recipeList = recipeList;
        this.firebaseAuth = firebaseAuth;
        this.gson = new Gson();
        databaseReference = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("recipes");
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        FirebaseUser user = firebaseAuth.getCurrentUser();

        // Set recipe name
        holder.tvRecipeName.setText(recipe.getName());

        // Set username with underline and icon
        holder.tvUserName.setText(recipe.getUserName());
        holder.tvUserName.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.baseline_user, 0, 0, 0
        );
        holder.tvUserName.setCompoundDrawablePadding(8);
        holder.tvUserName.setGravity(Gravity.CENTER_VERTICAL);
        holder.tvUserName.setPaintFlags(
                holder.tvUserName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
        );

        // Set category
        holder.tvCategory.setText(recipe.getCategory());

        // Set counts
        holder.tvLikeCount.setText(String.valueOf(recipe.getLikeCount()));
        holder.tvDownloadCount.setText(String.valueOf(recipe.getDownloadCount()));

        // Load Base64 image
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(recipe.getImageUrl(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivRecipeImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivRecipeImage.setBackgroundColor(0xFFE0E0E0);
            }
        } else {
            holder.ivRecipeImage.setBackgroundColor(0xFFE0E0E0);
        }

        // Show correct like icon based on user's like status
        if (user != null) {
            Map<String, Boolean> likes = recipe.getLikes();
            boolean isLiked = likes != null && likes.containsKey(user.getUid()) && likes.get(user.getUid());
            holder.ivLike.setImageResource(
                    isLiked ? R.drawable.baseline_star_24_liked : R.drawable.baseline_star_24
            );
        } else {
            holder.ivLike.setImageResource(R.drawable.baseline_star_24);
        }

        // Show download icon status
        boolean isDownloaded = isRecipeDownloaded(recipe.getId());
        holder.ivDownload.setImageResource(
                isDownloaded ? R.drawable.baseline_download_done_24 : R.drawable.baseline_download_24
        );

        // Card click: open recipe detail
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailRecipeActivity.class);
            if (isDownloaded && user != null) {
                SharedPreferences prefs = context.getSharedPreferences(
                        "RecipePrefs_" + user.getUid(), Context.MODE_PRIVATE
                );
                String json = prefs.getString("recipe_" + recipe.getId(), null);
                if (json != null) {
                    intent.putExtra("RECIPE_OBJECT", json);
                } else {
                    intent.putExtra("recipeId", recipe.getId());
                }
            } else {
                intent.putExtra("RECIPE_OBJECT", gson.toJson(recipe));
            }
            context.startActivity(intent);
        });

        // Username click: open user profile
        holder.tvUserName.setOnClickListener(v -> {
            if (recipe.getUserId() != null && !recipe.getUserId().isEmpty()) {
                openUserProfile(recipe.getUserId());
            } else {
                Toast.makeText(context, "User profile not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Like button
        holder.btnLike.setOnClickListener(v -> {
            if (user != null) {
                toggleLike(recipe, user.getUid(), holder);
            } else {
                Toast.makeText(context, "Login required", Toast.LENGTH_SHORT).show();
            }
        });

        // Download button
        holder.btnDownload.setOnClickListener(v -> {
            if (user != null) {
                toggleDownload(recipe, holder);
            } else {
                Toast.makeText(context, "Login required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUserProfile(String userId) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // Create UserProfileFragment with the userId
            UserProfileFragment profileFragment = UserProfileFragment.newInstance(userId);
            transaction.replace(R.id.fragment_container, profileFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void toggleLike(Recipe recipe, String uid, RecipeViewHolder holder) {
        Map<String, Boolean> likes = recipe.getLikes() != null ? recipe.getLikes() : new HashMap<>();
        boolean liked = likes.containsKey(uid) && likes.get(uid);

        if (liked) {
            // User is unliking
            likes.remove(uid);
            recipe.setLikeCount(Math.max(0, recipe.getLikeCount() - 1));
            holder.ivLike.setImageResource(R.drawable.baseline_star_24); // Empty star
        } else {
            // User is liking
            likes.put(uid, true);
            recipe.setLikeCount(recipe.getLikeCount() + 1);
            holder.ivLike.setImageResource(R.drawable.baseline_star_24_liked); // Filled star
        }

        recipe.setLikes(likes);
        holder.tvLikeCount.setText(String.valueOf(recipe.getLikeCount()));
        databaseReference.child(recipe.getId()).child("likes").setValue(likes);
        databaseReference.child(recipe.getId()).child("likeCount").setValue(recipe.getLikeCount());
    }

    private void toggleDownload(Recipe recipe, RecipeViewHolder holder) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs =
                context.getSharedPreferences("RecipePrefs_" + user.getUid(), Context.MODE_PRIVATE);

        boolean isDownloaded = isRecipeDownloaded(recipe.getId());

        if (isDownloaded) {
            // Remove from downloaded
            prefs.edit().remove("recipe_" + recipe.getId()).apply();
            holder.ivDownload.setImageResource(R.drawable.baseline_download_24);
            Toast.makeText(context, "Removed from downloads", Toast.LENGTH_SHORT).show();
        } else {
            // Add to downloaded
            prefs.edit().putString("recipe_" + recipe.getId(), gson.toJson(recipe)).apply();
            holder.ivDownload.setImageResource(R.drawable.baseline_download_done_24);

            // Increment download count
            recipe.setDownloadCount(recipe.getDownloadCount() + 1);
            holder.tvDownloadCount.setText(String.valueOf(recipe.getDownloadCount()));
            databaseReference.child(recipe.getId()).child("downloadCount").setValue(recipe.getDownloadCount());

            Toast.makeText(context, "Recipe downloaded", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isRecipeDownloaded(String id) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return false;
        SharedPreferences prefs = context.getSharedPreferences("RecipePrefs_" + user.getUid(), Context.MODE_PRIVATE);
        return prefs.contains("recipe_" + id);
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        recipeList = newRecipes;
        notifyDataSetChanged();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivRecipeImage, ivLike, ivDownload;
        TextView tvRecipeName, tvUserName, tvCategory, tvLikeCount, tvDownloadCount;
        LinearLayout btnLike, btnDownload;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnLike = itemView.findViewById(R.id.btnLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            ivDownload = itemView.findViewById(R.id.ivDownload);
            tvDownloadCount = itemView.findViewById(R.id.tvDownloadCount);
        }
    }
}