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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Recipe.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignup;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in BEFORE setting content view
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, going to MainActivity");
            // User is already logged in, go directly to MainActivity
            navigateToMain();
            return; // Don't continue with onCreate
        }

        // User not logged in, show login screen
        setContentView(R.layout.activity_login);

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://recipe-2f48e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        usersRef = database.getReference("users");

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        Log.d(TAG, "Attempting login for: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Login successful for UID: " + user.getUid());
                            ensureUserProfileExists(user);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.e(TAG, "Login failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void ensureUserProfileExists(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        Log.d(TAG, "Checking if user profile exists for: " + userId);

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                        Log.d(TAG, "User profile exists: " + user.getName());
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.d(TAG, "User profile exists but name is null/empty, fixing...");
                        createOrUpdateUserProfile(firebaseUser);
                    }
                } else {
                    Log.d(TAG, "User profile doesn't exist, creating...");
                    createOrUpdateUserProfile(firebaseUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createOrUpdateUserProfile(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();

        // If display name is null, try to extract from email
        if (name == null || name.isEmpty()) {
            if (email != null && email.contains("@")) {
                name = email.split("@")[0];
                Log.d(TAG, "Using email prefix as name: " + name);
            } else {
                name = "User";
                Log.d(TAG, "Using default name: User");
            }
        }

        Log.d(TAG, "Creating/updating user profile - Name: " + name + ", Email: " + email);

        User user = new User(userId, name, email != null ? email : "");

        usersRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created/updated successfully");
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create/update user profile", e);
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Profile update failed, but logging in anyway",
                            Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}