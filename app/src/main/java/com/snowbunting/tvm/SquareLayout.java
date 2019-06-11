package com.snowbunting.tvm;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

//Replace RelativeLayout with any layout of your choice
public class SquareLayout  extends RelativeLayout {

    public SquareLayout(Context context) {
        super(context);
    }
    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SquareLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // ratio 1:1

        //noinspection SuspiciousNameCombination
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);

        int size = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(size, size);
    }
}
