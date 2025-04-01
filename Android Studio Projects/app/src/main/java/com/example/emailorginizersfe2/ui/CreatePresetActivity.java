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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CreatePresetActivity extends AppCompatActivity {

    private EditText editPresetTitle, editPositiveKeyword, editNegativeKeyword;
    private LinearLayout layoutPositiveKeywords, layoutNegativeKeywords;
    private final ArrayList<String> positiveKeywords = new ArrayList<>();
    private final ArrayList<String> negativeKeywords = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_preset);

        // Setup back toolbar
        Toolbar toolbar = findViewById(R.id.preset_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Preset");
        }

        sharedPreferences = getSharedPreferences("presets", Context.MODE_PRIVATE);

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Back button pressed
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

    private void savePreset() {
        String title = editPresetTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a preset name", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject presetObject = new JSONObject();
            presetObject.put("positive", new JSONArray(positiveKeywords));
            presetObject.put("negative", new JSONArray(negativeKeywords));

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("preset_" + title, presetObject.toString());

            // Copy set before modifying to avoid runtime exceptions
            Set<String> names = new HashSet<>(sharedPreferences.getStringSet("preset_names", new HashSet<>()));
            names.add(title);
            editor.putStringSet("preset_names", names);

            editor.apply();

            Toast.makeText(this, "Preset '" + title + "' saved!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving preset", Toast.LENGTH_SHORT).show();
        }
    }
}
