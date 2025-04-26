package com.example.emailorginizersfe2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emailorginizersfe2.ui.ComposeEmailActivity;
import com.example.emailorginizersfe2.ui.CreatePresetActivity;
import com.example.emailorginizersfe2.ui.ManagePresetsActivity;
import com.example.emailorginizersfe2.ui.slideshow.InboxAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import com.example.emailorginizersfe2.ui.slideshow.MailFetcher;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView leftNavView;
    private static final int PRESET_MENU_GROUP_ID = R.id.dynamic_presets_group;
    private RecyclerView recyclerView;
    private InboxAdapter adapter;
    private final List<String> emailList = new ArrayList<>();
    private TextView debugInfo;
    private List<String> currentPositiveKeywords = new ArrayList<>();
    private List<String> currentNegativeKeywords = new ArrayList<>();
    private boolean isFilteringActive = false;
    private boolean isLoading = false;
    private boolean hasMoreEmails = true;
    private int currentOffset = 0;
    private static final int INITIAL_LOAD_COUNT = 40;
    private static final int PAGE_SIZE = 10;
    private MailFetcher mailFetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        mailFetcher = new MailFetcher("vlogs1167@gmail.com", "zsujmctuzmbstbla");
        initializeViews();
        setupToolbar();
        setupNavigation();
        setupRecyclerViewWithPagination();
        loadInitialEmails();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        debugInfo.setVisibility(View.GONE);
                    }
                },
                12000
        );
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        leftNavView = findViewById(R.id.nav_left_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        MaterialButton composeButton = findViewById(R.id.compose_button);
        recyclerView = findViewById(R.id.recycler_inbox);
        debugInfo = findViewById(R.id.debug_info);
        composeButton.setOnClickListener(view -> startActivity(new Intent(this, ComposeEmailActivity.class)));
    }

    private void setupRecyclerViewWithPagination() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InboxAdapter(emailList);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if (!isLoading && hasMoreEmails && dy > 0) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        loadMoreEmails();
                    }
                }
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        leftNavView.setNavigationItemSelectedListener(this::handleLeftDrawerSelection);
    }

    private void loadInitialEmails() {
        if (isLoading) return;
        isLoading = true;
        adapter.setLoading(true);
        debugInfo.setText(isFilteringActive ? "Loading filtered emails..." : "Loading initial emails...");
        new Thread(() -> {
            try {
                Message[] messages = isFilteringActive ?
                        mailFetcher.fetchFilteredEmails(0, INITIAL_LOAD_COUNT, currentPositiveKeywords, currentNegativeKeywords) :
                        mailFetcher.fetchInboxEmails(0, INITIAL_LOAD_COUNT);
                emailList.clear();
                for (Message msg : messages) {
                    String from = InternetAddress.toString(msg.getFrom());
                    String subject = msg.getSubject();
                    String preview = getMessagePreview(msg);
                    emailList.add("From: " + from + "\nSubject: " + subject + "\nPreview: " + preview);
                }
                currentOffset = emailList.size();
                hasMoreEmails = messages.length == INITIAL_LOAD_COUNT;
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    debugInfo.setText("Loaded " + emailList.size() + (isFilteringActive ? " filtered" : "") + " emails");
                    isLoading = false;
                    adapter.setLoading(false);
                });
            } catch (Exception e) {
                Log.e("INBOX", "Error loading emails", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading emails", Toast.LENGTH_LONG).show();
                    debugInfo.setText("Failed to load emails");
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        }).start();
    }

    private void loadMoreEmails() {
        if (isLoading || !hasMoreEmails) return;
        isLoading = true;
        adapter.setLoading(true);
        new Thread(() -> {
            try {
                Message[] messages = isFilteringActive ?
                        mailFetcher.fetchFilteredEmails(currentOffset, PAGE_SIZE, currentPositiveKeywords, currentNegativeKeywords) :
                        mailFetcher.fetchInboxEmails(currentOffset, PAGE_SIZE);
                int initialSize = emailList.size();
                for (Message msg : messages) {
                    String from = InternetAddress.toString(msg.getFrom());
                    String subject = msg.getSubject();
                    String preview = getMessagePreview(msg);
                    emailList.add("From: " + from + "\nSubject: " + subject + "\nPreview: " + preview);
                }
                currentOffset += messages.length;
                hasMoreEmails = messages.length == PAGE_SIZE;
                runOnUiThread(() -> {
                    adapter.notifyItemRangeInserted(initialSize, messages.length);
                    debugInfo.setText("Loaded " + emailList.size() + (isFilteringActive ? " filtered" : "") + " emails (total)");
                    isLoading = false;
                    adapter.setLoading(false);
                });
            } catch (Exception e) {
                Log.e("INBOX", "Error loading more emails", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading more emails", Toast.LENGTH_LONG).show();
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        }).start();
    }

    private String getMessagePreview(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                String text = (String) content;
                return text.length() > 100 ? text.substring(0, 100) + "..." : text;
            }
        } catch (Exception e) {
            Log.e("INBOX", "Error getting message preview", e);
        }
        return "";
    }

    private void refreshEmails() {
        currentOffset = 0;
        hasMoreEmails = true;
        emailList.clear();
        adapter.notifyDataSetChanged();
        loadInitialEmails();
    }

    private void clearFilters() {
        currentPositiveKeywords.clear();
        currentNegativeKeywords.clear();
        isFilteringActive = false;
        refreshEmails();
    }

    private void handlePresetSelection(String presetName) {
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        String positive = prefs.getString(presetName + "_positive", "");
        String negative = prefs.getString(presetName + "_negative", "");
        currentPositiveKeywords = new ArrayList<>(Arrays.asList(positive.split(",")));
        currentNegativeKeywords = new ArrayList<>(Arrays.asList(negative.split(",")));
        currentPositiveKeywords.removeAll(Collections.singleton(""));
        currentNegativeKeywords.removeAll(Collections.singleton(""));
        isFilteringActive = !currentPositiveKeywords.isEmpty() || !currentNegativeKeywords.isEmpty();
        showToast("Applying preset: " + presetName);
        refreshEmails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        addSavedPresetsToDrawer();
        refreshEmails();
    }

    private void addSavedPresetsToDrawer() {
        Menu menu = leftNavView.getMenu();
        menu.removeGroup(PRESET_MENU_GROUP_ID);
        SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
        Set<String> presetSet = prefs.getStringSet("preset_names", new HashSet<>());
        int index = 0;
        for (String name : presetSet) {
            menu.add(PRESET_MENU_GROUP_ID, 1000 + index++, Menu.NONE, name)
                    .setIcon(android.R.drawable.ic_menu_myplaces);
        }
    }

    private boolean handleLeftDrawerSelection(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_inbox) {
            clearFilters();
            Toast.makeText(this, "Showing all emails", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_manage_labels) {
            startActivity(new Intent(this, ManagePresetsActivity.class));
        } else if (id == R.id.nav_create_label) {
            startActivity(new Intent(this, CreatePresetActivity.class));
        } else if (item.getGroupId() == PRESET_MENU_GROUP_ID) {
            handlePresetSelection(item.getTitle().toString());
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mailFetcher != null) {
            mailFetcher.close();
        }
    }
}