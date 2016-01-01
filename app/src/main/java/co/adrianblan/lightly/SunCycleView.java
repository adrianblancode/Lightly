package co.adrianblan.lightly;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * A custom view which shows the cycle of the sun.
 */
public class SunCycleView extends View {

    private static final int PATH_ITERATIONS = 100;
    private static final float PATH_HEIGHT_SCALE = 0.80f;
    private static final float VIEW_HEIGHT_RATIO = 0.35f;

    private static final String DEFAULT_PRIMARY_COLOR_STRING = "#009688";

    private int accentColor;
    private ArrayList<Drawable> sunDrawables;

    private float sunPositionHorizontal;
    private float cycleOffsetHorizontal;
    private float twilightPositionVertical;

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
            sunDrawables = new ArrayList<Drawable>();
            sunDrawables.add(typedArray.getDrawable(R.styleable.SunCycleView_sunDrawable));
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

        sunPositionHorizontal = 0.5f;
        cycleOffsetHorizontal = 0.25f;
        twilightPositionVertical = 0.5f;

        // In case we couldn't get optional drawables
        if(sunDrawables == null) {
            sunDrawables = new ArrayList<Drawable>();
        }

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
        twilightDividerPaint.setColor(Color.LTGRAY);
        sunCirclePaint.setStyle(Paint.Style.STROKE);
        twilightDividerPaint.setStrokeWidth(2);
    }

    /**
     * Calculates PATH_ITERATIONS number of discrete points on the curve of the path.
     */
    private void calculatePath() {

        double cycleOffsetRadians = cycleOffsetHorizontal * Constants.tau;

        System.err.println("PO: " + cycleOffsetHorizontal + ", TDP: " + twilightPositionVertical + ", SO: " + sunPositionHorizontal);

        sunPath.reset();

        // Initial point of the path
        sunPath.moveTo(0, (float) -Math.sin(-cycleOffsetRadians) * PATH_HEIGHT_SCALE * canvasHeight / 2);

        for(int i = 0; i <= PATH_ITERATIONS; i++) {

            float percent = (float) i / PATH_ITERATIONS;

            double pathY = -Math.sin(percent * Constants.tau - cycleOffsetRadians) * PATH_HEIGHT_SCALE;

            sunPath.lineTo(percent * canvasWidth, (float) pathY * canvasHeight / 2);
        }
    }

    /**
     * Sets a list of Drawables that will be used for displaying the sun during a full cycle.
     *
     * The cycle is assumed to begin at sunrise. Can take any number of drawables and scales
     * accordingly, but a power of two is recommended. For example {sun, moon},
     * {sunrise, sun, sunset, moon} or more.
     */
    public void setSunDrawables(ArrayList<Drawable> sunDrawables) {
        this.sunDrawables = sunDrawables;
    }

    /**
     * Takes in a ArrayList of Drawable, together with the offset of the cycle, and position of the sun.
     * Assumes that the ArrayList is a set of sun icons starting from sunrise to a full cycle.
     * Returns the correct index for the sun icon to use.
     *
     * Can take any number of drawables and scales accordingly, but a power of two is recommended.
     * For example {sun, moon}, {sunrise, sun, sunset, moon} or more.
     *
     * @param sunIconDrawables the ArrayList of all sun icons to use over the cycle
     * @param cycleOffsetHorizontal the offset of the cycle [0, 1]
     * @param sunPositionHorizontal  the offset of the sun [0, 1]
     * @return the icon index based on the sun cycle
     */
    private int getSunDrawableIndex(ArrayList<Drawable> sunIconDrawables, float cycleOffsetHorizontal,
                                   float sunPositionHorizontal) {
        int arraySize = sunIconDrawables.size();

        // If there's only one element, pick it
        if(arraySize == 1) {
            return 0;
        }

        // Progress since sunrise scaled to [0, 1]
        double progressSinceSunrise = (cycleOffsetHorizontal - 0.25f - sunPositionHorizontal + 1.0) % 1.0;

        // Scales [0, 1] to [0, arraySize[
        int index = (int) Math.floor(progressSinceSunrise * arraySize);

        if(index == arraySize) {
            return arraySize - 1;
        }

        return index;
    }

    public void setSunPositionHorizontal(float sunPositionHorizontal) {
        this.sunPositionHorizontal = sunPositionHorizontal;
    }

    public void setCycleOffsetHorizontal(float cycleOffsetHorizontal) {
        this.cycleOffsetHorizontal = cycleOffsetHorizontal;
    }

    public void setTwilightPositionVertical(float twilightPositionVertical) {
        this.twilightPositionVertical = twilightPositionVertical;
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

        // Scale the position from [0, 1] to [-1, 1]
        float twilightDividerPositionScaled = -(twilightPositionVertical * 2f - 1f) * PATH_HEIGHT_SCALE;

        canvas.drawLine(0, twilightDividerPositionScaled * (canvasHeight / 2f), canvasWidth,
                twilightDividerPositionScaled * (canvasHeight / 2f), twilightDividerPaint);

        // Draws the path of the sun
        canvas.drawPath(sunPath, sunPathPaint);

        // Draws the sun
        double sunY = -Math.sin(sunPositionHorizontal * Constants.tau - cycleOffsetHorizontal * Constants.tau) * PATH_HEIGHT_SCALE * canvasHeight / 2f;

        if(!sunDrawables.isEmpty()) {

            int sunDrawableIndex = getSunDrawableIndex(sunDrawables, cycleOffsetHorizontal, sunPositionHorizontal);
            Bitmap sunBitmap = ((BitmapDrawable) sunDrawables.get(sunDrawableIndex)).getBitmap();

            ColorFilter sunIconColorFilter = new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
            sunCirclePaint.setColorFilter(sunIconColorFilter);

            canvas.drawBitmap(sunBitmap, sunPositionHorizontal * canvasWidth - (sunBitmap.getWidth() / 2f), (float) sunY - (sunBitmap.getHeight() / 2f), sunCirclePaint);
        } else {
            canvas.drawCircle(sunPositionHorizontal * canvasWidth, (float) sunY, 22f, sunCirclePaint);
        }

        // TODO: remove
        Paint kek = new Paint();
        kek.setColor(Color.RED);
        kek.setStyle(Paint.Style.STROKE);
        kek.setStrokeWidth(8);
        canvas.drawPoint(sunPositionHorizontal * canvasWidth, (float) sunY, kek);

        canvas.restore();
    }
}
