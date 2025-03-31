package com.example.emailorginizersfe2;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.emailorginizersfe2.ui.ComposeEmailActivity; // ✅ IMPORT ADDED
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DrawerLayout and NavigationViews
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView leftNavView = findViewById(R.id.nav_left_view);
        NavigationView rightNavView = findViewById(R.id.nav_right_view);
        ImageButton rightDrawerButton = findViewById(R.id.right_drawer_button);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup Drawer Toggle (Left Drawer)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Right Button Click Opens Right Drawer
        rightDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Handle Left Drawer Navigation Clicks
        leftNavView.setNavigationItemSelectedListener(item -> {
            handleLeftDrawerSelection(item);
            return true;
        });

        // Handle Right Drawer Navigation Clicks
        rightNavView.setNavigationItemSelectedListener(item -> {
            handleRightDrawerSelection(item);
            return true;
        });

        // COMPOSE BUTTON FUNCTIONALITY (launch in-app ComposeEmailActivity)
        MaterialButton composeButton = findViewById(R.id.compose_button);
        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ComposeEmailActivity.class);
                startActivity(intent);
            }
        });
    }

    // Handle Left Drawer Clicks
    private void handleLeftDrawerSelection(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_keywords) {
            // Open keyword search organizer
        } else if (id == R.id.nav_favorites) {
            // Open favorite search filters
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    // Handle Right Drawer Clicks (ONLY Inbox)
    private void handleRightDrawerSelection(MenuItem item) {
        if (item.getItemId() == R.id.nav_inbox) {
            // Open Inbox
        }

        drawerLayout.closeDrawer(GravityCompat.END);
    }
}
