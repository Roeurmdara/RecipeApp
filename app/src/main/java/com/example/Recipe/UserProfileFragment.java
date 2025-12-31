package com.example.Recipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Recipe.model.Recipe;
import com.example.Recipe.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private TextView tvUserName, tvUserEmail, tvUserInitial;
    private TextView tvRecipeCount, tvFollowerCount, tvFollowingCount, tvEmptyState, tvRecipesTitle;
    private LinearLayout layoutFollowers, layoutFollowing, layoutSettings;
    private RecyclerView recyclerViewRecipes;
    private ProgressBar progressBar;
    private Button btnFollow, btnLogout;
    private RadioGroup radioGroupTheme, radioGroupLang;
    private RadioButton rbLight, rbDark, rbEnglish, rbKhmer;

    private DatabaseReference usersRef, recipesRef;
    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;

    private String userId;
    private String currentUserId;
    private User profileUser;
    private boolean isOwnProfile = false;

    private RecipeAdapter recipeAdapter;
    private List<Recipe> userRecipes = new ArrayList<>();

    private ValueEventListener userProfileListener;

    public UserProfileFragment() {
    }

    public static UserProfileFragment newInstance(String userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        initViews(view);
        initFirebase();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            Log.d(TAG, "Current User ID: " + currentUserId);
            Log.d(TAG, "Viewing Profile ID: " + userId);

            if (userId == null || userId.isEmpty()) {
                userId = currentUserId;
            }

            checkIfOwnProfile();
            setupRecyclerView();

            if (isOwnProfile) {
                loadPreferences();
            }

            setupListeners();
            loadUserProfile();
            loadUserRecipes();
        } else {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent memory leaks
        if (userProfileListener != null && userId != null) {
            usersRef.child(userId).removeEventListener(userProfileListener);
        }
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserInitial = view.findViewById(R.id.tvUserInitial);
        tvRecipeCount = view.findViewById(R.id.tvRecipeCount);
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvRecipesTitle = view.findViewById(R.id.tvRecipesTitle);

        layoutFollowers = view.findViewById(R.id.layoutFollowers);
        layoutFollowing = view.findViewById(R.id.layoutFollowing);
        layoutSettings = view.findViewById(R.id.layoutSettings);

        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        btnFollow = view.findViewById(R.id.btnFollow);
        btnLogout = view.findViewById(R.id.btnLogout);

        radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        radioGroupLang = view.findViewById(R.id.radioGroupLang);
        rbLight = view.findViewById(R.id.rbLight);
        rbDark = view.findViewById(R.id.rbDark);
        rbEnglish = view.findViewById(R.id.rbEnglish);
        rbKhmer = view.findViewById(R.id.rbKhmer);

        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
    }

    private void initFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        usersRef = database.getReference("users");
        recipesRef = database.getReference("recipes");
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void checkIfOwnProfile() {
        isOwnProfile = userId != null && userId.equals(currentUserId);
        Log.d(TAG, "Is Own Profile: " + isOwnProfile);

        if (isOwnProfile) {
            btnFollow.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.VISIBLE);
            tvRecipesTitle.setText("My Recipes");
        } else {
            btnFollow.setVisibility(View.VISIBLE);
            layoutSettings.setVisibility(View.GONE);
            tvRecipesTitle.setText("Recipes");
        }
    }

    private void setupRecyclerView() {
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recipeAdapter = new RecipeAdapter(getContext(), userRecipes, firebaseAuth);
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void loadUserProfile() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "userId is null or empty!");
            Toast.makeText(getContext(), "Invalid user profile", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Loading profile for userId: " + userId);

        // Use ValueEventListener for real-time updates
        userProfileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Profile snapshot exists: " + snapshot.exists());

                if (snapshot.exists()) {
                    profileUser = snapshot.getValue(User.class);
                    if (profileUser != null) {
                        // Ensure userId is set
                        if (profileUser.getUserId() == null) {
                            profileUser.setUserId(userId);
                        }

                        // Initialize maps if null
                        if (profileUser.getFollowers() == null) {
                            profileUser.setFollowers(new HashMap<>());
                        }
                        if (profileUser.getFollowing() == null) {
                            profileUser.setFollowing(new HashMap<>());
                        }

                        Log.d(TAG, "Loaded user: " + profileUser.getName());

                        // If name is null or empty, update it from recipes
                        if (profileUser.getName() == null || profileUser.getName().isEmpty()) {
                            Log.d(TAG, "User name is null, fetching from recipes...");
                            fixUserNameFromRecipes();
                        } else {
                            displayUserProfile();
                            updateFollowButton();
                        }
                    } else {
                        Log.e(TAG, "Profile user is null after getValue");
                        createUserProfile();
                    }
                } else {
                    Log.d(TAG, "User profile doesn't exist, creating...");
                    createUserProfile();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        usersRef.child(userId).addValueEventListener(userProfileListener);
    }

    private void createUserProfile() {
        Log.d(TAG, "Creating user profile for: " + userId);

        // Try to get username from their recipes first
        recipesRef.orderByChild("userId").equalTo(userId).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String username = null;
                        String email = "";

                        // Check recipes
                        for (DataSnapshot recipeSnapshot : dataSnapshot.getChildren()) {
                            Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                            if (recipe != null) {
                                Log.d(TAG, "Found recipe by user: " + recipe.getUserName());
                                if (recipe.getUserName() != null && !recipe.getUserName().isEmpty()) {
                                    username = recipe.getUserName();
                                    break;
                                }
                            }
                        }

                        // If own profile, get from Firebase Auth
                        if (isOwnProfile) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                Log.d(TAG, "Getting info from Firebase Auth");
                                if (username == null || username.isEmpty()) {
                                    if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
                                        username = firebaseUser.getDisplayName();
                                        Log.d(TAG, "Got displayName: " + username);
                                    } else if (firebaseUser.getEmail() != null) {
                                        username = firebaseUser.getEmail().split("@")[0];
                                        Log.d(TAG, "Got username from email: " + username);
                                    }
                                }
                                email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                            }
                        }

                        // Final fallback
                        if (username == null || username.isEmpty()) {
                            username = "User";
                            Log.d(TAG, "Using fallback username: User");
                        }

                        // Create and save user
                        User newUser = new User(userId, username, email);
                        profileUser = newUser;

                        Log.d(TAG, "Saving new user profile: " + username + " with ID: " + userId);
                        usersRef.child(userId).setValue(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User profile saved successfully");
                                    displayUserProfile();
                                    updateFollowButton();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save user profile", e);
                                    Toast.makeText(getContext(), "Failed to create profile", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading recipes: " + error.getMessage());

                        // Create basic profile as fallback
                        String fallbackName = "User";
                        String fallbackEmail = "";

                        if (isOwnProfile) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                if (firebaseUser.getDisplayName() != null) {
                                    fallbackName = firebaseUser.getDisplayName();
                                } else if (firebaseUser.getEmail() != null) {
                                    fallbackEmail = firebaseUser.getEmail();
                                    fallbackName = fallbackEmail.split("@")[0];
                                }
                            }
                        }

                        profileUser = new User(userId, fallbackName, fallbackEmail);
                        usersRef.child(userId).setValue(profileUser);
                        displayUserProfile();
                    }
                });
    }

    private void displayUserProfile() {
        if (profileUser == null) {
            Log.e(TAG, "Cannot display: profileUser is null");
            return;
        }

        String name = profileUser.getName();
        String email = profileUser.getEmail();

        if (name == null || name.isEmpty()) {
            name = "User";
        }

        Log.d(TAG, "Displaying: " + name + " | " + email);

        tvUserName.setText(name);
        tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());

        if (isOwnProfile && email != null && !email.isEmpty()) {
            tvUserEmail.setText(email);
            tvUserEmail.setVisibility(View.VISIBLE);
        } else {
            tvUserEmail.setVisibility(View.GONE);
        }

        tvRecipeCount.setText(String.valueOf(profileUser.getRecipeCount()));
        tvFollowerCount.setText(String.valueOf(profileUser.getFollowerCount()));
        tvFollowingCount.setText(String.valueOf(profileUser.getFollowingCount()));
    }

    private void updateFollowButton() {
        if (isOwnProfile || profileUser == null || currentUserId == null) {
            return;
        }

        boolean isFollowing = profileUser.isFollowedBy(currentUserId);
        Log.d(TAG, "User is following: " + isFollowing);

        if (isFollowing) {
            btnFollow.setText("Unfollow");
            btnFollow.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.darker_gray)
            );
        } else {
            btnFollow.setText("Follow");
            btnFollow.setBackgroundTintList(
                    getResources().getColorStateList(R.color.purple_500)
            );
        }
    }

    private void loadUserRecipes() {
        if (userId == null) return;

        Log.d(TAG, "Loading recipes for: " + userId);
        recipesRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userRecipes.clear();

                        for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                            Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                            if (recipe != null) {
                                userRecipes.add(recipe);
                            }
                        }

                        Log.d(TAG, "Found " + userRecipes.size() + " recipes");

                        if (userRecipes.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerViewRecipes.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerViewRecipes.setVisibility(View.VISIBLE);
                        }

                        recipeAdapter.updateRecipes(userRecipes);

                        // Update recipe count
                        int recipeCount = userRecipes.size();
                        usersRef.child(userId).child("recipeCount").setValue(recipeCount);
                        if (profileUser != null) {
                            profileUser.setRecipeCount(recipeCount);
                            tvRecipeCount.setText(String.valueOf(recipeCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load recipes: " + error.getMessage());
                        Toast.makeText(getContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupListeners() {
        layoutFollowers.setOnClickListener(v -> {
            if (profileUser != null) {
                Toast.makeText(getContext(), "Followers: " + profileUser.getFollowerCount(), Toast.LENGTH_SHORT).show();
            }
        });

        layoutFollowing.setOnClickListener(v -> {
            if (profileUser != null) {
                Toast.makeText(getContext(), "Following: " + profileUser.getFollowingCount(), Toast.LENGTH_SHORT).show();
            }
        });

        btnFollow.setOnClickListener(v -> toggleFollow());

        if (isOwnProfile) {
            radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (checkedId == R.id.rbLight) {
                    editor.putBoolean("isDarkMode", false);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else if (checkedId == R.id.rbDark) {
                    editor.putBoolean("isDarkMode", true);
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                editor.apply();
            });

            radioGroupLang.setOnCheckedChangeListener((group, checkedId) -> {
                String currentLang = sharedPreferences.getString("My_Lang", "en");
                String newLang = (checkedId == R.id.rbKhmer) ? "km" : "en";

                if (!currentLang.equals(newLang)) {
                    sharedPreferences.edit().putString("My_Lang", newLang).apply();
                    setLocale(newLang);
                    requireActivity().recreate();
                }
            });

            btnLogout.setOnClickListener(v -> {
                firebaseAuth.signOut();
                Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }

    private void toggleFollow() {
        Log.d(TAG, "Toggle follow clicked");

        if (currentUserId == null || profileUser == null || userId == null) {
            Toast.makeText(getContext(), "Cannot follow at this time", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Null values - currentUserId: " + currentUserId + ", profileUser: " + (profileUser != null) + ", userId: " + userId);
            return;
        }

        btnFollow.setEnabled(false);
        boolean wasFollowing = profileUser.isFollowedBy(currentUserId);
        Log.d(TAG, "Was following: " + wasFollowing);

        // Update target user's followers
        Map<String, Boolean> followers = new HashMap<>(profileUser.getFollowers());

        if (wasFollowing) {
            followers.remove(currentUserId);
            profileUser.setFollowerCount(Math.max(0, profileUser.getFollowerCount() - 1));
        } else {
            followers.put(currentUserId, true);
            profileUser.setFollowerCount(profileUser.getFollowerCount() + 1);
        }

        profileUser.setFollowers(followers);

        // Update database
        Map<String, Object> targetUpdates = new HashMap<>();
        targetUpdates.put("followers", followers);
        targetUpdates.put("followerCount", profileUser.getFollowerCount());

        usersRef.child(userId).updateChildren(targetUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated target user followers");

                    // Now update current user's following
                    usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User currentUser = snapshot.getValue(User.class);
                            if (currentUser == null) {
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                String name = "User";
                                String email = "";
                                if (firebaseUser != null) {
                                    name = firebaseUser.getDisplayName() != null ?
                                            firebaseUser.getDisplayName() : "User";
                                    email = firebaseUser.getEmail() != null ?
                                            firebaseUser.getEmail() : "";
                                }
                                currentUser = new User(currentUserId, name, email);
                            }

                            Map<String, Boolean> following = new HashMap<>(currentUser.getFollowing());

                            if (wasFollowing) {
                                following.remove(userId);
                                currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));
                            } else {
                                following.put(userId, true);
                                currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);
                            }

                            currentUser.setFollowing(following);

                            Map<String, Object> currentUserUpdates = new HashMap<>();
                            currentUserUpdates.put("following", following);
                            currentUserUpdates.put("followingCount", currentUser.getFollowingCount());

                            usersRef.child(currentUserId).updateChildren(currentUserUpdates)
                                    .addOnSuccessListener(aVoid2 -> {
                                        btnFollow.setEnabled(true);
                                        String message = wasFollowing ?
                                                "Unfollowed " + profileUser.getName() :
                                                "Following " + profileUser.getName();
                                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Follow action completed successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        btnFollow.setEnabled(true);
                                        Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to update current user", e);
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            btnFollow.setEnabled(true);
                            Toast.makeText(getContext(), "Failed to follow", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Database error", error.toException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    btnFollow.setEnabled(true);
                    Toast.makeText(getContext(), "Failed to follow", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update target user", e);
                });
    }

    private void loadPreferences() {
        boolean isDark = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDark) rbDark.setChecked(true);
        else rbLight.setChecked(true);

        String lang = sharedPreferences.getString("My_Lang", "en");
        if ("km".equals(lang)) rbKhmer.setChecked(true);
        else rbEnglish.setChecked(true);
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(
                config, requireActivity().getResources().getDisplayMetrics());
    }

    private void fixUserNameFromRecipes() {
        Log.d(TAG, "Attempting to fix null username from recipes");
        recipesRef.orderByChild("userId").equalTo(userId).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String username = null;

                        for (DataSnapshot recipeSnapshot : dataSnapshot.getChildren()) {
                            Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                            if (recipe != null && recipe.getUserName() != null && !recipe.getUserName().isEmpty()) {
                                username = recipe.getUserName();
                                Log.d(TAG, "Found username in recipe: " + username);
                                break;
                            }
                        }

                        if (username == null && isOwnProfile) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
                                    username = firebaseUser.getDisplayName();
                                } else if (firebaseUser.getEmail() != null) {
                                    username = firebaseUser.getEmail().split("@")[0];
                                }
                            }
                        }

                        if (username == null || username.isEmpty()) {
                            username = "User";
                        }

                        // Update the profile name

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch recipes for username", error.toException());
                        profileUser.setName("User");
                        displayUserProfile();
                        updateFollowButton();
                    }
                });
    }
}