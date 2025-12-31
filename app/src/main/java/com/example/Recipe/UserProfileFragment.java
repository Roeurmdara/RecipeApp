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

    public UserProfileFragment() {
        Log.d(TAG, "Constructor called");
    }

    public static UserProfileFragment newInstance(String userId) {
        Log.d(TAG, "newInstance called with userId: " + userId);
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
            Log.d(TAG, "Got userId from arguments: " + userId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");

        try {
            View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
            Log.d(TAG, "Layout inflated successfully");

            initViews(view);
            Log.d(TAG, "Views initialized");

            initFirebase();
            Log.d(TAG, "Firebase initialized");

            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                currentUserId = firebaseUser.getUid();
                Log.d(TAG, "Current user ID: " + currentUserId);

                if (userId == null || userId.isEmpty()) {
                    userId = currentUserId;
                    Log.d(TAG, "No userId provided, using current user: " + userId);
                }

                checkIfOwnProfile();
                Log.d(TAG, "Is own profile: " + isOwnProfile);

                setupRecyclerView();
                setupListeners();
                loadUserProfile();
                loadUserRecipes();

                if (isOwnProfile) {
                    loadPreferences();
                }

                Log.d(TAG, "All initialization complete");
            } else {
                Log.e(TAG, "No user logged in");
                Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(requireContext());
        }
    }

    private void initViews(View view) {
        try {
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

            Log.d(TAG, "All views found successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void initFirebase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
            );
            usersRef = database.getReference("users");
            recipesRef = database.getReference("recipes");
            firebaseAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            throw e;
        }
    }

    private void checkIfOwnProfile() {
        isOwnProfile = userId != null && userId.equals(currentUserId);

        if (isOwnProfile) {
            // Own profile: show settings, hide follow button
            btnFollow.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.VISIBLE);
            tvRecipesTitle.setText("My Recipes");
            tvUserEmail.setVisibility(View.VISIBLE); // Show email on own profile
            Log.d(TAG, "Showing own profile with settings");
        } else {
            // Other user's profile: show follow button, hide settings
            btnFollow.setVisibility(View.VISIBLE);
            layoutSettings.setVisibility(View.GONE);
            tvRecipesTitle.setText("Recipes");
            tvUserEmail.setVisibility(View.GONE); // Hide email on other profiles
            Log.d(TAG, "Showing other user profile with follow button");
        }
    }

    private void setupRecyclerView() {
        try {
            recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
            recipeAdapter = new RecipeAdapter(getContext(), userRecipes, firebaseAuth);
            recyclerViewRecipes.setAdapter(recipeAdapter);
            Log.d(TAG, "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void loadUserProfile() {
        if (userId == null) {
            Log.e(TAG, "userId is null in loadUserProfile");
            return;
        }

        Log.d(TAG, "Loading user profile for: " + userId);
        progressBar.setVisibility(View.VISIBLE);

        usersRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "User profile data received");
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                String name = "User";
                String email = "";

                if (snapshot.exists()) {
                    profileUser = snapshot.getValue(User.class);
                    if (profileUser != null) {
                        if (profileUser.getName() != null && !profileUser.getName().isEmpty()) {
                            name = profileUser.getName();
                        }
                        if (profileUser.getEmail() != null && !profileUser.getEmail().isEmpty()) {
                            email = profileUser.getEmail();
                        }
                        displayUserInfo();
                        updateFollowButton();
                        Log.d(TAG, "Profile user loaded: " + name);
                    }
                } else {
                    Log.d(TAG, "User profile doesn't exist, creating basic profile");
                    if (isOwnProfile && firebaseUser != null) {
                        email = firebaseUser.getEmail();
                        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
                            name = firebaseUser.getDisplayName();
                        }
                        createBasicUserProfile(userId, name, email);
                    }
                }

                tvUserName.setText(name);
                if (isOwnProfile) {
                    tvUserEmail.setText(email != null ? email : "");
                }
                tvUserInitial.setText(!name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "U");

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user profile: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createBasicUserProfile(String uid, String username, String email) {
        Log.d(TAG, "Creating basic user profile");
        User newUser = new User(uid, username, email);
        usersRef.child(uid).setValue(newUser);
    }

    private void displayUserInfo() {
        if (profileUser == null) {
            Log.w(TAG, "profileUser is null in displayUserInfo");
            return;
        }

        tvRecipeCount.setText(String.valueOf(profileUser.getRecipeCount()));
        tvFollowerCount.setText(String.valueOf(profileUser.getFollowerCount()));
        tvFollowingCount.setText(String.valueOf(profileUser.getFollowingCount()));
        Log.d(TAG, "User info displayed");
    }

    private void updateFollowButton() {
        if (isOwnProfile || profileUser == null || currentUserId == null) return;

        boolean isFollowing = profileUser.isFollowedBy(currentUserId);

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
        Log.d(TAG, "Follow button updated, isFollowing: " + isFollowing);
    }

    private void loadUserRecipes() {
        if (userId == null) {
            Log.e(TAG, "userId is null in loadUserRecipes");
            return;
        }

        Log.d(TAG, "Loading recipes for user: " + userId);
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

                        Log.d(TAG, "Loaded " + userRecipes.size() + " recipes");

                        if (userRecipes.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerViewRecipes.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerViewRecipes.setVisibility(View.VISIBLE);
                        }

                        recipeAdapter.updateRecipes(userRecipes);

                        // Update recipe count
                        usersRef.child(userId).child("recipeCount").setValue(userRecipes.size());
                        if (profileUser != null) {
                            profileUser.setRecipeCount(userRecipes.size());
                            tvRecipeCount.setText(String.valueOf(userRecipes.size()));
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
                String lang = (checkedId == R.id.rbKhmer) ? "km" : "en";
                sharedPreferences.edit().putString("My_Lang", lang).apply();
                setLocale(lang);
                requireActivity().recreate();
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
        if (currentUserId == null || profileUser == null || userId == null) {
            Toast.makeText(getContext(), "Unable to process follow action", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent following yourself
        if (currentUserId.equals(userId)) {
            Toast.makeText(getContext(), "You cannot follow yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isFollowing = profileUser.isFollowedBy(currentUserId);

        // Update target user's followers
        Map<String, Boolean> targetFollowers = profileUser.getFollowers();
        if (targetFollowers == null) {
            targetFollowers = new HashMap<>();
        }

        if (isFollowing) {
            targetFollowers.remove(currentUserId);
            profileUser.setFollowerCount(Math.max(0, profileUser.getFollowerCount() - 1));
        } else {
            targetFollowers.put(currentUserId, true);
            profileUser.setFollowerCount(profileUser.getFollowerCount() + 1);
        }

        // Update Firebase for target user
        usersRef.child(userId).child("followers").setValue(targetFollowers);
        usersRef.child(userId).child("followerCount").setValue(profileUser.getFollowerCount());

        // Update current user's following list
        usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser == null) {
                    currentUser = new User(currentUserId, "User", "");
                }

                Map<String, Boolean> following = currentUser.getFollowing();
                if (following == null) {
                    following = new HashMap<>();
                }

                if (isFollowing) {
                    following.remove(userId);
                    currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));
                    Toast.makeText(getContext(), "Unfollowed " + profileUser.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    following.put(userId, true);
                    currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);
                    Toast.makeText(getContext(), "Following " + profileUser.getName(), Toast.LENGTH_SHORT).show();
                }

                usersRef.child(currentUserId).child("following").setValue(following);
                usersRef.child(currentUserId).child("followingCount")
                        .setValue(currentUser.getFollowingCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to update following: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to update following status", Toast.LENGTH_SHORT).show();
            }
        });

        // Update UI
        updateFollowButton();
        displayUserInfo();
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
}