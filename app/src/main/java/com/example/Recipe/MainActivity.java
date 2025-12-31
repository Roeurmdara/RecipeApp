package com.example.Recipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddRecipe;
    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Load language before onCreate
        SharedPreferences sharedPreferences = newBase.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String lang = sharedPreferences.getString("My_Lang", "en");

        Locale locale = new Locale(lang);
        Configuration config = new Configuration();
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);

        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme before super.onCreate()
        loadTheme();

        super.onCreate(savedInstanceState);

        // Firebase Authentication Check
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddRecipe = findViewById(R.id.fab_add_recipe);

        // Load default fragment
        if (savedInstanceState == null) {
            Log.d(TAG, "Loading default fragment (HomeFragment)");
            loadFragment(new HomeFragment());
        }

        // Bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            Log.d(TAG, "Bottom nav item selected: " + itemId);

            if (itemId == R.id.nav_home) {
                Log.d(TAG, "Loading HomeFragment");
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_dicover) {
                Log.d(TAG, "Loading DiscoverFragment");
                selectedFragment = new DiscoverFragment();
            } else if (itemId == R.id.nav_setting) {
                Log.d(TAG, "Loading FavoritesFragment");
                selectedFragment = new FavoritesFragment();
            } else if (itemId == R.id.nav_profile) {
                Log.d(TAG, "Loading UserProfileFragment (own profile)");
                selectedFragment = new UserProfileFragment(); // Own profile
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // FAB click listener for Add Recipe
        fabAddRecipe.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked - Loading SubmitRecipeFragment");
            loadFragment(new SubmitRecipeFragment());
            // Deselect all bottom navigation items
            bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setChecked(false);
            }
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
        });
    }

    private void loadFragment(Fragment fragment) {
        try {
            if (fragment != null) {
                Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.commit();
                Log.d(TAG, "Fragment loaded successfully");
            } else {
                Log.e(TAG, "Fragment is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public void switchToExpenseList() {
        // Now switches to the FAB action
        fabAddRecipe.performClick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_signout) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Theme loader
    private void loadTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}