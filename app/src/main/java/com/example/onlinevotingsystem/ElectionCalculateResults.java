package com.example.onlinevotingsystem;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ElectionCalculateResults extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    private LinearLayout showAllRunningElectionsToCalculate, linearLayout;
    private Button backToMainPage;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_elections);

        showAllRunningElectionsToCalculate = findViewById(R.id.showAllRunningElectionsToCalculate);
        linearLayout = findViewById(R.id.noOngoingElectionsToEndLayout);
        backToMainPage = findViewById(R.id.backToInfoPageFromMainECA);

        backToMainPage.setOnClickListener(v -> finish());
        fetchRunningElections();
    }

    private void fetchRunningElections() {
        String url = SUPABASE_URL + "/rest/v1/elections?status=eq.false&select=*";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    return;
                }
                try {
                    JSONArray electionsArray = new JSONArray(response.body().string());
                    runOnUiThread(() -> {
                        showAllRunningElectionsToCalculate.removeAllViews();
                        if (electionsArray.length() == 0) {
                            linearLayout.setVisibility(View.VISIBLE);
                        } else {
                            linearLayout.setVisibility(View.GONE);
                            for (int i = 0; i < electionsArray.length(); i++) {
                                try {
                                    JSONObject election = electionsArray.getJSONObject(i);
                                    String electionName = election.getString("election_name");
                                    Button electionBtn = new Button(getApplicationContext());
                                    electionBtn.setText(electionName);
                                    electionBtn.setOnClickListener(v -> calculateResults(election));
                                    showAllRunningElectionsToCalculate.addView(electionBtn);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void calculateResults(JSONObject election) {
        try {
            String electionName = election.getString("election_name");
            int electionId = election.getInt("id");
            String url = SUPABASE_URL + "/rest/v1/election_votes?election_name=eq." + electionName;

            Request request = new Request.Builder()
                    .url(url)
                    .header("apikey", SUPABASE_API_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        return;
                    }
                    try {
                        JSONArray votesArray = new JSONArray(response.body().string());
                        HashMap<String, HashMap<String, Integer>> results = new HashMap<>();

                        for (int i = 0; i < votesArray.length(); i++) {
                            JSONObject vote = votesArray.getJSONObject(i);
                            String partyName = vote.optString("party_name", "Independent");
                            String candidate = vote.getString("candidate");

                            // Ensure party exists in the map
                            results.putIfAbsent(partyName, new HashMap<>());

                            // Get the candidate map for this party
                            HashMap<String, Integer> candidateMap = results.get(partyName);

                            // Update the candidate's vote count
                            candidateMap.put(candidate, candidateMap.getOrDefault(candidate, 0) + 1);
                        }
                        saveResults(electionId, results);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    private void saveResults(int electionId, HashMap<String, HashMap<String, Integer>> results) {
        try {
            JSONArray resultsArray = new JSONArray();

            for (Map.Entry<String, HashMap<String, Integer>> partyEntry : results.entrySet()) {
                String partyName = partyEntry.getKey();
                HashMap<String, Integer> candidateMap = partyEntry.getValue();

                for (Map.Entry<String, Integer> candidateEntry : candidateMap.entrySet()) {
                    JSONObject resultObj = new JSONObject();
                    resultObj.put("election_id", electionId);
                    resultObj.put("party_name", partyName.equals("Independent") ? JSONObject.NULL : partyName); // Allow NULL for Independent candidates
                    resultObj.put("candidate_name", candidateEntry.getKey());
                    resultObj.put("vote_count", candidateEntry.getValue());

                    resultsArray.put(resultObj);
                }
            }

            String url = SUPABASE_URL + "/rest/v1/candidate_results";
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), resultsArray.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .header("apikey", SUPABASE_API_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d("onResponsesave: ", response.body().string());
                    if (response.isSuccessful()) {
                        markElectionAsDone(electionId);
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    private void markElectionAsDone(int electionId) {
        String url = SUPABASE_URL + "/rest/v1/elections?id=eq." + electionId;
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("status", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .patch(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponse: ",response.body().string());
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Election results calculated", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getApplicationContext(), ECAMainActivity.class));
                    finish();
                });

            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });
    }
}
