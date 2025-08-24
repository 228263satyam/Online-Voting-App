package com.example.onlinevotingsystem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ECMainActivity extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private static final String SUPABASE_STORAGE_BUCKET = "user_uploads";

    private Button submitBtn, chooseFileBtn, goBackToInfoPage;
    private EditText firstName, lastName, aadharNumber, partyName;
    private RadioButton genderMale, genderFemale, genderOthers;
    private RadioGroup genderGrp;
    private DatePicker datePicker;
    private LinearLayout cardView;
    private SharedPreferences sharedPreferences;
    private static final int REQUEST_CODE_FILES = 1;
    private Uri fileUri;
    private String userId,name;
    private Spinner electionSpinner;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ec_main);

        submitBtn = findViewById(R.id.submitBtn);
        goBackToInfoPage = findViewById(R.id.goBackToInfoPage);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        partyName = findViewById(R.id.partyName);
        aadharNumber = findViewById(R.id.aadharNumber);
        genderMale = findViewById(R.id.male);
        genderFemale = findViewById(R.id.female);
        genderOthers = findViewById(R.id.others);
        datePicker = findViewById(R.id.datePicker);
        chooseFileBtn = findViewById(R.id.chooseFileBtn);
        genderGrp = findViewById(R.id.genderGrp);
        cardView = findViewById(R.id.uploadEC);
        electionSpinner = findViewById(R.id.electionSpinner);
        fetchElectionNames();
        userId=getUserIdFromSession();
        name="";
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        fetchCandidateData();
        chooseFileBtn.setOnClickListener(v -> selectFile());
        submitBtn.setOnClickListener(v -> submitData());
        goBackToInfoPage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ECInfoActivity.class)));
    }

    private void fetchElectionNames() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/elections?select=election_name")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ECMainActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<String> electionList = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            electionList.add(jsonObject.getString("election_name"));
                        }

                        runOnUiThread(() -> populateSpinner(electionList));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    // Validate name: Only letters, no numbers or special characters
    private boolean isValidName(String name) {
        return name.matches("^[a-zA-Z]+$");
    }

    // Validate Aadhar number: Exactly 12 digits
    private boolean isValidAadhar(String aadhar) {
        return aadhar.matches("^[0-9]{12}$");
    }

    // Check if user is 18 years old
    private boolean isValidAge(int year, int month, int day) {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int age = currentYear - year;
        return age >= 18;
    }

    private void populateSpinner(List<String> electionList) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, electionList);
        electionSpinner.setAdapter(adapter);
    }
    private String getUserIdFromSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String id=sharedPreferences.getString("userId", "");
        return id;
    }
    private String getUsername() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String name=sharedPreferences.getString("first_name", "");
        return name;
    }
    private void fetchCandidateData() {
        String userId = getUserIdFromSession();
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?user_id=eq." + userId)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ECMainActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseBody);
                        if (jsonArray.length() > 0) {
                            JSONObject candidate = jsonArray.getJSONObject(0);
                            runOnUiThread(() -> populateFields(candidate));
                            name = candidate.optString("first_name", "");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void checkUserSession() {
        if (sharedPreferences.contains("first_name")) {
            Intent intent = new Intent(ECMainActivity.this, ECInfoActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_CODE_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FILES && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            cardView.setVisibility(View.VISIBLE);
        }
    }

    private void submitData() {
        String fName = firstName.getText().toString().trim();
        String lName = lastName.getText().toString().trim();
        String aadharNum = aadharNumber.getText().toString().trim();
        String pName = partyName.getText().toString().trim();
        String gender = genderMale.isChecked() ? "male" : genderFemale.isChecked() ? "female" : genderOthers.isChecked() ? "others" : "";
        String dob = datePicker.getYear() + "/" + (datePicker.getMonth() + 1) + "/" + datePicker.getDayOfMonth();
        String electionName = electionSpinner.getSelectedItem() != null ? electionSpinner.getSelectedItem().toString() : "";

        // Validate each field
        if (!isValidName(fName)) {
            firstName.setError("Enter a valid first name (letters only)");
            return;
        }
        if (!isValidName(lName)) {
            lastName.setError("Enter a valid last name (letters only)");
            return;
        }
        if (!isValidAadhar(aadharNum)) {
            aadharNumber.setError("Aadhar must be exactly 12 digits");
            return;
        }
        if (pName.isEmpty()) {
            partyName.setError("Party name cannot be empty");
            return;
        }
        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidAge(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth())) {
            Toast.makeText(this, "You must be at least 18 years old to register", Toast.LENGTH_SHORT).show();
            return;
        }
        if (electionName.isEmpty()) {
            Toast.makeText(this, "Please select an election", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUpdate = !name.isEmpty(); // Check if candidate already exists

        if (isUpdate) {
            updateCandidateInSupabase(userId, fName, lName, gender, pName, dob, aadharNum, electionName);
        } else {
            if (fileUri == null) {
                Toast.makeText(this, "Please upload a required document", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadFileToSupabase(fileUri, fName, lName, gender, pName, dob, aadharNum, electionName);
        }
    }





    private void uploadFileToSupabase(Uri fileUri, String fName, String lName, String gender, String pName, String dob, String aadharNum,String electionName) {
        File file = getFileFromUri(fileUri);
        if (file == null) {
            runOnUiThread(() -> Toast.makeText(ECMainActivity.this, "File path error!", Toast.LENGTH_LONG).show());
            return;
        }

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_STORAGE_BUCKET + "/" + file.getName())
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ECMainActivity.this, "File Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponseimage: ",response.body().string());
                if (response.isSuccessful()) {
                    String fileUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_STORAGE_BUCKET + "/" + file.getName();
                    saveCandidateToSupabase(fName, lName, gender, pName, dob, aadharNum, fileUrl,electionName);
                }
            }
        });
    }

    private void saveCandidateToSupabase(String fName, String lName, String gender, String pName, String dob, String aadharNum, String fileUrl, String electionName) {
        Map<String, String> candidate = new HashMap<>();
        candidate.put("first_name", fName);
        candidate.put("last_name", lName);
        candidate.put("gender", gender);
        candidate.put("party_name", pName);
        candidate.put("dob", dob);
        candidate.put("aadhar_number", aadharNum);
        candidate.put("file_url", fileUrl);
        candidate.put("election_name", electionName); // Added election name
        candidate.put("user_id", userId);

        RequestBody requestBody = RequestBody.create(new JSONObject(candidate).toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ECMainActivity.this, "Submission Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponse: ",response.body().string());
                runOnUiThread(() -> {
                    Toast.makeText(ECMainActivity.this, "Submitted Successfully!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getApplicationContext(), ECInfoActivity.class));
                });
            }
        });
    }


    private void populateFields(JSONObject candidate) {
        try {
            firstName.setText(candidate.getString("first_name"));
            lastName.setText(candidate.getString("last_name"));
            partyName.setText(candidate.getString("party_name"));
            aadharNumber.setText(candidate.getString("aadhar_number"));

            // Set Gender
            String gender = candidate.getString("gender");
            if (gender.equalsIgnoreCase("male")) {
                genderMale.setChecked(true);
            } else if (gender.equalsIgnoreCase("female")) {
                genderFemale.setChecked(true);
            } else {
                genderOthers.setChecked(true);
            }

            // Set Date of Birth
            try {
                String dob = candidate.getString("dob").trim(); // Expected format: yyyy-MM-dd
                Log.d("DOBDebug", "Received DOB: " + dob); // Debugging

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setLenient(false); // Ensure strict date parsing
                Date date = sdf.parse(dob);

                if (date != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    int month = calendar.get(Calendar.MONTH);
                    int year = calendar.get(Calendar.YEAR);

                    datePicker.post(() -> datePicker.updateDate(year, month, day));
                } else {
                    Log.e("DateError", "Parsed date is null");
                }
            } catch (ParseException e) {
                Log.e("DateError", "Failed to parse date: " + e.getMessage());
            }

            // Set Election Spinner selection
            electionSpinner.post(() -> {
                try {
                    String electionName = candidate.getString("election_name").trim();
                    for (int i = 0; i < electionSpinner.getCount(); i++) {
                        String item = electionSpinner.getItemAtPosition(i).toString().trim();
                        if (item.equalsIgnoreCase(electionName)) {
                            electionSpinner.setSelection(i);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    Log.e("ElectionError", "Failed to get election_name: " + e.getMessage());
                }
            });

        } catch (JSONException e) {
            Log.e("JSONError", "Failed to populate fields: " + e.getMessage());
        }
    }




    private void updateCandidateInSupabase(String candidateId, String fName, String lName, String gender, String pName, String dob, String aadharNum, String electionName) {
        Map<String, String> updatedCandidate = new HashMap<>();
        updatedCandidate.put("first_name", fName);
        updatedCandidate.put("last_name", lName);
        updatedCandidate.put("gender", gender);
        updatedCandidate.put("party_name", pName);
        updatedCandidate.put("dob", dob);
        updatedCandidate.put("is_verified", String.valueOf(false));
        updatedCandidate.put("aadhar_number", aadharNum);
        updatedCandidate.put("election_name", electionName); // Added election name

        RequestBody requestBody = RequestBody.create(new JSONObject(updatedCandidate).toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/election_candidates?user_id=eq." + candidateId)
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ECMainActivity.this, "Update Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    Toast.makeText(ECMainActivity.this, "Updated Successfully!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getApplicationContext(), ECInfoActivity.class));
                });
            }
        });
    }


    private File getFileFromUri(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            @SuppressLint("Range") String fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            File file = new File(getCacheDir(), fileName);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void saveToSharedPreferences(String fName, String lName, String gender, String pName, String dob, String aadharNum) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("first_name", fName);
        editor.putString("last_name", lName);
        editor.putString("gender", gender);
        editor.putString("party_name", pName);
        editor.putString("dob", dob);
        editor.putString("aadhar_number", aadharNum);
        editor.apply();
    }
}
