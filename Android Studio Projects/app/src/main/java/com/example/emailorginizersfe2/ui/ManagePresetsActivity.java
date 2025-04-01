package com.example.emailorginizersfe2.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.emailorginizersfe2.R;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class ManagePresetsActivity extends AppCompatActivity {

    private LinearLayout presetsContainer;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_presets);

        // Toolbar setup with back button
        Toolbar toolbar = findViewById(R.id.manage_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        presetsContainer = findViewById(R.id.presets_container);
        emptyMessage = findViewById(R.id.text_no_presets);

        loadPresets();
    }

    private void loadPresets() {
        presetsContainer.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));

        if (presetNames.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            presetsContainer.setVisibility(View.GONE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);
        presetsContainer.setVisibility(View.VISIBLE);

        for (String title : new TreeSet<>(presetNames)) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 16, 16, 16);

            TextView presetText = new TextView(this);
            presetText.setText("• " + title);
            presetText.setTextSize(16);
            presetText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button deleteBtn = new Button(this);
            deleteBtn.setText("Delete");
            deleteBtn.setOnClickListener(v -> {
                deletePreset(title);
                loadPresets(); // Refresh list after deletion
            });

            itemLayout.addView(presetText);
            itemLayout.addView(deleteBtn);
            presetsContainer.addView(itemLayout);
        }
    }

    private void deletePreset(String title) {
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));
        presetNames.remove(title);

        editor.putStringSet("preset_names", presetNames);
        editor.remove("preset_" + title);  // remove full data for the preset
        editor.apply();
    }
}
