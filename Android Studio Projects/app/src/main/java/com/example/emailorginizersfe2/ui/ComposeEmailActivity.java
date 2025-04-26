package com.example.emailorginizersfe2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.emailorginizersfe2.R;
import com.google.android.material.button.MaterialButton;

import java.util.Stack;

public class ComposeEmailActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private EditText editTo, editSubject, editBody;
    private Spinner spinnerFrom;
    private TextView attachmentTextView;
    private MaterialButton attachButton;
    private Uri attachmentUri = null;

    // Presets
    private LinearLayout presetContainer;
    private TextView presetPreview;
    private Button saveCustomButton;
    private Button deleteCustomButton;
    private final String[] presetTitles = {"Greeting", "Follow-up", "Meeting", "Thank You", "Reminder"};
    private final String[] presetTexts = {
            "Hello, hope you're doing well.",
            "Just checking in regarding our last conversation.",
            "Let's schedule a meeting to discuss further.",
            "Thank you for your time and support.",
            "This is a gentle reminder for the upcoming deadline."
    };
    private String customPreset = null;

    // Undo/Redo
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private Button undoButton, redoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_email);

        // Original bindings
        editTo = findViewById(R.id.edit_to);
        editSubject = findViewById(R.id.edit_subject);
        editBody = findViewById(R.id.edit_body);
        spinnerFrom = findViewById(R.id.spinner_from);
        attachmentTextView = findViewById(R.id.text_attachment);
        attachButton = findViewById(R.id.button_attach_file);

        Toolbar toolbar = findViewById(R.id.toolbar_compose);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Compose Email");
        }

        attachButton.setOnClickListener(v -> openFilePicker());

        // New additions
        presetContainer = findViewById(R.id.preset_container);
        presetPreview = findViewById(R.id.text_preset_preview);
        saveCustomButton = findViewById(R.id.button_save_custom);
        deleteCustomButton = findViewById(R.id.button_delete_custom);
        undoButton = findViewById(R.id.button_undo);
        redoButton = findViewById(R.id.button_redo);

        setupPresetButtons();

        saveCustomButton.setOnClickListener(v -> {
            customPreset = editBody.getText().toString();
            Toast.makeText(this, "Custom preset saved", Toast.LENGTH_SHORT).show();
            setupPresetButtons();
        });

        deleteCustomButton.setOnClickListener(v -> {
            customPreset = null;
            Toast.makeText(this, "Custom preset deleted", Toast.LENGTH_SHORT).show();
            setupPresetButtons();
        });

        undoButton.setOnClickListener(v -> {
            if (!undoStack.isEmpty()) {
                redoStack.push(editBody.getText().toString());
                editBody.setText(undoStack.pop());
            }
        });

        redoButton.setOnClickListener(v -> {
            if (!redoStack.isEmpty()) {
                undoStack.push(editBody.getText().toString());
                editBody.setText(redoStack.pop());
            }
        });

        editBody.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                undoStack.push(editBody.getText().toString());
            }
        });
    }

    private void setupPresetButtons() {
        presetContainer.removeAllViews();
        for (int i = 0; i < presetTitles.length; i++) {
            final String title = presetTitles[i];
            final String text = presetTexts[i];
            addPresetButton(title, text);
        }
        if (customPreset != null) {
            addPresetButton("Custom", customPreset);
        }
    }

    private void addPresetButton(String title, String text) {
        Button presetButton = new Button(this);
        presetButton.setText(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        presetButton.setLayoutParams(params);
        presetButton.setMinWidth(120); //
        presetButton.setMaxLines(1);
        presetButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        presetButton.setTextSize(14);
        presetButton.setAllCaps(false); // Optional: keep text as-is

        presetButton.setOnClickListener(v -> {
            presetPreview.setText(text);
            presetPreview.setVisibility(View.VISIBLE);
            presetPreview.setOnClickListener(previewClick -> editBody.setText(text));
        });

        presetContainer.addView(presetButton);
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
                attachmentTextView.setText("\uD83D\uDCCE Attached: " + fileName);
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

