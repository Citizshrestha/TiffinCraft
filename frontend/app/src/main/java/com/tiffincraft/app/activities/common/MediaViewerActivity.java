package com.tiffincraft.app.activities.common;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.tiffincraft.app.R;

/**
 * Full-screen "lightbox" for chat images/videos (Instagram/Facebook style):
 * black backdrop, tap outside the media to close, tap the media itself does
 * nothing. Replaces the old behavior of opening the raw Cloudinary URL in
 * an external browser.
 */
public class MediaViewerActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_URL = "media_url";
    public static final String EXTRA_IS_VIDEO = "is_video";

    private VideoView videoFull;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, 0);
        setContentView(R.layout.activity_media_viewer);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        String url = getIntent().getStringExtra(EXTRA_MEDIA_URL);
        boolean isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        View root = findViewById(R.id.mediaViewerRoot);
        ImageView ivFullImage = findViewById(R.id.ivFullImage);
        videoFull = findViewById(R.id.videoFull);
        ProgressBar progressMediaLoading = findViewById(R.id.progressMediaLoading);
        ImageView btnClose = findViewById(R.id.btnCloseMediaViewer);

        // Tapping the black backdrop closes the viewer; tapping the media itself
        // is consumed separately below so it does not also trigger a close.
        root.setOnClickListener(v -> closeViewer());
        btnClose.setOnClickListener(v -> closeViewer());

        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Media not available.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isVideo) {
            progressMediaLoading.setVisibility(View.GONE);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.setOnClickListener(v -> { /* consume — do not close */ });

            MediaController controller = new MediaController(this);
            controller.setAnchorView(videoFull);
            videoFull.setMediaController(controller);
            videoFull.setVideoURI(Uri.parse(url));
            videoFull.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                videoFull.start();
            });
            videoFull.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Could not play video.", Toast.LENGTH_SHORT).show();
                finish();
                return true;
            });
        } else {
            ivFullImage.setVisibility(View.VISIBLE);
            ivFullImage.setOnClickListener(v -> { /* consume — do not close */ });

            Glide.with(this)
                    .load(url)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    @NonNull Target<Drawable> target, boolean isFirstResource) {
                            progressMediaLoading.setVisibility(View.GONE);
                            Toast.makeText(MediaViewerActivity.this,
                                    "Could not load image.", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                       Target<Drawable> target, @NonNull DataSource dataSource,
                                                       boolean isFirstResource) {
                            progressMediaLoading.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivFullImage);
        }
    }

    private void closeViewer() {
        if (videoFull != null && videoFull.getVisibility() == View.VISIBLE) {
            videoFull.stopPlayback();
        }
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoFull != null && videoFull.isPlaying()) {
            videoFull.pause();
        }
    }
}
