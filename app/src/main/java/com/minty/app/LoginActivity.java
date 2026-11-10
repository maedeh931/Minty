package com.minty.app;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.minty.app.databinding.ActivityLoginBinding;

/**
 * LoginActivity serves as the launcher entry point for user authentication.
 * Supports email/password login, registration, password reset, and Google Sign-In.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: activity is being created");

        // Inflate layout using View Binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // ActivityResultLauncher for Google Sign-In intent
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            } else {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Google Sign-In failed: no token received", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ApiException e) {
                            setLoading(false);
                            Log.w(TAG, "Google sign in failed, code=" + e.getStatusCode(), e);
                            Toast.makeText(LoginActivity.this, "Google Sign-In failed (" + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                    }
                }
        );

        // Login button
        binding.btnLogin.setOnClickListener(v -> {
            String email = getEmailInput();
            String password = getPasswordInput();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String savedName = user.getDisplayName();
                                if (TextUtils.isEmpty(savedName)) {
                                    savedName = deriveFriendlyName(user.getEmail());
                                }
                                getSharedPreferences("minty_user_profile", MODE_PRIVATE)
                                        .edit()
                                        .putString("key_display_name", savedName)
                                        .apply();
                            }
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            handleAuthError(task.getException(), "Login failed");
                        }
                    });
        });

        // Register button
        binding.btnRegister.setOnClickListener(v -> {
            String email = getEmailInput();
            String password = getPasswordInput();
            String nameInput = binding.etDisplayName.getText() != null
                    ? binding.etDisplayName.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String finalName = !TextUtils.isEmpty(nameInput) ? nameInput : deriveFriendlyName(email);
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(finalName)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }
                            getSharedPreferences("minty_user_profile", MODE_PRIVATE)
                                    .edit()
                                    .putString("key_display_name", finalName)
                                    .apply();
                            FirestoreManager.getInstance().saveUserProfile(finalName, email);
                            Log.d(TAG, "createUserWithEmail:success name=" + finalName);
                            Toast.makeText(LoginActivity.this, "Welcome to Minty, " + finalName + "!", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            handleAuthError(task.getException(), "Registration failed");
                        }
                    });
        });

        // Forgot password text button
        binding.btnForgotPassword.setOnClickListener(v -> {
            String email = getEmailInput();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(this, task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "sendPasswordResetEmail:success");
                            Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                            handleAuthError(task.getException(), "Failed to send password reset email");
                        }
                    });
        });

        // Sign in with Google button
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            Log.d(TAG, "btnGoogleSignIn clicked: starting Google Sign-In flow");
            setLoading(true);
            googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: activity is becoming visible");

        // If user is already signed in, go straight to MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "onStart: user already signed in: " + currentUser.getEmail());
            navigateToMainActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: activity is interacting with the user");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: activity is restarting after being stopped");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: activity is pausing");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: activity is no longer visible");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity is being destroyed");
    }

    /**
     * Authenticates with Firebase using a Google AuthCredential.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: authenticating with Google ID token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String name = user.getDisplayName();
                            if (TextUtils.isEmpty(name)) {
                                name = deriveFriendlyName(user.getEmail());
                            }
                            getSharedPreferences("minty_user_profile", MODE_PRIVATE)
                                    .edit()
                                    .putString("key_display_name", name)
                                    .apply();
                            FirestoreManager.getInstance().saveUserProfile(name, user.getEmail());
                        }
                        Log.d(TAG, "signInWithCredential:success");
                        Toast.makeText(LoginActivity.this, "Signed in with Google!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        handleAuthError(task.getException(), "Google authentication failed");
                    }
                });
    }

    /**
     * Derives a clean, polite display name from an email address (e.g. "john.smith@domain.com" -> "John Smith").
     */
    public static String deriveFriendlyName(String email) {
        if (TextUtils.isEmpty(email)) {
            return "Minty Friend";
        }
        int atIndex = email.indexOf('@');
        String base = atIndex > 0 ? email.substring(0, atIndex) : email;
        base = base.replaceAll("[._0-9]+", " ").trim();
        if (base.isEmpty()) {
            return "Minty Friend";
        }
        String[] words = base.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "Minty Friend" : result;
    }

    /**
     * Navigates to MainActivity and terminates LoginActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Handles common Firebase authentication errors and presents clear user-facing messages.
     */
    private void handleAuthError(Exception e, String defaultMessage) {
        String message;
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            message = "Invalid email or wrong password.";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            message = "No account found with this email.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            message = "An account already exists with this email.";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            message = "Password is too weak. Please use at least 6 characters.";
        } else if (e instanceof FirebaseNetworkException) {
            message = "No internet connection. Please check your network.";
        } else if (e != null && e.getMessage() != null) {
            message = e.getMessage();
        } else {
            message = defaultMessage;
        }
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Toggles the loading progress indicator and action buttons.
     */
    private void setLoading(boolean loading) {
        binding.pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnRegister.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
        binding.btnForgotPassword.setEnabled(!loading);
    }

    private String getEmailInput() {
        return binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
    }

    private String getPasswordInput() {
        return binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
    }
}
