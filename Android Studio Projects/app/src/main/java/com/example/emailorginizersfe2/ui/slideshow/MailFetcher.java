package com.example.emailorginizersfe2.ui.slideshow;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

public class MailFetcher {
    private static final String TAG = "MailFetcher";
    private final String username;
    private final String password;
    private Store store;
    private Folder inbox;

    public MailFetcher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Message[] fetchInboxEmails(int offset, int limit) throws Exception {
        return fetchFilteredEmails(offset, limit, Collections.emptyList(), Collections.emptyList());
    }

    public Message[] fetchFilteredEmails(int offset, int limit,
                                         List<String> positiveKeywords,
                                         List<String> negativeKeywords) throws Exception {
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
            int end = totalMessages - offset;

            if (end < 1) return new Message[0];

            Log.d(TAG, "Fetching messages from " + start + " to " + end);
            Message[] messages = inbox.getMessages(start, end);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(FetchProfile.Item.ENVELOPE);
            inbox.fetch(messages, fp);

            return filterAndSortMessages(messages, positiveKeywords, negativeKeywords);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching emails", e);
            close();
            throw e;
        }
    }

    private Message[] filterAndSortMessages(Message[] messages,
                                            List<String> positiveKeywords,
                                            List<String> negativeKeywords) {
        List<Message> filteredMessages = new ArrayList<>();
        List<MessageWithScore> scoredMessages = new ArrayList<>();

        for (Message msg : messages) {
            try {
                String subject = msg.getSubject() != null ? msg.getSubject() : "";
                String content = getMessageContent(msg);

                boolean hasNegativeKeyword = false;
                if (negativeKeywords != null) {
                    for (String keyword : negativeKeywords) {
                        if (keyword != null &&
                                (subject.toLowerCase().contains(keyword.toLowerCase()) ||
                                        content.toLowerCase().contains(keyword.toLowerCase()))) {
                            hasNegativeKeyword = true;
                            break;
                        }
                    }
                }

                if (!hasNegativeKeyword) {
                    if (positiveKeywords != null && !positiveKeywords.isEmpty()) {
                        int score = 0;
                        for (String keyword : positiveKeywords) {
                            if (keyword != null) {
                                score += countMatches(subject, keyword) * 3;
                                score += countMatches(content, keyword);
                            }
                        }

                        if (score > 0) {
                            scoredMessages.add(new MessageWithScore(msg, score));
                        } else {
                            filteredMessages.add(msg);
                        }
                    } else {
                        filteredMessages.add(msg);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error processing message", e);
            }
        }

        scoredMessages.sort((m1, m2) -> Integer.compare(m2.score, m1.score));

        List<Message> result = new ArrayList<>();
        for (MessageWithScore mws : scoredMessages) {
            result.add(mws.message);
        }
        result.addAll(filteredMessages);

        return result.toArray(new Message[0]);
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
                    if (bodyPart.getContentType().startsWith("text/plain")) {
                        sb.append(bodyPart.getContent().toString());
                    }
                }
                return sb.toString();
            }
            return "";
        } catch (Exception e) {
            Log.w(TAG, "Error getting message content", e);
            return "";
        }
    }

    private int countMatches(String text, String keyword) {
        if (text == null || keyword == null) return 0;
        int count = 0;
        int idx = 0;
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        while ((idx = lowerText.indexOf(lowerKeyword, idx)) != -1) {
            count++;
            idx += lowerKeyword.length();
        }
        return count;
    }

    private static class MessageWithScore {
        final Message message;
        final int score;

        MessageWithScore(Message message, int score) {
            this.message = message;
            this.score = score;
        }
    }

    public void close() {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing resources", e);
        } finally {
            inbox = null;
            store = null;
        }
    }

    public boolean isConnected() {
        return store != null && store.isConnected();
    }
}