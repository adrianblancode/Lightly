package co.adrianblan.lightly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * A custom view which shows the cycle of the sun.
 */
public class SunCycleView extends View {

    private static final int PATH_ITERATIONS = 400;
    private static final float PATH_HEIGHT_SCALE = 0.92f;

    private float pathOffset;
    private float twilightDividerPosition;

    private int canvasWidth;
    private int canvasHeight;

    private Path sunPath;
    private Path sunPathThin;

    private Paint sunPathPaint;
    private Paint sunPathThinPaint;
    private Paint sunCirclePaint;
    private Paint twilightDividerPaint;

    public SunCycleView(Context context) {
        super(context);

        init();
    }

    public SunCycleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {

        pathOffset = 0f;
        twilightDividerPosition = 0.5f;

        sunPath = new Path();
        sunPathThin = new Path();

        sunPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPathPaint.setColor(0xFFEFCB8B);
        sunPathPaint.setStyle(Paint.Style.STROKE);
        sunPathPaint.setStrokeWidth(8);

        sunPathThinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPathThinPaint.setColor(0xFFEFCB8B);
        sunPathThinPaint.setStyle(Paint.Style.STROKE);
        sunPathThinPaint.setStrokeWidth(3);

        sunCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunCirclePaint.setColor(0xFFEFCB8B);
        sunCirclePaint.setStrokeWidth(15);

        twilightDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        twilightDividerPaint.setColor(0xFF505050);
        twilightDividerPaint.setStrokeWidth(3);
    }

    private void calculatePath() {

        double tau = Math.PI * 2.0;
        double pathOffsetRadians = pathOffset * tau;

        sunPath.moveTo(0, (float) -Math.sin(pathOffsetRadians) * PATH_HEIGHT_SCALE * canvasHeight / 2);

        for(int i = 0; i < PATH_ITERATIONS; i++) {

            float percent = (float) i / PATH_ITERATIONS;
            double pathY = Math.sin(percent * tau + pathOffsetRadians) * PATH_HEIGHT_SCALE;

            sunPath.lineTo(percent * canvasWidth, (float) -(pathY * canvasHeight / 2));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);

        int minh = (int) (MeasureSpec.getSize(w) * (2f / 3.14f)) + getPaddingBottom() + getPaddingTop();
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

        float twilightDividerPositionScaled = twilightDividerPosition * 2f - 1f;

        canvas.drawLine(0, twilightDividerPositionScaled * canvasHeight, canvasWidth, twilightDividerPositionScaled * canvasHeight, twilightDividerPaint);
        canvas.drawPath(sunPath, sunPathPaint);
        canvas.drawCircle(canvasWidth / 4.0f, (float) -Math.sin(Math.PI / 2.0) * PATH_HEIGHT_SCALE * (canvasHeight / 2.0f), 20f, sunCirclePaint);

        canvas.restore();
    }
}
