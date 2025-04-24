package com.example.emailorginizersfe2.ui.slideshow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.example.emailorginizersfe2.R;

public class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EMAIL = 0;
    private static final int TYPE_LOADING = 1;

    private final List<String> emails;
    private boolean isLoading = false;

    public InboxAdapter(List<String> emails) {
        this.emails = emails;
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
            String from = "";
            String subject = "";
            String preview = "";

            if (parts.length >= 1) from = parts[0].replace("From: ", "");
            if (parts.length >= 2) subject = parts[1].replace("Subject: ", "");
            if (parts.length >= 3) preview = parts[2].replace("Preview: ", "");

            emailHolder.senderView.setText(from);
            emailHolder.subjectView.setText(subject);
            emailHolder.previewView.setText(preview);

            // Set different styling for read/unread (example)
            boolean isRead = position % 3 == 0; // Replace with actual read status
            emailHolder.itemView.setAlpha(isRead ? 0.7f : 1.0f);
            emailHolder.subjectView.setTypeface(null, isRead ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        }
        // LoadingViewHolder doesn't need binding
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