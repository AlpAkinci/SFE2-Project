package com.example.emailorginizersfe2.cache;

import java.util.List;  // Add this import

public class Email {
    private String id;
    private String from;
    private String subject;
    private String content;
    private List<String> matchedKeywords;  // Now recognizes List

    public Email(String id, String from, String subject, String content, List<String> matchedKeywords) {
        this.id = id;
        this.from = from;
        this.subject = subject;
        this.content = content;
        this.matchedKeywords = matchedKeywords;
    }

    // Getters
    public String getId() { return id; }
    public String getFrom() { return from; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public List<String> getMatchedKeywords() { return matchedKeywords; }

    // Setters
    public void setMatchedKeywords(List<String> keywords) {
        this.matchedKeywords = keywords;
    }
}