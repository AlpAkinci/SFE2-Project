package com.example.emailorginizersfe2.ui.slideshow;

import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailFetcher {
    private static final String TAG = "MailFetcher";
    private final String username;
    private final String password;
    private Store store;
    private Folder inbox;
    private final List<MessageWithScore> cachedMessages = new CopyOnWriteArrayList<>();

    public MailFetcher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Message[] fetchInboxEmails(int offset, int limit) throws MessagingException {
        return fetchFilteredEmails(offset, limit, Collections.emptyList(), Collections.emptyList());
    }

    public Message[] fetchFilteredEmails(int offset, int limit,
                                         List<String> positiveKeywords,
                                         List<String> negativeKeywords) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "10000");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");

        try {
            store.connect("imap.gmail.com", username, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int totalMessages = inbox.getMessageCount();
            Log.d(TAG, "Total messages in inbox: " + totalMessages);

            int start = Math.max(1, totalMessages - offset - limit + 1);
            int end = Math.min(totalMessages, totalMessages - offset);

            if (start > end) return new Message[0];

            Log.d(TAG, "Fetching messages from " + start + " to " + end);
            Message[] messages = inbox.getMessages(start, end);

            // Pre-fetch all content while the folder is open
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            inbox.fetch(messages, fp);

            // Process messages while connection is still open
            List<MessageWithScore> processedMessages = new ArrayList<>();
            for (Message msg : messages) {
                try {
                    MessageWithScore messageWithScore = new MessageWithScore(msg, 0, new ArrayList<>());
                    processedMessages.add(messageWithScore);
                    cachedMessages.add(messageWithScore); // Add to cache
                } catch (Exception e) {
                    Log.w(TAG, "Error processing message", e);
                }
            }

            // Now filter and sort using the pre-processed data
            return filterAndSortMessages(processedMessages, positiveKeywords, negativeKeywords);
        } finally {
            close();
        }
    }

    public List<MessageWithScore> searchMessages(String query) {
        List<MessageWithScore> results = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(cachedMessages);
        }

        String lowerQuery = query.toLowerCase();
        for (MessageWithScore message : cachedMessages) {
            boolean matches = (message.subject != null && message.subject.toLowerCase().contains(lowerQuery)) ||
                    (message.from != null && message.from.toLowerCase().contains(lowerQuery)) ||
                    (message.content != null && message.content.toLowerCase().contains(lowerQuery));

            if (matches) {
                results.add(message);
            }
        }
        return results;
    }

    public List<MessageWithScore> getAllCachedMessages() {
        return new ArrayList<>(cachedMessages);
    }

    public void clearCache() {
        cachedMessages.clear();
    }

    private Message[] filterAndSortMessages(List<MessageWithScore> processedMessages,
                                            List<String> positiveKeywords,
                                            List<String> negativeKeywords) {
        List<Message> filteredMessages = new ArrayList<>();
        List<MessageWithScore> scoredMessages = new ArrayList<>();

        for (MessageWithScore email : processedMessages) {
            try {
                String subject = email.subject;
                String content = email.content;

                // Negative keyword filtering
                boolean shouldExclude = false;
                if (negativeKeywords != null && !negativeKeywords.isEmpty()) {
                    shouldExclude = negativeKeywords.stream()
                            .filter(Objects::nonNull)
                            .anyMatch(keyword -> {
                                if (keyword == null || keyword.isEmpty()) return false;
                                String lowerKeyword = keyword.toLowerCase().trim();
                                return subject.toLowerCase().contains(lowerKeyword) ||
                                        content.toLowerCase().contains(lowerKeyword);
                            });
                }

                if (shouldExclude) {
                    Log.d(TAG, "Excluded message with subject: " + subject);
                    continue;
                }

                // Positive keyword scoring
                int score = 0;
                List<String> matchedKeywords = new ArrayList<>();
                if (positiveKeywords != null && !positiveKeywords.isEmpty()) {
                    for (String keyword : positiveKeywords) {
                        if (keyword != null && !keyword.isEmpty()) {
                            String lowerKeyword = keyword.toLowerCase().trim();
                            int subjectMatches = countOccurrences(subject.toLowerCase(), lowerKeyword);
                            int contentMatches = countOccurrences(content.toLowerCase(), lowerKeyword);

                            if (subjectMatches > 0 || contentMatches > 0) {
                                matchedKeywords.add(keyword);
                            }

                            score += 3 * subjectMatches;
                            score += contentMatches;
                        }
                    }

                    if (score > 0) {
                        scoredMessages.add(new MessageWithScore(
                                email.id,
                                email.from,
                                subject,
                                content,
                                score,
                                matchedKeywords
                        ));
                        continue;
                    }
                }

                filteredMessages.add(createMessageFromData(email.from, subject, content));
            } catch (Exception e) {
                Log.w(TAG, "Error filtering message", e);
            }
        }

        // Sort by score (highest first)
        scoredMessages.sort((m1, m2) -> Integer.compare(m2.score, m1.score));

        // Combine results
        List<Message> result = new ArrayList<>();
        for (MessageWithScore scored : scoredMessages) {
            try {
                result.add(createMessageFromData(scored.from, scored.subject, scored.content));
            } catch (MessagingException e) {
                Log.w(TAG, "Error recreating message", e);
            }
        }
        result.addAll(filteredMessages);

        return result.toArray(new Message[0]);
    }

    private Message createMessageFromData(String from, String subject, String content) throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.setSubject(subject);
        message.setText(content);
        return message;
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

    public static class MessageWithScore implements Serializable {
        public String id;
        public String from;
        public String subject;
        public String content;
        public int score;
        public List<String> matchedKeywords;

        public MessageWithScore(Message message, int score, List<String> matchedKeywords) {
            try {
                this.from = getFromAddress(message);
                this.subject = message.getSubject() != null ? message.getSubject() : "";
                this.content = getMessageContent(message);
                this.score = score;
                this.matchedKeywords = matchedKeywords != null ? matchedKeywords : new ArrayList<>();

                // Generate ID if not available
                try {
                    String[] messageId = message.getHeader("Message-ID");
                    this.id = (messageId != null && messageId.length > 0) ? messageId[0]
                            : "gen_" + Math.abs((from + subject + content).hashCode());
                } catch (Exception e) {
                    this.id = "gen_" + System.currentTimeMillis();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating MessageWithScore", e);
                // Initialize with default values
                this.from = "";
                this.subject = "";
                this.content = "";
                this.score = 0;
                this.matchedKeywords = new ArrayList<>();
                this.id = "gen_" + System.currentTimeMillis();
            }
        }

        public MessageWithScore(String id, String from, String subject, String content,
                                int score, List<String> matchedKeywords) {
            this.id = id;
            this.from = from != null ? from : "";
            this.subject = subject != null ? subject : "";
            this.content = content != null ? content : "";
            this.score = score;
            this.matchedKeywords = matchedKeywords != null ? matchedKeywords : new ArrayList<>();
        }
    }

    private static String getFromAddress(Message message) {
        try {
            InternetAddress[] addresses = (InternetAddress[]) message.getFrom();
            if (addresses != null && addresses.length > 0) {
                return addresses[0].getAddress();
            }
        } catch (MessagingException e) {
            Log.e(TAG, "Error getting from address", e);
        }
        return "unknown@example.com";
    }

    private static String getMessageContent(Message message) {
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
                            sb.append(partContent.toString()).append("\n\n");
                        }
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting message content", e);
        }
        return "";
    }

    public void close() {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            Log.e(TAG, "Error closing connection", e);
        }
    }

    public boolean isConnected() {
        return store != null && store.isConnected();
    }
}