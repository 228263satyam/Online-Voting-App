package com.example.onlinevotingsystem;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShowAllCurrentElections extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    private LinearLayout showAllRunningElections, noElectionsLayout;
    private Button goBackToInfoPageFromAllElections;
    private OkHttpClient client = new OkHttpClient();
    private String userId = "user-123";  // Replace with actual user ID from authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_elections);

        showAllRunningElections = findViewById(R.id.showAllRunningElections);
        noElectionsLayout = findViewById(R.id.noOngoingElectionsLayout);
        goBackToInfoPageFromAllElections = findViewById(R.id.backToInfoPageFromAllElections);

        goBackToInfoPageFromAllElections.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), InfoActivity.class)));

        fetchElections();
    }

    private void fetchElections() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/elections?select=*")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to fetch elections", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONArray elections = new JSONArray(response.body().string());
                    runOnUiThread(() -> showElections(elections));
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error parsing data", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void showElections(JSONArray elections) {
        showAllRunningElections.removeAllViews();
        int numElections = 0;

        for (int i = 0; i < elections.length(); i++) {
            try {
                JSONObject election = elections.getJSONObject(i);
                String electionName = election.getString("election_name");
                boolean isDone = election.getBoolean("status");

                // Check if user already voted (mock logic for now)
                boolean userHasVoted = false; // Replace with actual logic to check user votes

                if (!userHasVoted && !isDone) {
                    numElections++;

                    Button curElectionBtn = new Button(getApplicationContext());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        curElectionBtn.setElevation(2);
                    }
                    curElectionBtn.setId(View.generateViewId());
                    curElectionBtn.setText(electionName);

                    curElectionBtn.setOnClickListener(v -> {
                        Intent intent = new Intent(getApplicationContext(), ElectionActivity.class);
                        intent.putExtra("electionName", electionName);
                        startActivity(intent);
                    });

                    showAllRunningElections.addView(curElectionBtn);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (numElections == 0) {
            noElectionsLayout.setVisibility(View.VISIBLE);
        } else {
            noElectionsLayout.setVisibility(View.GONE);
        }
    }
}
