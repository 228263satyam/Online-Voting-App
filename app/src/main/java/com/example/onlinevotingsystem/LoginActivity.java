package com.example.onlinevotingsystem;

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

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView;
    private Button Btn, btnECA, btnEC, backToRegisterBtn;
    private ProgressBar progressBar;
    public static String curUserType;

    private OkHttpClient client;
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private static final String PREF_NAME = "UserSession";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        client = new OkHttpClient();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        emailTextView = findViewById(R.id.emailLogin);
        passwordTextView = findViewById(R.id.password);
        Btn = findViewById(R.id.login);
        btnECA = findViewById(R.id.loginECA);
        progressBar = findViewById(R.id.progressBar);
        btnEC = findViewById(R.id.loginEC);
        backToRegisterBtn = findViewById(R.id.backToRegisterBtn);

        checkSession();

        Btn.setOnClickListener(v -> loginUserAccount("voter"));
        btnEC.setOnClickListener(v -> loginUserAccount("ec"));
        btnECA.setOnClickListener(v -> handleECALogin());
        backToRegisterBtn.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), RegisterActivity.class)));
    }

    private void checkSession() {
        String userType = sharedPreferences.getString("user_type", "");
        if (!userType.isEmpty()) {
            navigateToMainActivity(userType);
        }
    }

    private void loginUserAccount(final String type) {
        progressBar.setVisibility(View.VISIBLE);
        curUserType = type;

        String email = emailTextView.getText().toString();
        String password = passwordTextView.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please enter email and password!");
            progressBar.setVisibility(View.GONE);
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Login failed: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    String userId = responseObject.has("user") ? responseObject.getAsJsonObject("user").get("id").getAsString() : "";
                    saveUserSession(userId,email, type);
                    runOnUiThread(() -> {
                        showToast("Login successful!");
                        navigateToMainActivity(type);
                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("Login failed! Check your credentials.");
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void handleECALogin() {
        progressBar.setVisibility(View.VISIBLE);
        String email = emailTextView.getText().toString();
        String password = passwordTextView.getText().toString();

        if (!email.equals("eca@gov.in") || !password.equals("eca123")) {
            showToast("Invalid ECA credentials");
            progressBar.setVisibility(View.GONE);
            return;
        }

        saveUserSession("101",email, "eca");
        showToast("Successfully logged in");
        navigateToMainActivity("eca");
    }


    private void saveUserSession(String userId,String email, String type) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_email", email);
        editor.putString("user_type", type);
        editor.putString("userId",userId);
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }

    private void navigateToMainActivity(String type) {
        Intent intent;
        if (type.equals("ec")) {
            intent = new Intent(LoginActivity.this, ECInfoActivity.class);
        } else if (type.equals("eca")) {
            intent = new Intent(LoginActivity.this, ECAMainActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, InfoActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
