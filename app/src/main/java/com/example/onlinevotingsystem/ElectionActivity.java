package com.example.onlinevotingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ElectionActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    private Button chooseElectionCandidateBtn, goToAllElections;
    private String curElectionName,partyName;
    private TextView electionTxtView;
    private RecyclerView recyclerViewCandidates;
    private ElectionCandidateListAdapter adapter;
    private OkHttpClient client = new OkHttpClient();
    private List<String> candidateNames = new ArrayList<>();
    private String selectedCandidate = null;
    private String seletedCandidate2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_election);

        // Retrieve election name from Intent
        curElectionName = getIntent().getStringExtra("electionName");
        partyName="";
        // Initialize UI components
        chooseElectionCandidateBtn = findViewById(R.id.chooseElectionCandidateBtn);
        recyclerViewCandidates = findViewById(R.id.recyclerViewCandidates);
        electionTxtView = findViewById(R.id.electionTxtView);
        goToAllElections = findViewById(R.id.goToAllElections);
        seletedCandidate2="";

        // Set election name on screen
        electionTxtView.setText(curElectionName);

        // Navigate to All Elections page
        goToAllElections.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ShowAllCurrentElections.class)));

        // Set up RecyclerView
        recyclerViewCandidates.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ElectionCandidateListAdapter(candidateNames, this::onCandidateSelected);
        recyclerViewCandidates.setAdapter(adapter);

        // Fetch election candidates from Supabase
        fetchCandidates();

        // Handle vote submission
        chooseElectionCandidateBtn.setOnClickListener(v -> checkVoterVerification());
    }

    private void fetchCandidates() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?election_name=eq." + curElectionName + "&select=*")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to fetch candidates", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONArray candidatesArray = new JSONArray(response.body().string());
                    List<String> newCandidateNames = new ArrayList<>(); // Store new list

                    for (int i = 0; i < candidatesArray.length(); i++) {
                        JSONObject candidate = candidatesArray.getJSONObject(i);
                        boolean isVerified = candidate.getBoolean("is_verified");
                        String party = candidate.getString("party_name");
                        String fullName = candidate.getString("first_name") + " " + candidate.getString("last_name");

                        if (isVerified) {
                            newCandidateNames.add(party + " - " + fullName);
                        }
                    }

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        candidateNames.clear();
                        candidateNames.addAll(newCandidateNames);
                        adapter.notifyDataSetChanged();
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error parsing candidates", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void onCandidateSelected(String candidate) {
        selectedCandidate = candidate;
        String[] parts = candidate.split(" - ", 2); // Split into [party, candidateName]
        if (parts.length == 2) {
            partyName = parts[0];
            seletedCandidate2 = parts[1];
        }
    }


    private String getUserIdFromSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userId", "");
    }

    private void checkVoterVerification() {
        String userId = getUserIdFromSession();

        if (userId.isEmpty()) {
            Toast.makeText(getApplicationContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String checkUserUrl = SUPABASE_URL + "/rest/v1/user_data?user_id=eq." + userId + "&select=is_verified";

        Request checkUserRequest = new Request.Builder()
                .url(checkUserUrl)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(checkUserRequest).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "You are not Verified yet", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONArray userArray = new JSONArray(response.body().string());
                    if (userArray.length() > 0) {
                        boolean isVerified = userArray.getJSONObject(0).getBoolean("is_verified");

                        if (isVerified) {
                            checkIfVoted(userId);
                        } else {
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "You are not a verified voter!", Toast.LENGTH_LONG).show());
                        }
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "You are not a verified voter!", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void checkIfVoted(String userId) {
        String checkVoteUrl = SUPABASE_URL + "/rest/v1/election_votes?user_id=eq." + userId + "&election_name=eq." + curElectionName + "&select=*";

        Request checkVoteRequest = new Request.Builder()
                .url(checkVoteUrl)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(checkVoteRequest).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error checking vote status", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONArray voteArray = new JSONArray(responseBody);

                    if (voteArray.length() > 0) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "You have already voted in this election", Toast.LENGTH_SHORT).show());
                    } else {
                        submitVote(userId);
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error parsing response", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void submitVote(String userId) {
        if (selectedCandidate == null) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Please select a candidate!", Toast.LENGTH_LONG).show());
            return;
        }

        JSONObject voteData = new JSONObject();
        try {
            voteData.put("user_id", userId);
            voteData.put("party_name", partyName);  // Now correctly set
            voteData.put("election_name", curElectionName);
            voteData.put("candidate", seletedCandidate2);  // Now correctly set
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), voteData.toString());
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_votes")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Vote submitted successfully", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(getApplicationContext(), ShowAllCurrentElections.class));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to submit vote", Toast.LENGTH_LONG).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }


}
