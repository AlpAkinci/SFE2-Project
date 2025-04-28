package com.example.emailorginizersfe2.ui.slideshow;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Added this import
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.emailorginizersfe2.R;
import com.example.emailorginizersfe2.cache.EmailCache;
import com.example.emailorginizersfe2.cache.Email;

public class EmailDetailActivity extends AppCompatActivity {
    private EmailCache emailCache;
    private static final int MAX_EMAIL_SIZE_BYTES = 500000; // 500KB limit for email content

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        // Initialize cache
        SharedPreferences prefs = getSharedPreferences("EmailCachePrefs", MODE_PRIVATE);
        EmailCache emailCache = EmailCache.getInstance(prefs);

        // Get email ID from intent
        String emailId = getIntent().getStringExtra("EMAIL_ID");
        Log.d("EmailDetail", "Attempting to open email with ID: " + emailId);

        if (emailId == null || emailId.isEmpty()) {
            showErrorAndFinish("No email ID provided");
            return;
        }

        // Try to get email from cache
        Email email = emailCache.getEmail(emailId);
        if (email == null) {
            Log.e("EmailDetail", "Email not found in cache for ID: " + emailId);
            showErrorAndFinish("Email not found");
            return;
        }

        // Display the email
        displayEmail(email);
    }

    private void displayEmail(Email email) {
        try {
            Log.d("EmailDetail", "Displaying email - From: " + email.getFrom()
                    + ", Subject: " + email.getSubject());

            ((TextView)findViewById(R.id.sender)).setText(email.getFrom());
            ((TextView)findViewById(R.id.subject)).setText(email.getSubject());
            ((TextView)findViewById(R.id.content)).setText(email.getContent());
        } catch (Exception e) {
            Log.e("EmailDetail", "Error displaying email", e);
            showErrorAndFinish("Error displaying email");
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("EmailDetail", message);
        finish();
    }
}