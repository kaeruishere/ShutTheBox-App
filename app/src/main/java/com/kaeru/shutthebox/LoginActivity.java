package com.kaeru.shutthebox;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaeru.shutthebox.databinding.ActivityLoginBinding;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "FirebaseAuth";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
        }

        // Google Sign-In Ayarları
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        setupListeners();
    }

    private void setupListeners() {
        binding.rRegisterButton.setOnClickListener(view -> {
            String fullname = binding.rFullName.getText().toString().trim();
            String username = binding.rUsername.getText().toString().trim();
            String email = binding.rEmail.getText().toString().trim();
            String password = binding.rPassword.getText().toString().trim();
            String passwordConfirm = binding.rPasswordConfirm.getText().toString().trim();

            if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showToast("Please fill all fields");
                return;
            }

            if (!password.equals(passwordConfirm)) {
                showToast("Passwords do not match");
                return;
            }

            registerWithEmail(fullname, username, email, password);
        });

        binding.sSignInButton.setOnClickListener(view -> {
            String email = binding.sUsername.getText().toString().trim(); // Note: Firebase Auth uses email
            String password = binding.sPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password");
            } else {
                signInWithEmail(email, password);
            }
        });

        binding.gUsernameSaveButton.setOnClickListener(view -> {
            String googleUsername = binding.gUsername.getText().toString().trim();
            if (!googleUsername.isEmpty()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveUserToFirestore(user.getUid(), googleUsername, user.getEmail(), user.getDisplayName());
                }
            } else {
                showToast("Please enter a username");
            }
        });

        binding.btnGoogleSignIn.setOnClickListener(view -> signInWithGoogle());

        binding.rToS.setOnClickListener(view -> switchLayout(false));
        binding.sToR.setOnClickListener(view -> switchLayout(true));
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                navigateToMain();
            } else {
                showToast("Sign-In Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    private void registerWithEmail(String fullname, String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveUserToFirestore(user.getUid(), username, email, fullname);
                }
            } else {
                showToast("Registration Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    db.collection("users").document(user.getUid()).get()
                            .addOnCompleteListener(documentTask -> {
                                if (documentTask.isSuccessful() && documentTask.getResult() != null) {
                                    if (documentTask.getResult().exists()) {
                                        navigateToMain();
                                    } else {
                                        binding.GLayout.setVisibility(View.VISIBLE);
                                        binding.btnGoogleSignIn.setVisibility(View.GONE);
                                        binding.RegisterLayout.setVisibility(View.GONE);
                                        binding.SignInLayout.setVisibility(View.GONE);
                                    }
                                }
                            });
                }
            } else {
                showToast("Google Sign-In Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    private void saveUserToFirestore(String userId, String username, String email, String displayName) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("displayName", displayName);
        user.put("registrationDate", System.currentTimeMillis());

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> navigateToMain())
                .addOnFailureListener(e -> showToast("Error Saving User: " + e.getMessage()));
    }

    private void switchLayout(boolean toRegister) {
        binding.RegisterLayout.setVisibility(toRegister ? View.VISIBLE : View.GONE);
        binding.SignInLayout.setVisibility(toRegister ? View.GONE : View.VISIBLE);
        binding.GLayout.setVisibility(View.GONE);
    }

    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
