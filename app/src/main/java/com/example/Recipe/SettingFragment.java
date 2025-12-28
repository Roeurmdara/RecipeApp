package com.example.Recipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.Recipe.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class SettingFragment extends Fragment {

    private TextView tvUserName, tvUserEmail, tvUserInitial;
    private RadioGroup radioGroupTheme, radioGroupLang;
    private RadioButton rbLight, rbDark, rbEnglish, rbKhmer;
    private Button btnLogout;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    public SettingFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Initialize UI
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserInitial = view.findViewById(R.id.tvUserInitial);
        radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        radioGroupLang = view.findViewById(R.id.radioGroupLang);
        rbLight = view.findViewById(R.id.rbLight);
        rbDark = view.findViewById(R.id.rbDark);
        rbEnglish = view.findViewById(R.id.rbEnglish);
        rbKhmer = view.findViewById(R.id.rbKhmer);


        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        loadUserInfo();
        loadPreferences();

        // Theme selection listener
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

        // Language selection listener
        radioGroupLang.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = (checkedId == R.id.rbKhmer) ? "km" : "en";
            sharedPreferences.edit().putString("My_Lang", lang).apply();
            setLocale(lang);
            requireActivity().recreate();
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Get user info from Firebase Database
        databaseReference.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = "User";
                        String email = currentUser.getEmail(); // fallback

                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                if (user.getName() != null && !user.getName().isEmpty()) name = user.getName();
                                if (user.getEmail() != null && !user.getEmail().isEmpty()) email = user.getEmail();
                            }
                        } else {
                            // Optional: If database record doesn't exist, use Firebase Auth info
                            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty())
                                name = currentUser.getDisplayName();
                        }

                        tvUserName.setText(name);
                        tvUserEmail.setText(email != null ? email : "");
                        tvUserInitial.setText(!name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "U");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load user info", Toast.LENGTH_SHORT).show();
                    }
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
}
