package com.example.emailorginizersfe2.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.emailorginizersfe2.R;
import com.google.android.material.card.MaterialCardView;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class ManagePresetsActivity extends AppCompatActivity {

    private LinearLayout presetsContainer;
    private TextView emptyMessage;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_presets);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.manage_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        presetsContainer = findViewById(R.id.presets_container);
        emptyMessage = findViewById(R.id.text_no_presets);
        prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);

        loadPresets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPresets(); // Refresh when returning from editing
    }

    private void loadPresets() {
        presetsContainer.removeAllViews();
        Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));

        if (presetNames.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            presetsContainer.setVisibility(View.GONE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);
        presetsContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String title : new TreeSet<>(presetNames)) {
            // Create card view for each preset
            MaterialCardView cardView = (MaterialCardView) inflater.inflate(
                    R.layout.item_preset, presetsContainer, false);

            TextView presetName = cardView.findViewById(R.id.preset_name);
            Button editBtn = cardView.findViewById(R.id.btn_edit);
            Button deleteBtn = cardView.findViewById(R.id.btn_delete);

            presetName.setText(title);

            editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreatePresetActivity.class);
                intent.putExtra("EDIT_PRESET", title);
                startActivity(intent);
            });

            deleteBtn.setOnClickListener(v -> {
                deletePreset(title);
                loadPresets(); // Refresh list
            });

            presetsContainer.addView(cardView);
        }
    }

    private void deletePreset(String title) {
        SharedPreferences.Editor editor = prefs.edit();

        // Remove from names set
        Set<String> presetNames = new HashSet<>(prefs.getStringSet("preset_names", new HashSet<>()));
        presetNames.remove(title);
        editor.putStringSet("preset_names", presetNames);

        // Remove keyword data
        editor.remove(title + "_positive");
        editor.remove(title + "_negative");

        editor.apply();
    }
}