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
import com.google.firebase.database.*;

import java.util.Locale;

public class SettingFragment extends Fragment {

    private TextView tvUserName, tvUserEmail, tvUserInitial;
    private RadioButton rbLight, rbDark, rbEnglish, rbKhmer;
    private RadioGroup radioGroupTheme, radioGroupLang;
    private Button btnLogout;

    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserInitial = view.findViewById(R.id.tvUserInitial);
        rbLight = view.findViewById(R.id.rbLight);
        rbDark = view.findViewById(R.id.rbDark);
        rbEnglish = view.findViewById(R.id.rbEnglish);
        rbKhmer = view.findViewById(R.id.rbKhmer);
        radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        radioGroupLang = view.findViewById(R.id.radioGroupLang);
        btnLogout = view.findViewById(R.id.btnLogout);

        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        loadUserInfo();
        loadPreferences();

        radioGroupTheme.setOnCheckedChangeListener((g, id) -> {
            SharedPreferences.Editor e = sharedPreferences.edit();
            if (id == R.id.rbDark) {
                e.putBoolean("isDarkMode", true);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                e.putBoolean("isDarkMode", false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            e.apply();
        });

        radioGroupLang.setOnCheckedChangeListener((g, id) -> {
            String lang = (id == R.id.rbKhmer) ? "km" : "en";
            sharedPreferences.edit().putString("My_Lang", lang).apply();
            setLocale(lang);
            requireActivity().recreate();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        databaseReference.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = "User";
                        String email = user.getEmail();

                        if (snapshot.exists()) {
                            User u = snapshot.getValue(User.class);
                            if (u != null && u.getName() != null) name = u.getName();
                        }

                        tvUserName.setText(name);
                        tvUserEmail.setText(email);
                        tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPreferences() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) rbDark.setChecked(true);
        else rbLight.setChecked(true);

        if ("km".equals(sharedPreferences.getString("My_Lang", "en"))) rbKhmer.setChecked(true);
        else rbEnglish.setChecked(true);
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(
                config, requireActivity().getResources().getDisplayMetrics());
    }
}
