package com.example.emailorginizersfe2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.emailorginizersfe2.R;
import com.google.android.material.button.MaterialButton;

public class ComposeEmailActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private EditText editTo, editSubject, editBody;
    private Spinner spinnerFrom;
    private TextView attachmentTextView;
    private MaterialButton attachButton;
    private Uri attachmentUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_email);

        // Bind views
        editTo = findViewById(R.id.edit_to);
        editSubject = findViewById(R.id.edit_subject);
        editBody = findViewById(R.id.edit_body);
        spinnerFrom = findViewById(R.id.spinner_from);
        attachmentTextView = findViewById(R.id.text_attachment);
        attachButton = findViewById(R.id.button_attach_file);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_compose);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Optional: give the toolbar a title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Compose Email");
        }

        // Attach file button
        attachButton.setOnClickListener(v -> openFilePicker());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_send) {
            sendEmail();
            return true;
        } else if (id == R.id.action_attach) {
            openFilePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendEmail() {
        String to = editTo.getText().toString().trim();
        String subject = editSubject.getText().toString().trim();
        String body = editBody.getText().toString().trim();

        if (to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
        } else {
            String from = spinnerFrom.getSelectedItem().toString();
            String attachmentMsg = (attachmentUri != null)
                    ? "\nAttached: " + getFileNameFromUri(attachmentUri)
                    : "";
            Toast.makeText(this, "Sending from " + from + attachmentMsg, Toast.LENGTH_LONG).show();

            // TODO: Hook up SMTP or Gmail API
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select attachment"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            attachmentUri = data.getData();
            if (attachmentUri != null) {
                String fileName = getFileNameFromUri(attachmentUri);
                attachmentTextView.setText("📎 Attached: " + fileName);
                attachmentTextView.setVisibility(TextView.VISIBLE);
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = uri.getLastPathSegment();
        if (result == null) return "Unknown File";
        int cut = result.lastIndexOf('/');
        return (cut != -1) ? result.substring(cut + 1) : result;
    }
}
