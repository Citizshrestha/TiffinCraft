package com.tiffincraft.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal 7-day earnings sparkline: smooth cubic line with dot markers,
 * no axes, transparent background. Designed to sit inside the green
 * earnings card, so the line and dots are white.
 */
public class SparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  linePath  = new Path();

    private final List<Float> values = new ArrayList<>();

    public SparklineView(Context context) {
        this(context, null);
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(0xFFFFFFFF);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0x66FFFFFF); // soft halo

        dotCorePaint.setStyle(Paint.Style.FILL);
        dotCorePaint.setColor(0xFFFFFFFF);
    }

    /** Replace the plotted series and redraw. Nulls/empty clear the chart. */
    public void setValues(@Nullable List<Double> data) {
        values.clear();
        if (data != null) {
            for (Double d : data) {
                values.add(d == null ? 0f : d.floatValue());
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) return;

        float density = getResources().getDisplayMetrics().density;
        float dotRadius  = 3f * density;
        float haloRadius = 5.5f * density;

        float padX = haloRadius + linePaint.getStrokeWidth();
        float padY = haloRadius + linePaint.getStrokeWidth();
        float w = getWidth()  - 2 * padX;
        float h = getHeight() - 2 * padY;
        if (w <= 0 || h <= 0) return;

        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for (float v : values) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        // Flat series (all equal, incl. all zero): draw a centered line
        float range = max - min;

        int n = values.size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padX + (w * i) / (n - 1);
            ys[i] = range == 0f
                    ? padY + h / 2f
                    : padY + h - ((values.get(i) - min) / range) * h;
        }

        // Smooth cubic bezier through the points
        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            float midX = (xs[i] + xs[i + 1]) / 2f;
            linePath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1]);
        }
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], haloRadius, dotPaint);
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotCorePaint);
        }
    }
}
