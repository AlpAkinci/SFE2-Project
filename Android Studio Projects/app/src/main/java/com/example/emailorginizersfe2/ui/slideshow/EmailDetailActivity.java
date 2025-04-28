package com.example.emailorginizersfe2.ui.slideshow;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;  // Added missing import
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.emailorginizersfe2.R;
import com.example.emailorginizersfe2.cache.EmailCache;
import com.example.emailorginizersfe2.cache.Email;

public class EmailDetailActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        // Initialize WebView for HTML content
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false); // Disable JS for security
        webView.getSettings().setDefaultTextEncodingName("utf-8");

        // Initialize cache
        SharedPreferences prefs = getSharedPreferences("EmailCachePrefs", MODE_PRIVATE);
        EmailCache emailCache = EmailCache.getInstance(prefs);  // Made local variable

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

            // Check if content is HTML
            if (isHtmlContent(email.getContent())) {
                // Load HTML content in WebView
                webView.setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.content)).setVisibility(View.GONE);

                // Basic CSS to make content readable
                String styledHtml = "<html><head><style type=\"text/css\">" +
                        "body { color: #333333; font-family: sans-serif; line-height: 1.4; } " +
                        "a { color: #2196F3; } " +
                        "img { max-width: 100%; height: auto; }" +
                        "</style></head><body>" +
                        email.getContent() + "</body></html>";

                webView.loadDataWithBaseURL(
                        null,
                        styledHtml,
                        "text/html",
                        "UTF-8",
                        null);
            } else {
                // Plain text content
                webView.setVisibility(View.GONE);
                TextView contentView = findViewById(R.id.content);
                contentView.setVisibility(View.VISIBLE);
                contentView.setText(email.getContent());
            }
        } catch (Exception e) {
            Log.e("EmailDetail", "Error displaying email", e);
            showErrorAndFinish("Error displaying email");
        }
    }

    private boolean isHtmlContent(String content) {
        if (content == null) return false;
        // Simple check for HTML tags
        return content.contains("<html") ||
                content.contains("<div") ||
                content.contains("<p>") ||
                content.contains("<br") ||
                content.contains("<table");
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("EmailDetail", message);
        finish();
    }
}