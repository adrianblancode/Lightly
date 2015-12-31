package co.adrianblan.lightly;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * A custom view which shows the cycle of the sun.
 */
public class SunCycleView extends View {

    private static final int PATH_ITERATIONS = 100;
    private static final float PATH_HEIGHT_SCALE = 0.80f;
    private static final float VIEW_HEIGHT_RATIO = 0.35f;

    private static final String DEFAULT_PRIMARY_COLOR_STRING = "#009688";
    private static final String DEFAULT_TWILIGHT_DIVIDER_COLOR_STRING = "#505050";

    private int accentColor;
    private Drawable sunIconDrawable;

    private float sunOffset;
    private float pathOffset;
    private float twilightDividerPosition;

    private int canvasWidth;
    private int canvasHeight;

    private Path sunPath;

    private Paint sunPathPaint;
    private Paint sunCirclePaint;
    private Paint twilightDividerPaint;

    public SunCycleView(Context context) {
        super(context);
        init();
    }

    public SunCycleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SunCycleView, 0, 0);

        // Attempt to get attributes from XML
        try {
            // TODO: get system accent color
            accentColor = typedArray.getColor(R.styleable.SunCycleView_primaryColor, Color.parseColor(DEFAULT_PRIMARY_COLOR_STRING));
            sunIconDrawable = typedArray.getDrawable(R.styleable.SunCycleView_sunIcon);
        } finally {
            typedArray.recycle();
        }

        init();
    }

    /**
     * Initializes the member variables of the class.
     *
     * We are using an init methods because multiple constructors need to initialize the values.
     */
    private void init() {


        sunOffset = 0.25f;
        pathOffset = 0f;
        twilightDividerPosition = 0.5f;

        sunPath = new Path();

        // TODO: fix colors properly

        sunPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPathPaint.setColor(accentColor);
        sunPathPaint.setStyle(Paint.Style.STROKE);
        sunPathPaint.setStrokeWidth(8);

        sunCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunCirclePaint.setColor(accentColor);
        sunCirclePaint.setStyle(Paint.Style.STROKE);
        sunCirclePaint.setStrokeWidth(8);

        twilightDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        twilightDividerPaint.setColor(Color.DKGRAY);
        twilightDividerPaint.setStrokeWidth(3);
    }

    /**
     * Calculates PATH_ITERATIONS number of discrete points on the curve of the path.
     */
    private void calculatePath() {

        double tau = Math.PI * 2.0;
        double pathOffsetRadians = pathOffset * tau;

        // TODO fix bad
        sunPath = new Path();

        System.err.println(pathOffset + " " + twilightDividerPosition);

        // Initial point of the path
        sunPath.moveTo(0, (float) Math.sin(pathOffsetRadians) * PATH_HEIGHT_SCALE * canvasHeight / 2);

        for(int i = 0; i <= PATH_ITERATIONS; i++) {

            float percent = (float) i / PATH_ITERATIONS;

            /*
            if(percent + pathOffset > 1.0f) {
                pathOffsetRadians = -percent * tau;
            }
            */

            double pathY = Math.sin(percent * tau + pathOffsetRadians) * PATH_HEIGHT_SCALE;

            sunPath.lineTo(percent * canvasWidth, (float) pathY * canvasHeight / 2);
        }
    }

    public void setSunOffset(float sunOffset) {
        this.sunOffset = sunOffset;
    }

    public void setPathOffset(float pathOffset) {
        this.pathOffset = pathOffset;
    }

    public void setTwilightDividerPosition(float twilightDividerPosition) {
        this.twilightDividerPosition = twilightDividerPosition;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);

        // Scales the height according to the width
        int minh = (int) (MeasureSpec.getSize(w) * VIEW_HEIGHT_RATIO) + getPaddingBottom() + getPaddingTop();
        int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int width, int height, int previousWidth, int previousHeight) {
        super.onSizeChanged(width, height, previousWidth, previousHeight);
        this.canvasWidth = width;
        this.canvasHeight = height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        calculatePath();

        canvas.save();
        canvas.translate(0, getMeasuredHeight() / 2F);

        // Draws the twilight line
        float twilightDividerPositionScaled = twilightDividerPosition * 2f - 1f;
        canvas.drawLine(0, twilightDividerPositionScaled * canvasHeight, canvasWidth, twilightDividerPositionScaled * canvasHeight, twilightDividerPaint);

        // TODO solve drawline disappearing

        // Draws the path of the sun
        canvas.drawPath(sunPath, sunPathPaint);

        double tau = Math.PI * 2.0;

        // Draws the sun
        double sunY = Math.sin(sunOffset * tau) * PATH_HEIGHT_SCALE * canvasHeight / 2.0f;

        if(sunIconDrawable != null) {
            Bitmap sunBitmap = ((BitmapDrawable) sunIconDrawable).getBitmap();
            ColorFilter sunIconColorFilter = new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
            sunCirclePaint.setColorFilter(sunIconColorFilter);
            canvas.drawBitmap(sunBitmap, (sunOffset * canvasWidth), (float) sunY, sunCirclePaint);
        } else {
            canvas.drawCircle(sunOffset * canvasWidth, (float) sunY, 22f, sunCirclePaint);
        }

        canvas.restore();
    }
}
