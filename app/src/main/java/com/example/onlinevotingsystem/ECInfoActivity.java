package com.example.onlinevotingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ECInfoActivity extends AppCompatActivity {
    private Button goToEditPage, goToResultsPage, logout;
    private TextView messageText;
    private CardView cardView;
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private static String USER_ID; // Replace with actual user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ec_info);

        goToEditPage = findViewById(R.id.goToMainPage);
        goToResultsPage = findViewById(R.id.goToResultsPage);
        messageText = findViewById(R.id.messageTxt);
        cardView = findViewById(R.id.reviewTextCandidate);
        logout = findViewById(R.id.logoutCandidate);
        USER_ID=getUserIdFromSession();
        fetchCandidateStatus();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        goToEditPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ECMainActivity.class));
            }
        });

        goToResultsPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ResultsActivity.class));
            }
        });
    }

    private void fetchCandidateStatus() {
        OkHttpClient client = new OkHttpClient();

        // Supabase request to fetch candidate data
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?user_id=eq." + USER_ID)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Supabase Error", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseBody); // Parse response as JSON array

                        if (jsonArray.length() > 0) { // Ensure there is at least one object
                            JSONObject jsonObject = jsonArray.getJSONObject(0); // Get first object

                            final Boolean isVerified; // Declare as final

                            if (jsonObject.has("is_verified") && !jsonObject.isNull("is_verified")) {
                                isVerified = jsonObject.optBoolean("is_verified"); // Will be true or false if present
                            } else {
                                isVerified = null; // Explicitly set null if not found
                            }

                            String message = jsonObject.optString("message", "");

                            runOnUiThread(() -> updateUI(isVerified, message));
                        } else {
                            Log.e("Supabase JSON Error", "No data found in response.");
                        }
                    } catch (Exception e) {
                        Log.e("Supabase JSON Error", "Parsing error: " + e.getMessage());
                    }
                } else {
                    Log.e("Supabase Error", "Response unsuccessful: " + response.code());
                }
            }

        });
    }

    private void updateUI(Boolean isVerified, String message) {

        if (isVerified == null) {
            messageText.setText("Verification status is pending. " + message);
            cardView.setCardBackgroundColor(Color.YELLOW);
            messageText.setTextColor(Color.BLACK);
        } else if (isVerified) {
            messageText.setText("Profile verified. Authorized for enrolling in elections.");
            cardView.setCardBackgroundColor(Color.GREEN);
            messageText.setTextColor(Color.BLACK);
        } else {
            messageText.setText("You are not authorized to enroll in elections.");
            cardView.setCardBackgroundColor(Color.RED);
            messageText.setTextColor(Color.WHITE);
        }
    }

    private String getUserIdFromSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String id=sharedPreferences.getString("userId", "");
        return id;
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("UserSession", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }
}
