package com.example.onlinevotingsystem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InfoActivity extends AppCompatActivity {
    private Button goToPendingElectionsPage, goToEditPage, vGoToResultsPage, logout;
    private CardView cardView;
    private TextView messageText;

    // Supabase Credentials (Replace with your actual details)
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private static final String SUPABASE_TABLE = "user_data";

    private String userId; // Store user ID for requests
    private OkHttpClient client = new OkHttpClient(); // OkHttp Client

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        goToPendingElectionsPage = findViewById(R.id.goToPendingElectionsActivity);
        goToEditPage = findViewById(R.id.goToMainPage);
        vGoToResultsPage = findViewById(R.id.vGoToResultsPage);
        messageText = findViewById(R.id.messageTxt);
        logout = findViewById(R.id.logout);
        cardView = findViewById(R.id.reviewTextVoter);

        // Get user ID from saved session (replace with your own method)
        userId = getUserIdFromSession();
        if (userId == null) {
            startActivity(new Intent(InfoActivity.this, LoginActivity.class));
            finish();
            return;
        }

        fetchUserData();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        goToPendingElectionsPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ShowAllCurrentElections.class));
            }
        });

        goToEditPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        vGoToResultsPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ResultsActivity.class));
            }
        });
    }

    private void fetchUserData() {
        String url = SUPABASE_URL + "/rest/v1/" + SUPABASE_TABLE + "?user_id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    messageText.setText("Error fetching data.");
                    Toast.makeText(InfoActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Log.d("onResponse: ",response.body().string());
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> messageText.setText("Failed to load profile data."));
                    return;
                }

                try {
                    JSONArray jsonArray = new JSONArray(response.body().string());
                    if (jsonArray.length() > 0) {
                        JSONObject userData = jsonArray.getJSONObject(0);
                        final Boolean isVerified; // Declare as final

                        if (userData.has("is_verified") && !userData.isNull("is_verified")) {
                            isVerified = userData.optBoolean("is_verified"); // Will be true or false if present
                        } else {
                            isVerified = null; // Explicitly set null if not found
                        }

                        final String message = userData.optString("message", "");

                        runOnUiThread(() -> {
                            if (isVerified == null) {
                                messageText.setText("Verification status is pending. " + message);
                                cardView.setCardBackgroundColor(Color.YELLOW);
                                messageText.setTextColor(Color.BLACK);
                            } else if (isVerified) {
                                messageText.setText("Profile verified. Authorized for Voting.");
                                cardView.setCardBackgroundColor(Color.GREEN);
                                messageText.setTextColor(Color.BLACK);
                            } else {
                                messageText.setText("You are not authorized to enroll in elections.");
                                cardView.setCardBackgroundColor(Color.RED);
                                messageText.setTextColor(Color.WHITE);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> messageText.setText("Error parsing data."));
                }
            }
        });
    }

    private void logoutUser() {
        // Clear session storage (implement your own method)
        clearUserSession();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    private String getUserIdFromSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String id=sharedPreferences.getString("userId", "");
        return id;
    }

    private void clearUserSession() {
        SharedPreferences.Editor editor = getSharedPreferences("UserSession", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }
}
