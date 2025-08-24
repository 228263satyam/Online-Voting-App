package com.example.onlinevotingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MakeElectionActivity extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    private EditText electionName;
    private Button makeElectionBtn, backToMainPage;
    private DatePicker startElectionDate, endElectionDate;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_election);

        electionName = findViewById(R.id.electionName);
        makeElectionBtn = findViewById(R.id.makeElectionBtn);
        startElectionDate = findViewById(R.id.startDatePicker);
        endElectionDate = findViewById(R.id.endDatePicker);
        backToMainPage = findViewById(R.id.backToInfoPageFromCreateElectionECA);

        backToMainPage.setOnClickListener(v -> {

            finish();
        });

        makeElectionBtn.setOnClickListener(v -> createElection());
    }

    private void createElection() {
        int startDay = startElectionDate.getDayOfMonth();
        int startMonth = startElectionDate.getMonth() + 1;
        int startYear = startElectionDate.getYear();
        String startElectionDateFormatted = startYear + "-" + startMonth + "-" + startDay;

        int endDay = endElectionDate.getDayOfMonth();
        int endMonth = endElectionDate.getMonth() + 1;
        int endYear = endElectionDate.getYear();
        String endElectionDateFormatted = endYear + "-" + endMonth + "-" + endDay;

        String electionNameStr = electionName.getText().toString();

        if (electionNameStr.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter an election name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> electionData = new HashMap<>();
        electionData.put("election_name", electionNameStr);
        electionData.put("start_date", startElectionDateFormatted);
        electionData.put("end_date", endElectionDateFormatted);
        electionData.put("winner", "");
        electionData.put("status", "false");

        RequestBody requestBody = RequestBody.create(new JSONObject(electionData).toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/elections")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Election added successfully!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(getApplicationContext(), ECAMainActivity.class));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to create election!", Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}
