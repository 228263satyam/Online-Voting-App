package com.example.onlinevotingsystem;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button submitBtn, chooseFileBtn;
    private EditText firstName, lastName, aadharNumber;
    private RadioButton genderMale, genderFemale, genderOthers;
    private RadioGroup genderGrp;
    private DatePicker datePicker;
    private LinearLayout cardView;
    private static final int REQUEST_CODE_FILES = 1;
    private Uri fileUri;
    private File selectedFile;
    private OkHttpClient client = new OkHttpClient();
    private static final String SUPABASE_URL = "https://jguebadkcrppupsgvnqu.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpndWViYWRrY3JwcHVwc2d2bnF1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY0ODI1MzgsImV4cCI6MjA1MjA1ODUzOH0.CZ2KeuqdsRODg2QzpSGfqxlqTpaIDrt8WEEJ1A6JGuU";
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;
    private String userId,name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitBtn = findViewById(R.id.submitBtn);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        aadharNumber = findViewById(R.id.aadharNumber);
        genderMale = findViewById(R.id.male);
        genderFemale = findViewById(R.id.female);
        genderOthers = findViewById(R.id.others);
        datePicker = findViewById(R.id.datePicker);
        chooseFileBtn = findViewById(R.id.chooseFileBtn);
        genderGrp = findViewById(R.id.genderGrp);
        cardView = findViewById(R.id.uploadVoter);
        userId=getUserIdFromSession();
        name="";
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing, please wait...");
        progressDialog.setCancelable(false);

        chooseFileBtn.setOnClickListener(v -> selectFile());
        submitBtn.setOnClickListener(v -> submitData());
        fetchUserData();
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
//    private void checkUserSession() {
//        if (sharedPreferences.contains("first_name")) {
//            Intent intent = new Intent(MainActivity.this, InfoActivity.class);
//            startActivity(intent);
//            finish();
//        }
//    }
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_CODE_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FILES && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            selectedFile = getFileFromUri(fileUri);

            if (selectedFile != null) {
                cardView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Image selected: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to get file path.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            String fileName = getFileName(uri);
            File tempFile = new File(getCacheDir(), fileName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void submitData() {
        String fName = firstName.getText().toString().trim();
        String lName = lastName.getText().toString().trim();
        String aadharNum = aadharNumber.getText().toString().trim();
        String gender = genderMale.isChecked() ? "male" : genderFemale.isChecked() ? "female" : genderOthers.isChecked() ? "others" : "";
        String dob = datePicker.getYear() + "-" + (datePicker.getMonth() + 1) + "-" + datePicker.getDayOfMonth();

        // Name validation
        if (fName.isEmpty() || !fName.matches("^[a-zA-Z]{2,}$")) {
            firstName.setError("Enter a valid first name (at least 2 alphabets)");
            firstName.requestFocus();
            return;
        }
        if (lName.isEmpty() || !lName.matches("^[a-zA-Z]{2,}$")) {
            lastName.setError("Enter a valid last name (at least 2 alphabets)");
            lastName.requestFocus();
            return;
        }

        // Aadhar validation
        if (aadharNum.isEmpty() || !aadharNum.matches("^[0-9]{12}$")) {
            aadharNumber.setError("Enter a valid 12-digit Aadhar number");
            aadharNumber.requestFocus();
            return;
        }

        // Gender validation
        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }

        // Age validation (user should be at least 18 years old)
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int userYear = datePicker.getYear();
        int age = currentYear - userYear;
        if (age < 18) {
            Toast.makeText(this, "You must be at least 18 years old to register", Toast.LENGTH_SHORT).show();
            return;
        }

        // File validation
        if (selectedFile == null) {
            Toast.makeText(this, "Please select an image before submitting", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate file type (only JPG, PNG, JPEG allowed)
        String fileName = selectedFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
            Toast.makeText(this, "Invalid file type. Only JPG, PNG, or JPEG allowed", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate file size (should be less than 5MB)
        if (selectedFile.length() > 5 * 1024 * 1024) { // 5MB limit
            Toast.makeText(this, "File size should be less than 5MB", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        Toast.makeText(MainActivity.this, "Processing user data...", Toast.LENGTH_SHORT).show();

        // Check if user already has an entry in Supabase and update if necessary
        boolean update = name.isEmpty();
        if (!update) {
            updateUserData(fName, lName, aadharNum, gender, dob);
        } else {
            uploadFile(fName, lName, aadharNum, gender, dob);
        }
    }


    private void updateUserData(String fName, String lName, String aadharNum, String gender, String dob) {
        JSONObject userJson = new JSONObject();
        try {
            userJson.put("first_name", fName);
            userJson.put("last_name", lName);
            userJson.put("aadhar_number", aadharNum);
            userJson.put("gender", gender);
            userJson.put("is_verified",String.valueOf(false));
            userJson.put("dob", dob);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(userJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/user_data?user_id=eq." + userId) // Update specific user
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge")
                .method("PATCH", requestBody) // Use PATCH to update existing record
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed to update user data!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponse: ",response.body().string());
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "User data updated successfully!", Toast.LENGTH_SHORT).show();
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Failed to update user data!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    private void uploadFile(String fName, String lName, String aadharNum, String gender, String dob) {
        RequestBody fileBody = RequestBody.create(selectedFile, MediaType.parse("image/*"));

        MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", selectedFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/user_uploads/" + selectedFile.getName())
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponse1: ",response.body().string());
                if (response.isSuccessful()) {
                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/user_uploads/" + selectedFile.getName();
                    saveUserData(fName, lName, aadharNum, gender, dob, imageUrl);
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "File upload failed!", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void saveUserData(String fName, String lName, String aadharNum, String gender, String dob, String imageUrl) {
        JSONObject userJson = new JSONObject();
        try {
            userJson.put("first_name", fName);
            userJson.put("last_name", lName);
            userJson.put("aadhar_number", aadharNum);
            userJson.put("gender", gender);
            userJson.put("dob", dob);
            userJson.put("image_url", imageUrl);
            userJson.put("user_id",userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(userJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/user_data")
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> progressDialog.dismiss());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("onResponse2: ", response.body().string());

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "User data submitted successfully...", Toast.LENGTH_SHORT).show();

                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    // Ensure startActivity runs on UI thread
                    Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                    startActivity(intent);
                });
            }

        });
    }
    private void fetchUserData() {
        if (userId.isEmpty()) {
            return;
        }

        progressDialog.show();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/user_data")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("select", "*");

        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .header("apikey", SUPABASE_API_KEY)
                .header("Authorization", "Bearer " + SUPABASE_API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> populateUserData(responseData));
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "No user data found", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    private void populateUserData(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            if (jsonArray.length() > 0) {
                JSONObject user = jsonArray.getJSONObject(0);
                name=user.optString("first_name", "");
                firstName.setText(name);
                lastName.setText(user.optString("last_name", ""));
                aadharNumber.setText(user.optString("aadhar_number", ""));

                String gender = user.optString("gender", "others");
                if (gender.equals("male")) genderMale.setChecked(true);
                else if (gender.equals("female")) genderFemale.setChecked(true);
                else genderOthers.setChecked(true);

                String dob = user.optString("dob", "2000-01-01");
                String[] dateParts = dob.split("-");
                datePicker.updateDate(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1, Integer.parseInt(dateParts[2]));

                String imageUrl = user.optString("image_url", "");
                if (!imageUrl.isEmpty()) {
                    cardView.setVisibility(View.VISIBLE);
                    selectedFile = new File(imageUrl);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            progressDialog.dismiss();
        }
    }

}
