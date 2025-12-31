package com.example.Recipe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Recipe.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        databaseReference = database.getReference("users");

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        btnSignup.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(false);

        Log.d(TAG, "Creating account with email: " + email + ", name: " + name);

        // Create user account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Log.d(TAG, "User created with UID: " + userId);

                            // Update display name in Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Display name updated in Auth");
                                        // Save user to database
                                        saveUserToDatabase(userId, name, email);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to update Auth profile", e);
                                        // Still try to save to database even if Auth update fails
                                        saveUserToDatabase(userId, name, email);
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnSignup.setEnabled(true);
                        Log.e(TAG, "Registration failed", task.getException());
                        Toast.makeText(SignupActivity.this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email) {
        Log.d(TAG, "Saving user to database - UID: " + userId + ", Name: " + name + ", Email: " + email);

        User user = new User(userId, name, email);

        databaseReference.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Log.d(TAG, "User successfully saved to database");
                    Toast.makeText(SignupActivity.this, "Account created successfully!",
                            Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    Log.e(TAG, "Failed to save user to database", e);
                    Toast.makeText(SignupActivity.this,
                            "Account created but failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Still navigate to main even if database save fails
                    navigateToMain();
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}