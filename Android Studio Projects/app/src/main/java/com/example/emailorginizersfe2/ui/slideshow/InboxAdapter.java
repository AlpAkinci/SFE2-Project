package com.example.emailorginizersfe2.ui.slideshow;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.example.emailorginizersfe2.R;
import java.util.ArrayList;

public class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EMAIL = 0;
    private static final int TYPE_LOADING = 1;

    private final List<String> emails;
    private boolean isLoading = false;
    private List<String> currentPositiveKeywords = new ArrayList<>();

    public InboxAdapter(List<String> emails) {
        this.emails = emails;
    }

    public void setKeywords(List<String> positiveKeywords) {
        this.currentPositiveKeywords = positiveKeywords;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View loadingView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(loadingView);
        } else {
            View emailView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_email, parent, false);
            return new EmailViewHolder(emailView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmailViewHolder) {
            String emailData = emails.get(position);
            EmailViewHolder emailHolder = (EmailViewHolder) holder;

            // Parse email components
            String[] parts = emailData.split("\n");
            String from = parts[0].replace("From: ", "");
            String subject = parts[1].replace("Subject: ", "");
            String preview = parts.length > 2 ? parts[2].replace("Preview: ", "") : "";

            // Highlight keywords in subject
            SpannableString spannableSubject = new SpannableString(subject);
            for (String keyword : currentPositiveKeywords) {
                if (keyword == null || keyword.isEmpty()) continue;

                int start = subject.toLowerCase().indexOf(keyword.toLowerCase());
                if (start >= 0) {
                    spannableSubject.setSpan(
                            new ForegroundColorSpan(Color.GREEN),
                            start,
                            start + keyword.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }

            emailHolder.senderView.setText(from);
            emailHolder.subjectView.setText(spannableSubject);
            emailHolder.previewView.setText(preview);

            // Set different styling based on match count
            int matchCount = countMatches(subject + " " + preview, currentPositiveKeywords);
            emailHolder.itemView.setAlpha(0.7f + (0.3f * Math.min(1, matchCount/3f)));
        }
    }

    private int countMatches(String text, List<String> keywords) {
        int count = 0;
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) continue;
            if (lowerText.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
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

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView senderView;
        TextView subjectView;
        TextView previewView;

        EmailViewHolder(View itemView) {
            super(itemView);
            senderView = itemView.findViewById(R.id.text_sender);
            subjectView = itemView.findViewById(R.id.text_subject);
            previewView = itemView.findViewById(R.id.text_preview);

            itemView.setOnClickListener(v -> {
                // Handle email click
            });
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}