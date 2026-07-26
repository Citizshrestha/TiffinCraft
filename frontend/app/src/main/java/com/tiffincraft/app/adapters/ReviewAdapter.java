package com.tiffincraft.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiffincraft.app.R;
import com.tiffincraft.app.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private List<Review> reviews;
    private final OnReviewActionListener listener;
    private final boolean readOnly;
    private final int currentUserId;

    public interface OnReviewActionListener {
        void onReplyClick(Review review);
        default void onEditClick(Review review) {}
        default void onDeleteClick(Review review) {}
    }

    public ReviewAdapter(Context context, List<Review> reviews, OnReviewActionListener listener) {
        this(context, reviews, listener, false, -1);
    }

    /** readOnly hides the reply button — for customer-facing review lists (e.g. cook profile page). */
    public ReviewAdapter(Context context, List<Review> reviews, OnReviewActionListener listener, boolean readOnly) {
        this(context, reviews, listener, readOnly, -1);
    }

    /** currentUserId enables Edit/Delete on the row belonging to the logged-in customer (readOnly lists only). */
    public ReviewAdapter(Context context, List<Review> reviews, OnReviewActionListener listener, boolean readOnly, int currentUserId) {
        this.context = context;
        this.reviews = reviews;
        this.listener = listener;
        this.readOnly = readOnly;
        this.currentUserId = currentUserId;
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCustomerName;
        private final TextView tvDate;
        private final RatingBar ratingBar;
        private final TextView tvRating;
        private final TextView tvComment;
        private final TextView tvMealName;
        private final TextView tvCookReply;
        private final View layoutCookReply;
        private final Button btnReply;
        private final View layoutOwnReviewActions;
        private final Button btnEditReview;
        private final Button btnDeleteReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvCookReply = itemView.findViewById(R.id.tvCookReply);
            layoutCookReply = itemView.findViewById(R.id.layoutCookReply);
            btnReply = itemView.findViewById(R.id.btnReply);
            layoutOwnReviewActions = itemView.findViewById(R.id.layoutOwnReviewActions);
            btnEditReview = itemView.findViewById(R.id.btnEditReview);
            btnDeleteReview = itemView.findViewById(R.id.btnDeleteReview);
        }

        public void bind(Review review) {
            tvCustomerName.setText(review.getCustomerName() != null ? review.getCustomerName() : "Customer");
            tvRating.setText(String.valueOf(review.getRating()));
            ratingBar.setRating(review.getRating());
            tvComment.setText(review.getComment() != null ? review.getComment() : "No comment");

            if (review.getMealName() != null && !review.getMealName().isEmpty()) {
                tvMealName.setVisibility(View.VISIBLE);
                tvMealName.setText("Meal: " + review.getMealName());
            } else {
                tvMealName.setVisibility(View.GONE);
            }

            tvDate.setText(formatDate(review.getCreatedAt()));

            // Show cook reply if exists
            if (review.getCookReply() != null && !review.getCookReply().isEmpty()) {
                layoutCookReply.setVisibility(View.VISIBLE);
                tvCookReply.setText(review.getCookReply());
                btnReply.setText("Edit Reply");
            } else {
                layoutCookReply.setVisibility(View.GONE);
                btnReply.setText("Reply");
            }

            if (readOnly) {
                btnReply.setVisibility(View.GONE);
            } else {
                btnReply.setVisibility(View.VISIBLE);
                btnReply.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReplyClick(review);
                    }
                });
            }

            // Edit/Delete — only on the row belonging to the logged-in customer,
            // and only in read-only (customer-facing) lists; a cook never owns a review.
            boolean isOwnReview = readOnly && currentUserId > 0 && review.getCustomerId() == currentUserId;
            layoutOwnReviewActions.setVisibility(isOwnReview ? View.VISIBLE : View.GONE);
            if (isOwnReview) {
                btnEditReview.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(review);
                });
                btnDeleteReview.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(review);
                });
            }
        }

        private String formatDate(String rawDate) {
            if (rawDate == null || rawDate.isEmpty()) return "";
            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss"
            };
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.US);
                    if (pattern.endsWith("'Z'")) {
                        inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    }
                    Date date = inputFormat.parse(rawDate);
                    if (date != null) {
                        return new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(date);
                    }
                } catch (Exception ignored) {}
            }
            return rawDate;
        }
    }
}
