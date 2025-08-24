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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ECAVoterReviewActivity extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private ArrayList<String> voterIds = new ArrayList<>();
    private int idx = 0;

    private TextView firstName, lastName, aadharNum, gender, dob;
    private EditText message;
    private Button vAccept, vDecline, backToMain, backToMain2;
    private ImageView vAadharImg;
    private LinearLayout upperLayout, doneBtn;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eca_voter_review);

        vAccept = findViewById(R.id.vAccept);
        vDecline = findViewById(R.id.vDecline);
        firstName = findViewById(R.id.vFirstName);
        lastName = findViewById(R.id.vLastName);
        aadharNum = findViewById(R.id.vAadharNum);
        gender = findViewById(R.id.vGender);
        dob = findViewById(R.id.vDob);
        message = findViewById(R.id.vReviewMessage);
        vAadharImg = findViewById(R.id.vAadharImg);
        upperLayout = findViewById(R.id.upperLayoutVoter);
        doneBtn = findViewById(R.id.doneLayout);
        backToMain = findViewById(R.id.backToMainFromReviewVoter);
        backToMain2 = findViewById(R.id.backToMainFromReviewVoter2);

        backToMain.setOnClickListener(v -> {
//            startActivity(new Intent(getApplicationContext(), ECAMainActivity.class));
            finish();
        });
        backToMain2.setOnClickListener(v -> {
//            startActivity(new Intent(getApplicationContext(), ECAMainActivity.class));
            finish();
        });

        fetchUnverifiedVoters();

        vAccept.setOnClickListener(v -> verifyVoter(true));
        vDecline.setOnClickListener(v -> verifyVoter(false));
    }

    private void fetchUnverifiedVoters() {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/user_data?is_verified=eq.false")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error fetching voters", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            voterIds.add(jsonArray.getJSONObject(i).getString("id"));
                        }
                        runOnUiThread(() -> {
                            if (!voterIds.isEmpty()) updateUser(voterIds.get(idx));
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
                .url(SUPABASE_URL + "/rest/v1/user_data?id=eq." + uid)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject voter = new JSONArray(response.body().string()).getJSONObject(0);
                        String img=voter.optString("image_url");
                        runOnUiThread(() -> {
                            firstName.setText(voter.optString("first_name"));
                            lastName.setText(voter.optString("last_name"));
                            aadharNum.setText(voter.optString("aadhar_number"));
                            gender.setText(voter.optString("gender"));
                            dob.setText(voter.optString("dob"));
                            loadAadharImage(img);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void loadAadharImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Invalid Image URL", Toast.LENGTH_LONG).show());
            return;
        }

        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to load image", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error fetching image", Toast.LENGTH_LONG).show());
                    return;
                }

                byte[] imageBytes = response.body().bytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                runOnUiThread(() -> vAadharImg.setImageBitmap(bitmap));
            }
        });
    }


    private void verifyVoter(boolean accept) {
        String uid = voterIds.get(idx);
        JSONObject json = new JSONObject();
        try {

            if(!accept){
                json.put("is_verified", JSONObject.NULL);
                json.put("message", message.getText().toString());
            }else{
                json.put("is_verified", accept);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/user_data?id=eq." + uid)
                .patch(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    idx++;
                    if (idx < voterIds.size()) updateUser(voterIds.get(idx));
                    else done();
                });
            }
        });
    }

    private void done() {
        doneBtn.setVisibility(View.VISIBLE);
        upperLayout.setVisibility(View.GONE);
    }
}
