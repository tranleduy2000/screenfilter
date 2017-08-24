package com.duy.screenfilter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.duy.screenfilter.model.ColorProfile;

/**
 * Created by Duy on 23-Aug-17.
 */

public class MaskView extends View {
    private final Object mLock = new Object();
    private ColorProfile mProfile;

    public MaskView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (mLock) {
            if (mProfile != null) {
                canvas.drawColor(mProfile.getFilterColor());
            }
        }
    }

    public void setProfile(ColorProfile profile) {
        synchronized (mLock) {
            if (mProfile != null && mProfile.equals(profile)) {
                return;
            }
            this.mProfile = profile;
            invalidate();
        }
    }
}
