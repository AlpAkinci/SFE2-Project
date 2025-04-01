package com.example.emailorginizersfe2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.emailorginizersfe2.ui.ComposeEmailActivity;
import com.example.emailorginizersfe2.ui.CreatePresetActivity;
import com.example.emailorginizersfe2.ui.ManagePresetsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView leftNavView;
    private static final int PRESET_MENU_GROUP_ID = R.id.dynamic_presets_group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        leftNavView = findViewById(R.id.nav_left_view);
        NavigationView rightNavView = findViewById(R.id.nav_right_view);
        ImageButton rightDrawerButton = findViewById(R.id.right_drawer_button);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        rightDrawerButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        leftNavView.setNavigationItemSelectedListener(item -> {
            handleLeftDrawerSelection(item);
            return true;
        });

        rightNavView.setNavigationItemSelectedListener(item -> {
            handleRightDrawerSelection(item);
            return true;
        });

        MaterialButton composeButton = findViewById(R.id.compose_button);
        composeButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ComposeEmailActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        addSavedPresetsToDrawer();
    }

    private void addSavedPresetsToDrawer() {
        Menu menu = leftNavView.getMenu();

        // Clear dynamic group first
        for (int i = menu.size() - 1; i >= 0; i--) {
            MenuItem item = menu.getItem(i);
            if (item.getGroupId() == PRESET_MENU_GROUP_ID) {
                menu.removeItem(item.getItemId());
            }
        }

        // Load saved preset names from StringSet
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        Set<String> presetSet = prefs.getStringSet("preset_names", new HashSet<>());
        List<String> presetList = new ArrayList<>(presetSet);

        for (int i = 0; i < presetList.size(); i++) {
            String name = presetList.get(i);
            int itemId = 1000 + i;
            menu.add(PRESET_MENU_GROUP_ID, itemId, Menu.NONE, name)
                    .setIcon(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void handleLeftDrawerSelection(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_keywords) {
            // Keyword Organizer
        } else if (id == R.id.nav_favorites) {
            // Favorites logic
        } else if (id == R.id.nav_manage_labels) {
            startActivity(new Intent(MainActivity.this, ManagePresetsActivity.class));
        } else if (id == R.id.nav_create_label) {
            startActivity(new Intent(MainActivity.this, CreatePresetActivity.class));
        } else if (item.getGroupId() == PRESET_MENU_GROUP_ID) {
            String presetName = item.getTitle().toString();
            // TODO: Handle preset click
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleRightDrawerSelection(MenuItem item) {
        if (item.getItemId() == R.id.nav_inbox) {
            // Inbox logic
        }

        drawerLayout.closeDrawer(GravityCompat.END);
    }
}
