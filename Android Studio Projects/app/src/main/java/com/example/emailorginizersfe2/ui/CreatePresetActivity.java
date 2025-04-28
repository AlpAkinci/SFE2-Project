package com.example.emailorginizersfe2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.emailorginizersfe2.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CreatePresetActivity extends AppCompatActivity {

    private EditText editPresetTitle, editPositiveKeyword, editNegativeKeyword;
    private LinearLayout layoutPositiveKeywords, layoutNegativeKeywords;
    private final ArrayList<String> positiveKeywords = new ArrayList<>();
    private final ArrayList<String> negativeKeywords = new ArrayList<>();
    private boolean isEditMode = false;
    private String originalPresetName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_preset);

        // Setup back toolbar
        Toolbar toolbar = findViewById(R.id.preset_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editPresetTitle = findViewById(R.id.edit_preset_title);
        editPositiveKeyword = findViewById(R.id.edit_positive_keyword);
        editNegativeKeyword = findViewById(R.id.edit_negative_keyword);
        layoutPositiveKeywords = findViewById(R.id.layout_positive_keywords);
        layoutNegativeKeywords = findViewById(R.id.layout_negative_keywords);

        Button buttonAddPositive = findViewById(R.id.button_add_positive);
        Button buttonAddNegative = findViewById(R.id.button_add_negative);
        Button buttonSave = findViewById(R.id.button_save_preset);

        buttonAddPositive.setOnClickListener(v -> addKeyword(editPositiveKeyword, layoutPositiveKeywords, positiveKeywords));
        buttonAddNegative.setOnClickListener(v -> addKeyword(editNegativeKeyword, layoutNegativeKeywords, negativeKeywords));

        buttonSave.setOnClickListener(v -> savePreset());

        // Check if we're in edit mode
        if (getIntent().hasExtra("EDIT_PRESET")) {
            originalPresetName = getIntent().getStringExtra("EDIT_PRESET");
            isEditMode = true;
            getSupportActionBar().setTitle("Edit Preset");
            editPresetTitle.setText(originalPresetName);
            loadPresetData(originalPresetName);
        } else {
            getSupportActionBar().setTitle("Create Preset");
        }
    }

    private void loadPresetData(String presetName) {
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);

        // Load positive keywords
        String positiveStr = prefs.getString(presetName + "_positive", "");
        if (!positiveStr.isEmpty()) {
            String[] positives = positiveStr.split(",");
            for (String keyword : positives) {
                if (!keyword.trim().isEmpty()) {
                    positiveKeywords.add(keyword.trim());
                    addKeywordToLayout(keyword.trim(), layoutPositiveKeywords, positiveKeywords);
                }
            }
        }

        // Load negative keywords
        String negativeStr = prefs.getString(presetName + "_negative", "");
        if (!negativeStr.isEmpty()) {
            String[] negatives = negativeStr.split(",");
            for (String keyword : negatives) {
                if (!keyword.trim().isEmpty()) {
                    negativeKeywords.add(keyword.trim());
                    addKeywordToLayout(keyword.trim(), layoutNegativeKeywords, negativeKeywords);
                }
            }
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addKeyword(EditText input, LinearLayout layout, ArrayList<String> list) {
        String keyword = input.getText().toString().trim();
        if (!keyword.isEmpty()) {
            list.add(keyword);
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(view -> {
                layout.removeView(chip);
                list.remove(keyword);
            });
            layout.addView(chip);
            input.setText("");
        }
    }

    private void addKeywordToLayout(String keyword, LinearLayout layout, ArrayList<String> list) {
        Chip chip = new Chip(this);
        chip.setText(keyword);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(view -> {
            layout.removeView(chip);
            list.remove(keyword);
        });
        layout.addView(chip);
    }

    private void savePreset() {
        String presetName = editPresetTitle.getText().toString().trim();

        if (presetName.isEmpty()) {
            Toast.makeText(this, "Please enter a preset name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (positiveKeywords.isEmpty() && negativeKeywords.isEmpty()) {
            Toast.makeText(this, "Please add at least one keyword", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert keyword lists to comma-separated strings
        String positiveKeywordsStr = String.join(",", positiveKeywords);
        String negativeKeywordsStr = String.join(",", negativeKeywords);

        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Handle name change in edit mode
        if (isEditMode && !originalPresetName.equals(presetName)) {
            // Remove old preset data
            Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));
            presetNames.remove(originalPresetName);
            editor.remove(originalPresetName + "_positive");
            editor.remove(originalPresetName + "_negative");

            // Add new name
            presetNames.add(presetName);
            editor.putStringSet("preset_names", presetNames);
        }
        else if (!isEditMode) {
            // Add to preset names set if new preset
            Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));
            presetNames.add(presetName);
            editor.putStringSet("preset_names", presetNames);
        }

        // Store keywords
        editor.putString(presetName + "_positive", positiveKeywordsStr);
        editor.putString(presetName + "_negative", negativeKeywordsStr);

        editor.apply();

        Toast.makeText(this, "Preset " + (isEditMode ? "updated" : "saved") + " successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}