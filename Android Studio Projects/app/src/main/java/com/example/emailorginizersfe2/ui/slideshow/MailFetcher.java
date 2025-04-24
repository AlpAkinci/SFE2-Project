package com.example.emailorginizersfe2.ui.slideshow;

import android.util.Log;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
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

    /**
     * Fetches emails with pagination support
     * @param offset The starting index (0-based)
     * @param limit Number of emails to fetch
     * @return Array of messages
     * @throws Exception if connection fails
     */
    public Message[] fetchInboxEmails(int offset, int limit) throws Exception {
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

            // Calculate the range of messages to fetch
            int start = Math.max(1, totalMessages - offset - limit + 1);
            int end = totalMessages - offset;

            // Ensure we don't go out of bounds
            if (start < 1) start = 1;
            if (end < 1) return new Message[0]; // No messages to fetch

            Log.d(TAG, "Fetching messages from " + start + " to " + end);

            // Fetch the messages (most recent first)
            return inbox.getMessages(start, end);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching emails", e);
            close(); // Ensure resources are cleaned up
            throw e;
        }
    }

    /**
     * Gets the total number of messages in the inbox
     */
    public int getTotalMessageCount() throws Exception {
        if (!isConnected()) {
            connect();
        }
        return inbox.getMessageCount();
    }

    private void connect() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect("imap.gmail.com", username, password);
        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
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