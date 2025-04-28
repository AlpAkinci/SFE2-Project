package com.example.emailorginizersfe2.ui.slideshow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emailorginizersfe2.R;
import com.example.emailorginizersfe2.cache.EmailCache;
import com.example.emailorginizersfe2.cache.Email;
import java.util.ArrayList;
import java.util.List;

public class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EMAIL = 0;
    private static final int TYPE_LOADING = 1;

    private final List<MailFetcher.MessageWithScore> emails = new ArrayList<>();
    private boolean isLoading = false;
    private List<String> currentKeywords = new ArrayList<>();
    private final EmailCache emailCache;
    private final String currentCacheKey = "inbox_default";

    public InboxAdapter(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("EmailCachePrefs", Context.MODE_PRIVATE);
        this.emailCache = EmailCache.getInstance(prefs);
    }

    public void setKeywords(List<String> keywords) {
        this.currentKeywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        notifyDataSetChanged(); // Force full refresh to ensure highlighting updates
    }

    public void setEmails(List<MailFetcher.MessageWithScore> newEmails) {
        this.emails.clear();
        if (newEmails != null) {
            this.emails.addAll(newEmails);
            cacheEmails(newEmails);
        }
        notifyDataSetChanged();
    }

    public void addEmails(List<MailFetcher.MessageWithScore> newEmails) {
        if (newEmails != null && !newEmails.isEmpty()) {
            int startPosition = emails.size();
            this.emails.addAll(newEmails);
            cacheEmails(newEmails);
            notifyItemRangeInserted(startPosition, newEmails.size());
        }
    }

    private void cacheEmails(List<MailFetcher.MessageWithScore> emailsToCache) {
        List<Email> cacheEntries = new ArrayList<>();
        for (MailFetcher.MessageWithScore message : emailsToCache) {
            cacheEntries.add(new Email(
                    message.id,
                    message.from,
                    message.subject,
                    message.content,
                    message.matchedKeywords
            ));
        }
        emailCache.putSortedList(currentCacheKey, cacheEntries);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LOADING) {
            return new LoadingViewHolder(inflater.inflate(R.layout.item_loading, parent, false));
        }
        return new EmailViewHolder(inflater.inflate(R.layout.item_email, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmailViewHolder) {
            MailFetcher.MessageWithScore email = emails.get(position);
            ((EmailViewHolder) holder).bind(email, currentKeywords);
        }
    }

    private SpannableString highlightKeywords(String text, List<String> keywords, int color) {
        SpannableString spannable = new SpannableString(text != null ? text : "");
        if (keywords == null || keywords.isEmpty()) return spannable;

        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) continue;

            String lowerKeyword = keyword.toLowerCase();
            int index = lowerText.indexOf(lowerKeyword);
            while (index >= 0) {
                spannable.setSpan(new BackgroundColorSpan(color),
                        index,
                        index + keyword.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = lowerText.indexOf(lowerKeyword, index + keyword.length());
            }
        }
        return spannable;
    }

    private SpannableString highlightSearchTerm(String text, String query) {
        SpannableString spannable = new SpannableString(text != null ? text : "");
        if (query == null || query.isEmpty()) return spannable;

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = lowerText.indexOf(lowerQuery);
        while (index >= 0) {
            spannable.setSpan(new BackgroundColorSpan(Color.YELLOW),
                    index,
                    index + query.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = lowerText.indexOf(lowerQuery, index + query.length());
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return emails.size() + (isLoading ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == emails.size() && isLoading) ? TYPE_LOADING : TYPE_EMAIL;
    }

    public void setLoading(boolean loading) {
        if (isLoading != loading) {
            isLoading = loading;
            if (loading) {
                notifyItemInserted(emails.size());
            } else {
                notifyItemRemoved(emails.size());
            }
        }
    }

    class EmailViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderView;
        private final TextView subjectView;
        private final TextView previewView;

        EmailViewHolder(View itemView) {
            super(itemView);
            senderView = itemView.findViewById(R.id.text_sender);
            subjectView = itemView.findViewById(R.id.text_subject);
            previewView = itemView.findViewById(R.id.text_preview);
        }

        void bind(MailFetcher.MessageWithScore email, List<String> keywords) {
            // Set sender text
            senderView.setText(email.from != null ? email.from : "Unknown");

            // Highlight keywords in subject
            subjectView.setText(highlightKeywords(
                    email.subject != null ? email.subject : "No Subject",
                    keywords,
                    Color.YELLOW));

            // Process content: Remove HTML tags, decode entities, and compact whitespace
            String content = email.content != null ? email.content : "";
            String plainTextContent = compactText(content);

            // Generate preview (first 100 chars, trimmed)
            String preview = plainTextContent.length() > 100 ?
                    plainTextContent.substring(0, 100).trim() + "..." :
                    plainTextContent;

            previewView.setText(highlightKeywords(preview, keywords, Color.CYAN));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), EmailDetailActivity.class);
                intent.putExtra("EMAIL_ID", email.id);
                itemView.getContext().startActivity(intent);
            });
        }

        // Helper method to clean HTML and compact whitespace
        private String compactText(String htmlText) {
            if (htmlText == null) return "";

            // Step 1: Remove all HTML tags
            String noHtml = htmlText.replaceAll("<[^>]*>", "");

            // Step 2: Replace common HTML entities (e.g., &nbsp;) with a space
            String decodedText = noHtml
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">");

            // Step 3: Collapse all whitespace (spaces, newlines, tabs) into a single space
            String compactText = decodedText.replaceAll("\\s+", " ");

            // Step 4: Trim leading/trailing spaces
            return compactText.trim();
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        final ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}