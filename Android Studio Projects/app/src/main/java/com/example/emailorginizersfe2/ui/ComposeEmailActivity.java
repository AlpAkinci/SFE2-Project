package com.example.emailorginizersfe2.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.emailorginizersfe2.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class ComposeEmailActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private EditText editTo, editSubject, editBody;
    private Spinner spinnerFrom;
    private TextView attachmentTextView;
    private MaterialButton attachButton;
    private Uri attachmentUri = null;

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

    private static final String PREF_KEY_CUSTOM_PRESETS = "custom_presets"; // [ADDED]

    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private Button undoButton, redoButton;

    // [NEW] - instead of only saving body text, now save both title and body
    private final ArrayList<HashMap<String, String>> customPresets = new ArrayList<>();

    // [ADDED] Track selected preset index
    private int selectedCustomPresetIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_email);

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

        presetContainer = findViewById(R.id.preset_container);
        presetPreview = findViewById(R.id.text_preset_preview);
        saveCustomButton = findViewById(R.id.button_save_custom);
        deleteCustomButton = findViewById(R.id.button_delete_custom);
        undoButton = findViewById(R.id.button_undo);
        redoButton = findViewById(R.id.button_redo);

        loadCustomPresets();
        setupPresetButtons();

        saveCustomButton.setOnClickListener(v -> {
            String customText = editBody.getText().toString();
            if (!customText.isEmpty()) {
                showSaveCustomPresetDialog(customText);
            } else {
                Toast.makeText(this, "Cannot save empty preset", Toast.LENGTH_SHORT).show();
            }
        });

        deleteCustomButton.setOnClickListener(v -> {
            if (!customPresets.isEmpty()) {
                if (selectedCustomPresetIndex >= 0 && selectedCustomPresetIndex < customPresets.size()) {
                    customPresets.remove(selectedCustomPresetIndex);
                    selectedCustomPresetIndex = -1; // Reset after deleting
                    saveCustomPresets();
                    Toast.makeText(this, "Selected custom preset deleted", Toast.LENGTH_SHORT).show();
                    setupPresetButtons();
                    presetPreview.setText("");
                    presetPreview.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "No custom preset selected to delete", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No custom presets to delete", Toast.LENGTH_SHORT).show();
            }
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

        editBody.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                undoStack.push(editBody.getText().toString());
                redoStack.clear();
            }
            return false;
        });
    }

    private void setupPresetButtons() {
        presetContainer.removeAllViews();
        int i = 0;
        for (; i < presetTitles.length; i++) {
            addPresetButton(presetTitles[i], presetTexts[i], -1);
        }
        for (int j = 0; j < customPresets.size(); j++) {
            HashMap<String, String> custom = customPresets.get(j);
            addPresetButton(custom.get("title"), custom.get("text"), j);
        }
    }

    private void addPresetButton(String title, String text, int customIndex) {
        Button presetButton = new Button(this);
        presetButton.setText(title);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        presetButton.setLayoutParams(params);
        presetButton.setMinEms(8);
        presetButton.setMaxLines(1);
        presetButton.setEllipsize(android.text.TextUtils.TruncateAt.END);
        presetButton.setTextSize(14);
        presetButton.setAllCaps(false);
        presetButton.setPadding(24, 8, 24, 8);

        presetButton.setOnClickListener(v -> {
            presetPreview.setText(text);
            presetPreview.setVisibility(View.VISIBLE);
            presetPreview.setOnClickListener(previewClick -> {
                editBody.setText(text);
            });

            if (customIndex >= 0) {
                selectedCustomPresetIndex = customIndex;
            } else {
                selectedCustomPresetIndex = -1;
            }
        });

        presetContainer.addView(presetButton);
    }

    private void showSaveCustomPresetDialog(String customText) {
        EditText input = new EditText(this);
        input.setHint("Enter preset title");

        new AlertDialog.Builder(this)
                .setTitle("Name this Custom Preset")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        HashMap<String, String> preset = new HashMap<>();
                        preset.put("title", title);
                        preset.put("text", customText);
                        customPresets.add(preset);
                        saveCustomPresets();
                        setupPresetButtons();
                        Toast.makeText(this, "Custom preset saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCustomPresets() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        JSONArray jsonArray = new JSONArray();
        for (HashMap<String, String> map : customPresets) {
            JSONArray item = new JSONArray();
            item.put(map.get("title"));
            item.put(map.get("text"));
            jsonArray.put(item);
        }
        prefs.edit().putString(PREF_KEY_CUSTOM_PRESETS, jsonArray.toString()).apply();
    }

    private void loadCustomPresets() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String jsonString = prefs.getString(PREF_KEY_CUSTOM_PRESETS, null);
        if (jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                customPresets.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONArray item = jsonArray.getJSONArray(i);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("title", item.getString(0));
                    map.put("text", item.getString(1));
                    customPresets.add(map);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            attachmentUri = data.getData();
            if (attachmentUri != null) {
                attachmentTextView.setText(attachmentUri.getLastPathSegment());
            }
        }
    }
}
