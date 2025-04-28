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
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

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
import com.example.emailorginizersfe2.ui.slideshow.MailFetcher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.mail.Multipart;
import javax.mail.BodyPart;

import javax.mail.Message;
import com.example.emailorginizersfe2.cache.EmailCache;
import com.example.emailorginizersfe2.cache.Email;
import javax.mail.internet.InternetAddress;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView leftNavView;
    private NavigationView rightNavView;
    private static final int PRESET_MENU_GROUP_ID = R.id.dynamic_presets_group;
    private RecyclerView recyclerView;
    private InboxAdapter adapter;
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
    private EmailCache emailCache;
    private final String currentCacheKey = "inbox_default";
    private boolean isRefreshing = false;
    private boolean forceRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Initialize EmailCache
        SharedPreferences prefs = getSharedPreferences("EmailCachePrefs", MODE_PRIVATE);
        emailCache = EmailCache.getInstance(prefs);

        mailFetcher = new MailFetcher("vlogs1167@gmail.com", "zsujmctuzmbstbla");
        initializeViews();
        setupToolbar();
        setupNavigation();
        setupRecyclerViewWithPagination();
        loadInitialEmails();

        setupSearchBar();

        new android.os.Handler().postDelayed(
                () -> debugInfo.setVisibility(View.GONE),
                12000
        );
    }

    private void setupSearchBar() {
        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBar.getText().toString());
                return true;
            }
            return false;
        });

        // Optional: Add real-time search as user types
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            // Show all emails when search is empty
            List<MailFetcher.MessageWithScore> allMessages = mailFetcher.getAllCachedMessages();
            List<MailFetcher.MessageWithScore> displayMessages = convertMessageWithScoresToDisplay(allMessages);
            adapter.setEmails(displayMessages);
            return;
        }

        List<MailFetcher.MessageWithScore> searchResults = mailFetcher.searchMessages(query);
        List<MailFetcher.MessageWithScore> displayMessages = convertMessageWithScoresToDisplay(searchResults);
        adapter.setEmails(displayMessages);
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        leftNavView = findViewById(R.id.nav_left_view);
        rightNavView = findViewById(R.id.nav_right_view);
        ImageButton rightDrawerButton = findViewById(R.id.right_drawer_button);
        Toolbar toolbar = findViewById(R.id.toolbar);
        MaterialButton composeButton = findViewById(R.id.compose_button);
        recyclerView = findViewById(R.id.recycler_inbox);
        debugInfo = findViewById(R.id.debug_info);
        composeButton.setOnClickListener(view -> startActivity(new Intent(this, ComposeEmailActivity.class)));
        rightDrawerButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
    }

    private void setupRecyclerViewWithPagination() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new InboxAdapter(this);  // Pass 'this' as Context
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
        rightNavView.setNavigationItemSelectedListener(this::handleRightDrawerSelection);
        leftNavView.setNavigationItemSelectedListener(this::handleLeftDrawerSelection);
    }

    private void loadInitialEmails() {
        if (isLoading) return;

        isLoading = true;
        adapter.setLoading(true);
        debugInfo.setText(getString(isFilteringActive ? R.string.loading_filtered_emails : R.string.loading_initial_emails));

        // First try to load from cache if not refreshing
        if (!forceRefresh && !isRefreshing && !mailFetcher.getAllCachedMessages().isEmpty()) {
            List<MailFetcher.MessageWithScore> cachedEmails = mailFetcher.getAllCachedMessages();
            List<MailFetcher.MessageWithScore> displayMessages = convertMessageWithScoresToDisplay(cachedEmails);
            adapter.setEmails(displayMessages);
            isLoading = false;
            adapter.setLoading(false);
            return;
        }

        // Reset refresh flag
        forceRefresh = false;

        new Thread(() -> {
            try {
                Message[] messages = mailFetcher.fetchInboxEmails(0, INITIAL_LOAD_COUNT);
                List<Message> messageList = Arrays.asList(messages);
                currentOffset = messageList.size();
                hasMoreEmails = messages.length == INITIAL_LOAD_COUNT;

                runOnUiThread(() -> {
                    List<MailFetcher.MessageWithScore> displayMessages = convertMessagesToDisplay(messageList);
                    adapter.setKeywords(currentPositiveKeywords);
                    adapter.setEmails(displayMessages);
                    debugInfo.setText(getString(R.string.loaded_emails_count,
                            adapter.getItemCount() - (isLoading ? 1 : 0),
                            isFilteringActive ? getString(R.string.filtered) : ""));
                    isLoading = false;
                    adapter.setLoading(false);
                });
            } catch (Exception e) {
                Log.e("INBOX", "Error loading emails", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_loading_emails, Toast.LENGTH_LONG).show();
                    debugInfo.setText(R.string.failed_to_load_emails);
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        }).start();
    }

    private List<MailFetcher.MessageWithScore> convertMessagesToDisplay(List<Message> messages) {
        List<MailFetcher.MessageWithScore> result = new ArrayList<>();

        for (Message msg : messages) {
            try {
                String subject = msg.getSubject() != null ? msg.getSubject() : "";
                String content = getMessageContent(msg);

                if (isFilteringActive && !matchesKeywords(subject, content, currentPositiveKeywords)) {
                    continue;
                }

                if (matchesKeywords(subject, content, currentNegativeKeywords)) {
                    continue;
                }

                int score = calculateEmailScore(subject, content, currentPositiveKeywords);
                List<String> matchedKeywords = findMatchedKeywords(subject, content, currentPositiveKeywords);
                result.add(new MailFetcher.MessageWithScore(msg, score, matchedKeywords));

            } catch (Exception e) {
                Log.e("INBOX", "Error processing message", e);
            }
        }

        if (isFilteringActive) {
            Collections.sort(result, (m1, m2) -> Integer.compare(m2.score, m1.score));
        }

        return result;
    }

    // Add this new method to handle MessageWithScore directly
    private List<MailFetcher.MessageWithScore> convertMessageWithScoresToDisplay(List<MailFetcher.MessageWithScore> messages) {
        if (!isFilteringActive && currentNegativeKeywords.isEmpty()) {
            return messages; // Return as-is if no filtering needed
        }

        List<MailFetcher.MessageWithScore> result = new ArrayList<>();

        for (MailFetcher.MessageWithScore msg : messages) {
            try {
                String subject = msg.subject != null ? msg.subject : "";
                String content = msg.content != null ? msg.content : "";

                if (isFilteringActive && !matchesKeywords(subject, content, currentPositiveKeywords)) {
                    continue;
                }

                if (matchesKeywords(subject, content, currentNegativeKeywords)) {
                    continue;
                }

                // For already scored messages, we might want to preserve their original score
                // or recalculate if needed
                int score = msg.score;
                List<String> matchedKeywords = msg.matchedKeywords;

                if (isFilteringActive) {
                    score = calculateEmailScore(subject, content, currentPositiveKeywords);
                    matchedKeywords = findMatchedKeywords(subject, content, currentPositiveKeywords);
                }

                result.add(new MailFetcher.MessageWithScore(
                        msg.id,
                        msg.from,
                        subject,
                        content,
                        score,
                        matchedKeywords
                ));

            } catch (Exception e) {
                Log.e("INBOX", "Error processing message", e);
            }
        }

        if (isFilteringActive) {
            Collections.sort(result, (m1, m2) -> Integer.compare(m2.score, m1.score));
        }

        return result;
    }

    private List<MailFetcher.MessageWithScore> convertCachedEmails(List<Email> cachedEmails) {
        List<MailFetcher.MessageWithScore> result = new ArrayList<>();
        for (Email email : cachedEmails) {
            result.add(new MailFetcher.MessageWithScore(
                    email.getId(),
                    email.getFrom(),
                    email.getSubject(),
                    email.getContent(),
                    0, // Default score
                    email.getMatchedKeywords()
            ));
        }
        return result;
    }

    private boolean matchesKeywords(String subject, String content, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }

        String lowerSubject = subject.toLowerCase();
        String lowerContent = content.toLowerCase();

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                if (lowerSubject.contains(lowerKeyword) || lowerContent.contains(lowerKeyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int calculateEmailScore(String subject, String content, List<String> keywords) {
        int score = 0;
        String lowerSubject = subject.toLowerCase();
        String lowerContent = content.toLowerCase();

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                score += 10 * countOccurrences(lowerSubject, lowerKeyword);
                score += countOccurrences(lowerContent, lowerKeyword);
            }
        }
        return score;
    }

    private List<String> findMatchedKeywords(String subject, String content, List<String> keywords) {
        List<String> matched = new ArrayList<>();
        String lowerSubject = subject.toLowerCase();
        String lowerContent = content.toLowerCase();

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                if (lowerSubject.contains(lowerKeyword) || lowerContent.contains(lowerKeyword)) {
                    matched.add(keyword);
                }
            }
        }
        return matched;
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }

    private String getMessageContent(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof Multipart) {
                StringBuilder sb = new StringBuilder();
                Multipart mp = (Multipart) content;
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bodyPart = mp.getBodyPart(i);
                    if (bodyPart.getContentType().startsWith("text/")) {
                        Object partContent = bodyPart.getContent();
                        if (partContent != null) {
                            sb.append(partContent.toString()).append(" ");
                        }
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            Log.w("INBOX", "Error getting message content", e);
        }
        return "";
    }

    private void loadMoreEmails() {
        if (isLoading || !hasMoreEmails) return;

        isLoading = true;
        adapter.setLoading(true);

        new Thread(() -> {
            try {
                Message[] messages = mailFetcher.fetchInboxEmails(currentOffset, PAGE_SIZE);
                List<Message> newMessages = Arrays.asList(messages);
                currentOffset += newMessages.size();
                hasMoreEmails = messages.length == PAGE_SIZE;

                runOnUiThread(() -> {
                    adapter.addEmails(convertMessagesToDisplay(newMessages));
                    debugInfo.setText(getString(R.string.loaded_total_emails,
                            adapter.getItemCount() - (isLoading ? 1 : 0),
                            isFilteringActive ? getString(R.string.filtered) : ""));
                    isLoading = false;
                    adapter.setLoading(false);
                });
            } catch (Exception e) {
                Log.e("INBOX", "Error loading more emails", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_loading_more_emails, Toast.LENGTH_LONG).show();
                    isLoading = false;
                    adapter.setLoading(false);
                });
            }
        }).start();
    }

    private void refreshEmails() {
        forceRefresh = true;  // Force refresh when explicitly requested
        currentOffset = 0;
        hasMoreEmails = true;
        adapter.setEmails(new ArrayList<>());
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

        // Clean up empty keywords
        currentPositiveKeywords.removeAll(Collections.singleton(""));
        currentNegativeKeywords.removeAll(Collections.singleton(""));

        // Log the keywords being applied
        Log.d("PRESET_APPLY", "Applying preset: " + presetName);
        Log.d("PRESET_APPLY", "Positive keywords: " + currentPositiveKeywords);
        Log.d("PRESET_APPLY", "Negative keywords: " + currentNegativeKeywords);

        isFilteringActive = !currentPositiveKeywords.isEmpty();

        Toast.makeText(this, getString(R.string.applying_preset, presetName), Toast.LENGTH_SHORT).show();
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
        Menu menu = rightNavView.getMenu();
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
            forceRefresh = true;
            clearFilters();
            Toast.makeText(this, R.string.showing_all_emails, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_snoozed) {
            Toast.makeText(this, R.string.jobs_selected, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_trash) {
            Toast.makeText(this, R.string.trash_selected, Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START); // Close left drawer
        return true;
    }

    private boolean handleRightDrawerSelection(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_manage_labels) {
            startActivity(new Intent(this, ManagePresetsActivity.class));
        } else if (id == R.id.nav_create_label) {
            startActivity(new Intent(this, CreatePresetActivity.class));
        } else if (item.getGroupId() == PRESET_MENU_GROUP_ID) {
            handlePresetSelection(item.getTitle().toString());
        }
        drawerLayout.closeDrawer(GravityCompat.END); // Close right drawer
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mailFetcher != null) {
            mailFetcher.close();
        }
    }
}