package br.edu.ucsal.rotinewater;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class WaterProgressView extends View {

    private static final int DEFAULT_MAX_VALUE = 2000;
    private static final int DEFAULT_SIZE_DP = 200;
    private static final long ANIMATION_DURATION_MS = 700;
    private static final float START_ANGLE = -90f;
    private static final float DEGREES_PER_PERCENT = 3.6f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int progressColor;
    private int textColor;
    private int alertColor;
    private int maxValue = DEFAULT_MAX_VALUE;
    private int currentValue = 0;
    private float animatedProgress = 0f;
    private boolean absoluteMode = false;
    private ValueAnimator animator;

    public WaterProgressView(Context context) {
        this(context, null);
    }

    public WaterProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        progressColor = context.getColor(R.color.water_blue);
        textColor = context.getColor(R.color.text_dark);
        alertColor = context.getColor(R.color.alert_red);
        readAttributes(context, attrs);
        setupPaints(context);
        setOnClickListener(v -> toggleMode());
    }

    private void readAttributes(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.WaterProgressView);
        try {
            progressColor = array.getColor(R.styleable.WaterProgressView_progressColor, progressColor);
            textColor = array.getColor(R.styleable.WaterProgressView_textColor, textColor);
            maxValue = Math.max(1, array.getInt(R.styleable.WaterProgressView_maxValue, DEFAULT_MAX_VALUE));
        } finally {
            array.recycle();
        }
    }

    private void setupPaints(Context context) {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(context.getColor(R.color.track_gray));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(int percent) {
        float target = Math.max(0, percent);
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(animatedProgress, target);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(valueAnimator -> {
            animatedProgress = (float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setMaxValue(int maxValueMl) {
        maxValue = Math.max(1, maxValueMl);
        updateProgress();
    }

    public void setCurrentValue(int currentValueMl) {
        currentValue = Math.max(0, currentValueMl);
        updateProgress();
    }

    public void setProgressColor(int color) {
        progressColor = color;
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = color;
        invalidate();
    }

    public boolean isOverGoal() {
        return currentValue > maxValue;
    }

    private void updateProgress() {
        setProgress(Math.round(currentValue * 100f / maxValue));
    }

    private void toggleMode() {
        absoluteMode = !absoluteMode;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = (int) (DEFAULT_SIZE_DP * getResources().getDisplayMetrics().density);
        setMeasuredDimension(
                resolveSize(desiredSize, widthMeasureSpec),
                resolveSize(desiredSize, heightMeasureSpec));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float size = Math.min(availableWidth, availableHeight);
        float centerX = getPaddingLeft() + availableWidth / 2f;
        float centerY = getPaddingTop() + availableHeight / 2f;

        boolean overGoal = isOverGoal();
        int activeColor = overGoal ? alertColor : progressColor;

        drawArcs(canvas, size, centerX, centerY, activeColor);
        drawTexts(canvas, size, centerX, centerY, overGoal);
    }

    private void drawArcs(Canvas canvas, float size, float centerX, float centerY, int activeColor) {
        float strokeWidth = size * 0.09f;
        float radius = size / 2f - strokeWidth / 2f;
        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        trackPaint.setStrokeWidth(strokeWidth);
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint);

        float sweep = Math.min(animatedProgress, 100f) * DEGREES_PER_PERCENT;
        if (sweep > 0f) {
            progressPaint.setStrokeWidth(strokeWidth);
            progressPaint.setColor(activeColor);
            canvas.drawArc(arcBounds, START_ANGLE, sweep, false, progressPaint);
        }
    }

    private void drawTexts(Canvas canvas, float size, float centerX, float centerY, boolean overGoal) {
        float maxTextWidth = size * 0.62f;

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setColor(textColor);
        drawCenteredText(canvas, getContext().getString(R.string.chart_title),
                centerX, centerY - size * 0.22f, size * 0.06f, maxTextWidth);

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(overGoal ? alertColor : textColor);
        drawCenteredText(canvas, buildMainText(), centerX, centerY + size * 0.02f, size * 0.17f, maxTextWidth);

        if (overGoal) {
            String alertText = getContext().getString(R.string.chart_alert, currentValue - maxValue);
            drawCenteredText(canvas, alertText, centerX, centerY + size * 0.22f, size * 0.06f, maxTextWidth);
        }
    }

    private String buildMainText() {
        if (absoluteMode) {
            return getContext().getString(R.string.absolute_format, currentValue, maxValue);
        }
        return getContext().getString(R.string.percent_format, Math.round(animatedProgress));
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float desiredSize, float maxWidth) {
        textPaint.setTextSize(desiredSize);
        float width = textPaint.measureText(text);
        if (width > maxWidth) {
            textPaint.setTextSize(desiredSize * maxWidth / width);
        }
        float baseline = y - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(text, x, baseline, textPaint);
    }
}
