package com.example.emailorginizersfe2.cache;

import android.content.SharedPreferences;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.util.Log;

public class EmailCache {
    private static EmailCache instance;
    private final LruCache<String, Email> emailCache;
    private final Map<String, List<String>> sortedLists;
    private final SharedPreferences prefs;

    private EmailCache(SharedPreferences preferences) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        emailCache = new LruCache<String, Email>(cacheSize) {
            @Override
            protected int sizeOf(String key, Email email) {
                int size = 0;
                if (email.getContent() != null) size += email.getContent().length();
                if (email.getSubject() != null) size += email.getSubject().length();
                if (email.getFrom() != null) size += email.getFrom().length();
                return size / 1024 + 1;
            }
        };
        sortedLists = new HashMap<>();
        prefs = preferences;
    }

    public static synchronized EmailCache getInstance(SharedPreferences preferences) {
        if (instance == null) {
            instance = new EmailCache(preferences);
        }
        return instance;
    }

    public void putEmail(Email email) {
        if (email == null || email.getId() == null) {
            Log.e("EmailCache", "Attempt to cache null email or email with null ID");
            return;
        }

        // Store in memory cache
        emailCache.put(email.getId(), email);

        // Store in SharedPreferences for persistence
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(email.getId() + "_from", email.getFrom());
        editor.putString(email.getId() + "_subject", email.getSubject());
        editor.putString(email.getId() + "_content", email.getContent());

        // Store keywords
        Set<String> keywords = new HashSet<>();
        if (email.getMatchedKeywords() != null) {
            keywords.addAll(email.getMatchedKeywords());
        }
        editor.putStringSet(email.getId() + "_keywords", keywords);

        editor.apply();
        Log.d("EmailCache", "Successfully cached email with ID: " + email.getId());
    }

    public Email getEmail(String id) {
        if (id == null) {
            return null;
        }

        // First try memory cache
        Email email = emailCache.get(id);
        if (email != null) {
            Log.d("EmailCache", "Retrieved email from memory cache: " + id);
            return email;
        }

        // Fall back to SharedPreferences
        if (prefs.contains(id + "_from")) {
            Email cachedEmail = new Email(
                    id,
                    prefs.getString(id + "_from", ""),
                    prefs.getString(id + "_subject", ""),
                    prefs.getString(id + "_content", ""),
                    new ArrayList<>(prefs.getStringSet(id + "_keywords", new HashSet<>()))
            );
            Log.d("EmailCache", "Retrieved email from SharedPreferences: " + id);
            return cachedEmail;
        }

        Log.d("EmailCache", "No email found for ID: " + id);
        return null;
    }

    public void putSortedList(String key, List<Email> emails) {
        List<String> ids = new ArrayList<>();
        for (Email email : emails) {
            if (email != null && email.getId() != null) {
                ids.add(email.getId());
                putEmail(email);
            }
        }
        sortedLists.put(key, ids);
    }

    public List<Email> getSortedList(String key) {
        List<String> ids = sortedLists.get(key);
        if (ids == null) return null;

        List<Email> result = new ArrayList<>();
        for (String id : ids) {
            Email email = getEmail(id);
            if (email != null) {
                result.add(email);
            }
        }
        return result;
    }

    public List<Email> getRecentEmails() {
        LinkedHashSet<String> recentIds = new LinkedHashSet<>(
                prefs.getStringSet("recent_emails", new LinkedHashSet<String>())
        );
        List<Email> recentEmails = new ArrayList<>();
        for (String id : recentIds) {
            Email email = getEmail(id);
            if (email != null) {
                recentEmails.add(email);
            }
        }
        return recentEmails;
    }

    public void clear() {
        emailCache.evictAll();
        sortedLists.clear();
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("recent_emails");
        editor.apply();
    }
}