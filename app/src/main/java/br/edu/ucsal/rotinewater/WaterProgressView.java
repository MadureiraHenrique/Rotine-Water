package br.edu.ucsal.rotinewater;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.graphics.ColorUtils;

public class WaterProgressView extends View {

    private static final int DEFAULT_MAX_VALUE = 2000;
    private static final int DEFAULT_SIZE_DP = 200;
    private static final long ANIMATION_DURATION_MS = 700;
    private static final float START_ANGLE = -90f;
    private static final float DEGREES_PER_PERCENT = 3.6f;
    private static final float STROKE_RATIO = 0.09f;
    private static final float KNOB_RATIO = 0.22f;
    private static final float GRADIENT_LIGHTEN = 0.45f;
    private static final int SECONDARY_TEXT_ALPHA = 150;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final Matrix shaderMatrix = new Matrix();

    private int progressColor;
    private int textColor;
    private int alertColor;
    private int maxValue = DEFAULT_MAX_VALUE;
    private int currentValue = 0;
    private float animatedProgress = 0f;
    private boolean absoluteMode = false;
    private ValueAnimator animator;

    private SweepGradient progressShader;
    private boolean shaderDirty = true;
    private boolean shaderOverGoal;
    private float shaderCenterX;
    private float shaderCenterY;

    public WaterProgressView(Context context) {
        this(context, null);
    }

    public WaterProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        progressColor = context.getColor(R.color.accent);
        textColor = context.getColor(R.color.text_primary);
        alertColor = context.getColor(R.color.alert_red);
        readAttributes(context, attrs);
        setupPaints(context);
        setOnClickListener(v -> setAbsoluteMode(!absoluteMode));
        updateDescription();
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
        trackPaint.setColor(context.getColor(R.color.track));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(Color.WHITE);
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
        shaderDirty = true;
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = color;
        invalidate();
    }

    public void setAbsoluteMode(boolean absoluteMode) {
        this.absoluteMode = absoluteMode;
        invalidate();
    }

    public boolean isAbsoluteMode() {
        return absoluteMode;
    }

    public boolean isOverGoal() {
        return currentValue > maxValue;
    }

    private boolean isGoalReached() {
        return currentValue == maxValue;
    }

    private void updateProgress() {
        setProgress(Math.round(currentValue * 100f / maxValue));
        updateDescription();
    }

    private void updateDescription() {
        Context context = getContext();
        setContentDescription(context.getString(R.string.chart_description,
                context.getString(R.string.chart_title),
                Math.round(currentValue * 100f / maxValue),
                currentValue,
                maxValue));
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

        drawRing(canvas, size, centerX, centerY);
        drawTexts(canvas, size, centerX, centerY);
    }

    private void drawRing(Canvas canvas, float size, float centerX, float centerY) {
        float strokeWidth = size * STROKE_RATIO;
        float radius = size / 2f - strokeWidth / 2f;
        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        trackPaint.setStrokeWidth(strokeWidth);
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint);

        float sweep = Math.min(animatedProgress, 100f) * DEGREES_PER_PERCENT;
        if (sweep <= 0f) {
            return;
        }

        updateShader(centerX, centerY, isOverGoal());
        progressPaint.setStrokeWidth(strokeWidth);
        canvas.drawArc(arcBounds, START_ANGLE, sweep, false, progressPaint);

        double endAngle = Math.toRadians(START_ANGLE + sweep);
        float knobX = centerX + radius * (float) Math.cos(endAngle);
        float knobY = centerY + radius * (float) Math.sin(endAngle);
        canvas.drawCircle(knobX, knobY, strokeWidth * KNOB_RATIO, knobPaint);
    }

    private void updateShader(float centerX, float centerY, boolean overGoal) {
        boolean unchanged = !shaderDirty
                && shaderOverGoal == overGoal
                && shaderCenterX == centerX
                && shaderCenterY == centerY;
        if (progressShader != null && unchanged) {
            return;
        }
        int endColor = overGoal ? alertColor : progressColor;
        int startColor = ColorUtils.blendARGB(endColor, Color.WHITE, GRADIENT_LIGHTEN);
        progressShader = new SweepGradient(centerX, centerY,
                new int[]{startColor, endColor, endColor},
                new float[]{0f, 0.55f, 1f});
        shaderMatrix.setRotate(START_ANGLE, centerX, centerY);
        progressShader.setLocalMatrix(shaderMatrix);
        progressPaint.setShader(progressShader);
        shaderDirty = false;
        shaderOverGoal = overGoal;
        shaderCenterX = centerX;
        shaderCenterY = centerY;
    }

    private void drawTexts(Canvas canvas, float size, float centerX, float centerY) {
        float maxTextWidth = size * 0.6f;
        boolean overGoal = isOverGoal();
        int secondaryColor = ColorUtils.setAlphaComponent(textColor, SECONDARY_TEXT_ALPHA);

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setColor(secondaryColor);
        drawCenteredText(canvas, getContext().getString(R.string.chart_title),
                centerX, centerY - size * 0.23f, size * 0.058f, maxTextWidth);

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(overGoal ? alertColor : textColor);
        drawCenteredText(canvas, buildMainText(), centerX, centerY, size * 0.2f, maxTextWidth);

        if (overGoal) {
            textPaint.setColor(alertColor);
        } else if (isGoalReached()) {
            textPaint.setColor(progressColor);
        } else {
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setColor(secondaryColor);
        }
        drawCenteredText(canvas, buildSubText(), centerX, centerY + size * 0.21f, size * 0.056f, maxTextWidth);
    }

    private String buildMainText() {
        if (absoluteMode) {
            return getContext().getString(R.string.absolute_format, currentValue, maxValue);
        }
        return getContext().getString(R.string.percent_format, Math.round(animatedProgress));
    }

    private String buildSubText() {
        Context context = getContext();
        if (isOverGoal()) {
            return context.getString(R.string.chart_alert, currentValue - maxValue);
        }
        if (isGoalReached()) {
            return context.getString(R.string.chart_reached);
        }
        return context.getString(R.string.chart_remaining, maxValue - currentValue);
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
