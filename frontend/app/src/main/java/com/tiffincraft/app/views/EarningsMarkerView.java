package com.tiffincraft.app.views;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.tiffincraft.app.R;
import com.tiffincraft.app.utils.CurrencyUtils;

/** Small dark tooltip shown above the touched point on the earnings trend chart. */
public class EarningsMarkerView extends MarkerView {

    private final TextView tvMonth;
    private final TextView tvValue;

    public EarningsMarkerView(Context context) {
        super(context, R.layout.view_earnings_marker);
        tvMonth = findViewById(R.id.tvMarkerMonth);
        tvValue = findViewById(R.id.tvMarkerValue);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvMonth.setText(e.getData() != null ? e.getData().toString() : "");
        tvValue.setText("value : " + CurrencyUtils.formatRupees(e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        // Centered above the point, but clamped inside the chart so the
        // tooltip never clips at the edges (e.g. the last month's point).
        float x = -(getWidth() / 2f);
        float y = -getHeight() - 24f;
        Chart<?> chart = getChartView();
        if (chart != null) {
            if (posX + x < 0) x = -posX;
            else if (posX + x + getWidth() > chart.getWidth()) x = chart.getWidth() - posX - getWidth();
        }
        if (posY + y < 0) y = 24f; // flip below the point near the top edge
        return new MPPointF(x, y);
    }
}
