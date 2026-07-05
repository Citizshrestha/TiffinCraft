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

    public interface OnReviewActionListener {
        void onReplyClick(Review review);
    }

    public ReviewAdapter(Context context, List<Review> reviews, OnReviewActionListener listener) {
        this.context = context;
        this.reviews = reviews;
        this.listener = listener;
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

            // Format date
            try {
                String createdAt = review.getCreatedAt();
                if (createdAt != null) {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    Date date = inputFormat.parse(createdAt);
                    if (date != null) {
                        tvDate.setText(outputFormat.format(date));
                    }
                }
            } catch (Exception e) {
                tvDate.setText(review.getCreatedAt());
            }

            // Show cook reply if exists
            if (review.getCookReply() != null && !review.getCookReply().isEmpty()) {
                layoutCookReply.setVisibility(View.VISIBLE);
                tvCookReply.setText(review.getCookReply());
                btnReply.setText("Edit Reply");
            } else {
                layoutCookReply.setVisibility(View.GONE);
                btnReply.setText("Reply");
            }

            // Reply button click
            btnReply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReplyClick(review);
                }
            });
        }
    }
}
