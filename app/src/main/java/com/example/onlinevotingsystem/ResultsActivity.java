package com.example.onlinevotingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ResultsActivity extends AppCompatActivity {
    private LinearLayout showAllDoneElections;
    private Button goBackToInfoPageResults;
    private ArrayList<String> candidates = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co/rest/v1/";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        showAllDoneElections = findViewById(R.id.allDoneElections);
        goBackToInfoPageResults = findViewById(R.id.backToInfoPageFromResults);
        goBackToInfoPageResults.setOnClickListener(v -> {
            String userType = LoginActivity.curUserType;
            if ("ec".equals(userType)) {
                startActivity(new Intent(getApplicationContext(), ECInfoActivity.class));
                finish();
            } else if ("voter".equals(userType)) {
                startActivity(new Intent(getApplicationContext(), InfoActivity.class));
                finish();
            } else {
//                startActivity(new Intent(getApplicationContext(), ECAMainActivity.class));
                finish();
            }
        });

        fetchElections();
    }



    private void fetchElections() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "elections?status=eq.true") // Fetch only completed elections
                .addHeader("apikey", SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to fetch elections", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());

                        // ✅ Call processElectionData() here to display results
                        runOnUiThread(() -> processElectionData(jsonArray));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }




    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void processElectionData(JSONArray elections) {
        for (int i = 0; i < elections.length(); i++) {
            try {
                JSONObject election = elections.getJSONObject(i);
                int electionId = election.getInt("id");
                String electionName = election.getString("election_name");

                // Card styling
                CardView newCard = new CardView(getApplicationContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(20, 20, 20, 20);
                newCard.setLayoutParams(cardParams);
                newCard.setRadius(20);
                newCard.setCardElevation(10);
                newCard.setPadding(20, 20, 20, 20);
                newCard.setCardBackgroundColor(Color.WHITE);

                LinearLayout newLinearLayout = new LinearLayout(getApplicationContext());
                newLinearLayout.setOrientation(LinearLayout.VERTICAL);
                newLinearLayout.setPadding(20, 20, 20, 20);

                // Election Name Styling
                TextView electionNameTxt = new TextView(getApplicationContext());
                electionNameTxt.setText(electionName);
                electionNameTxt.setTextSize(24);
                electionNameTxt.setTypeface(null, Typeface.BOLD);
                electionNameTxt.setGravity(Gravity.CENTER);
                electionNameTxt.setPadding(10, 10, 10, 20);
                newLinearLayout.addView(electionNameTxt);

                // Table Layout
                TableLayout tableLayout = new TableLayout(getApplicationContext());
                tableLayout.setStretchAllColumns(true);
                tableLayout.setBackgroundColor(Color.LTGRAY);
                tableLayout.setPadding(10, 10, 10, 10);

                // Header Row
                TableRow tableRowHeader = new TableRow(getApplicationContext());
                tableRowHeader.setBackgroundColor(Color.parseColor("#0079D6"));

                String[] headers = {"Candidate", "Party", "Votes"};
                for (String header : headers) {
                    TextView textView = new TextView(getApplicationContext());
                    textView.setText(header);
                    textView.setTextColor(Color.WHITE);
                    textView.setTypeface(null, Typeface.BOLD);
                    textView.setPadding(20, 20, 20, 20);
                    textView.setGravity(Gravity.CENTER);
                    tableRowHeader.addView(textView);
                }
                tableLayout.addView(tableRowHeader);

                fetchCandidateResults(electionId, tableLayout, newLinearLayout, newCard);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchCandidateResults(int electionId, TableLayout tableLayout, LinearLayout newLinearLayout, CardView newCard) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "candidate_results?election_id=eq." + electionId)
                .addHeader("apikey", SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to fetch candidate results", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());

                        runOnUiThread(() -> {
                            try {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject candidateObj = jsonArray.getJSONObject(i);
                                    String candidateName = candidateObj.getString("candidate_name");
                                    String partyName = candidateObj.getString("party_name");
                                    int voteCount = candidateObj.getInt("vote_count");

                                    TableRow tableRow = new TableRow(getApplicationContext());
                                    if (i % 2 == 0) {
                                        tableRow.setBackgroundColor(Color.parseColor("#E3F2FD")); // Alternate row color
                                    }

                                    String[] rowData = {candidateName, partyName, String.valueOf(voteCount)};
                                    for (String data : rowData) {
                                        TextView textView = new TextView(getApplicationContext());
                                        textView.setText(data);
                                        textView.setPadding(20, 20, 20, 20);
                                        textView.setGravity(Gravity.CENTER);
                                        textView.setTextSize(16);
                                        tableRow.addView(textView);
                                    }

                                    tableLayout.addView(tableRow);
                                }

                                newLinearLayout.addView(tableLayout);
                                newCard.addView(newLinearLayout);
                                showAllDoneElections.addView(newCard);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }




}
