package com.example.onlinevotingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class ECACandidateReviewActivity extends AppCompatActivity {
    private List<String> candidatesId = new ArrayList<>();
    private int idx = 0;
    private TextView firstName, lastName, aadharNum, gender, dob, partyName,electionName;
    private LinearLayout upparLayout, done;
    private EditText message;
    private Button cAccept, cDecline, backToMain, backToMain2;
    private ImageView cAadharImg;
    private final OkHttpClient client = new OkHttpClient();
    private final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eca_ec_review);

        backToMain = findViewById(R.id.backToMainFromReviewCandidate);
        backToMain2 = findViewById(R.id.backToMainFromReviewCandidate2);
        backToMain.setOnClickListener(v -> finish());
        backToMain2.setOnClickListener(v -> finish());

        cAccept = findViewById(R.id.cAccept);
        cDecline = findViewById(R.id.cDecline);
        firstName = findViewById(R.id.cFirstName);
        lastName = findViewById(R.id.cLastName);
        aadharNum = findViewById(R.id.cAadharNum);
        gender = findViewById(R.id.cGender);
        dob = findViewById(R.id.cDob);
        message = findViewById(R.id.cReviewMessage);
        cAadharImg = findViewById(R.id.cAadharImg);
        upparLayout = findViewById(R.id.upperLayout);
        done = findViewById(R.id.doneLayout);
        partyName = findViewById(R.id.cProfileName);
        electionName=findViewById(R.id.cElectionName);
        fetchCandidates();

        cAccept.setOnClickListener(v -> verifyCandidate(true));
        cDecline.setOnClickListener(v -> verifyCandidate(false));
    }

    private void fetchCandidates() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?is_verified=eq.false")
                .addHeader("apikey", SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responsebody=response.body().string();
                Log.d("onResponse: ",responsebody);
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(responsebody);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            candidatesId.add(jsonArray.getJSONObject(i).getString("id"));
                        }
                        runOnUiThread(() -> {
                            if (!candidatesId.isEmpty()) updateUser(candidatesId.get(idx));
                            else done();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void updateUser(String uid) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?id=eq." + uid)
                .addHeader("apikey", SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        JSONObject candidate = jsonArray.getJSONObject(0);

                        // Extract data
                        String imageUrl = candidate.optString("file_url"); // Ensure field name matches DB
                        String firstNameStr = candidate.optString("first_name");
                        String lastNameStr = candidate.optString("last_name");
                        String aadharNumStr = candidate.optString("aadhar_number");
                        String genderStr = candidate.optString("gender");
                        String dobStr = candidate.optString("dob");
                        String partyNameStr = candidate.optString("party_name");
                        String eName=candidate.optString("election_name");

                        runOnUiThread(() -> {
                            // Set text fields
                            firstName.setText(firstNameStr);
                            lastName.setText(lastNameStr);
                            aadharNum.setText(aadharNumStr);
                            gender.setText(genderStr);
                            dob.setText(dobStr);
                            partyName.setText(partyNameStr);
                            electionName.setText(eName);

                            // Load the image
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                loadAadharImage(imageUrl);
                            } else {
                                cAadharImg.setImageResource(R.drawable.indiavotefinal); // Default image if missing
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void verifyCandidate(boolean isAccepted) {
        String candidateId = candidatesId.get(idx);
        JSONObject payload = new JSONObject();
        try {

            if (!isAccepted) {
                payload.put("is_verified", JSONObject.NULL);
                payload.put("message", message.getText().toString());
            }else{
                payload.put("is_verified", isAccepted);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), payload.toString());
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?id=eq." + candidateId)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation") // Important for updates
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("onResponse: ", responseBody);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        idx++;
                        if (idx < candidatesId.size()) updateUser(candidatesId.get(idx));
                        else done();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to update", Toast.LENGTH_LONG).show());
                }
            }
        });
    }
    private void loadAadharImage(String imageUrl) {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to load image", Toast.LENGTH_LONG).show();
                    Log.e("AadharImageError", "Failed to load image", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error fetching image", Toast.LENGTH_LONG).show());
                    return;
                }

                byte[] imageBytes = response.body().bytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                runOnUiThread(() -> cAadharImg.setImageBitmap(bitmap));
            }
        });
    }


    void done() {
        done.setVisibility(View.VISIBLE);
        upparLayout.setVisibility(View.GONE);
    }
}
