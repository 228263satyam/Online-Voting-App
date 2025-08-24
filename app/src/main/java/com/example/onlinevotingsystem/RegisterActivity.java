package com.example.onlinevotingsystem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";  // Use service role key for DB writes
    private static final String AUTH_ENDPOINT = "/auth/v1/signup";
    private static final String USERS_TABLE_ENDPOINT = "/rest/v1/users";

    private EditText emailTextView, passwordTextView;
    private Button Btn, btnECRegister, goToLoginPage;
    private ProgressBar progressBar;
    private OkHttpClient client;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "UserSession";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        emailTextView = findViewById(R.id.email);
        passwordTextView = findViewById(R.id.passwd);
        Btn = findViewById(R.id.btnregister);
        progressBar = findViewById(R.id.progressbar);
        goToLoginPage = findViewById(R.id.goToLoginPage);
        btnECRegister = findViewById(R.id.btnECRegister);
        client = new OkHttpClient();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Set onClick listeners
        Btn.setOnClickListener(v -> registerNewUser("voter"));
        btnECRegister.setOnClickListener(v -> registerNewUser("ec"));
        goToLoginPage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), LoginActivity.class)));
    }

    private void registerNewUser(String userType) {
        progressBar.setVisibility(View.VISIBLE);

        String email = emailTextView.getText().toString();
        String password = passwordTextView.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || password.length() < 6) {
            showToast("Please enter a valid email and password (min 6 characters).");
            progressBar.setVisibility(View.GONE);
            return;
        }

        JSONObject jsonAuth = new JSONObject();
        try {
            jsonAuth.put("email", email);
            jsonAuth.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody authBody = RequestBody.create(jsonAuth.toString(), MediaType.get("application/json; charset=utf-8"));

        Request authRequest = new Request.Builder()
                .url(SUPABASE_URL + AUTH_ENDPOINT)
                .post(authBody)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(authRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Registration failed! Please try again.");
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String userId = jsonResponse.getString("id");  // Get user UUID from Supabase Auth

                        insertUserIntoDatabase(userId, email, password, userType);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showToast("Error parsing authentication response"));
                    }
                } else {
                    runOnUiThread(() -> {
                        showToast("Authentication failed! Email might be taken.");
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void insertUserIntoDatabase(String userId, String email, String password, String userType) {
        JSONObject jsonUser = new JSONObject();
        try {
            jsonUser.put("id", userId);
            jsonUser.put("email", email);
            jsonUser.put("password_hash", password);  // Ideally, store hashed password
            jsonUser.put("user_type", userType);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody userBody = RequestBody.create(jsonUser.toString(), MediaType.get("application/json; charset=utf-8"));

        Request userRequest = new Request.Builder()
                .url(SUPABASE_URL + USERS_TABLE_ENDPOINT)
                .post(userBody)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)  // Service role key needed for DB writes
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(userRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Database insertion failed!");
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("onResponse: ",response.body().string());
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showToast("User registered successfully!");
                        progressBar.setVisibility(View.GONE);
                        navigateToNextActivity(userType);
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("Error inserting into database");
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void navigateToNextActivity(String userType) {
        Intent intent;
        if ("voter".equals(userType)) {
            intent = new Intent(RegisterActivity.this, LoginActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
