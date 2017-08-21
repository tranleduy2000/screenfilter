package com.duy.screenfilter.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.duy.screenfilter.R;
import com.duy.screenfilter.utils.AppSetting;
import com.duy.screenfilter.utils.Utility;
import com.github.florent37.diagonallayout.DiagonalLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Locale;

public class SchedulerDialog extends AlertDialog implements TimePickerDialog.OnTimeSetListener {

    private TimePickerDialog sunrisePicker, sunsetPicker;
    private TextView sunriseTime, sunsetTime;

    private FrameLayout mFrameLayout;
    private DiagonalLayout mLeftLayout, mRightLayout;

    private int hrsSunrise = 6, minSunrise = 0, hrsSunset = 22, minSunset = 0;

    private AppSetting mSettings;

    public SchedulerDialog(Context context) {
        super(context);
        if (context instanceof Activity) setOwnerActivity((Activity) context);
        init();
    }

    public SchedulerDialog(Context context, OnDismissListener onDismissListener) {
        super(context);
        if (context instanceof Activity) setOwnerActivity((Activity) context);
        if (onDismissListener != null) setOnDismissListener(onDismissListener);
        init();
    }

    public SchedulerDialog(Context context, int themeResId) {
        super(context, themeResId);
        if (context instanceof Activity) setOwnerActivity((Activity) context);
        init();
    }

    protected SchedulerDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        if (context instanceof Activity) setOwnerActivity((Activity) context);
        init();
    }

    private void init() {
        mSettings = AppSetting.getInstance(getContext());
        hrsSunrise = mSettings.getInt(AppSetting.KEY_HOURS_SUNRISE, 6);
        minSunrise = mSettings.getInt(AppSetting.KEY_MINUTES_SUNRISE, 0);
        hrsSunset = mSettings.getInt(AppSetting.KEY_HOURS_SUNSET, 22);
        minSunset = mSettings.getInt(AppSetting.KEY_MINUTES_SUNSET, 0);

        View rootView = getLayoutInflater().inflate(R.layout.dialog_scheduler, null);
        setView(rootView);

        mLeftLayout = rootView.findViewById(R.id.left_layout);
        mRightLayout = rootView.findViewById(R.id.right_layout);
        mFrameLayout = rootView.findViewById(R.id.frame_layout);
        mFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mFrameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        final int measureWidth = mFrameLayout.getMeasuredWidth();
                        final int dp20 = (int) Utility.dpToPx(getContext(), 20);
                        Log.i("TAG", "measureWidth: " + measureWidth);
                        mLeftLayout.getLayoutParams().width = measureWidth / 2 + dp20;
                        mRightLayout.getLayoutParams().width = measureWidth / 2 + dp20;
                    }
                });

        sunriseTime = rootView.findViewById(R.id.sunrise_time);
        sunsetTime = rootView.findViewById(R.id.sunset_time);
        sunriseTime.setText(String.format(Locale.getDefault(), "%1$02d:%2$02d", hrsSunrise, minSunrise));
        sunsetTime.setText(String.format(Locale.getDefault(), "%1$02d:%2$02d", hrsSunset, minSunset));

        Switch switchView = rootView.findViewById(R.id.auto_switch);
        switchView.setChecked(mSettings.getBoolean(AppSetting.KEY_AUTO_MODE, false));
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mSettings.putBoolean(AppSetting.KEY_AUTO_MODE, b);
                Utility.updateAlarmSettings(getContext());
            }
        });

        rootView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        rootView.findViewById(R.id.sunrise_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sunrisePicker = TimePickerDialog.newInstance(
                        SchedulerDialog.this,
                        hrsSunrise,
                        minSunrise,
                        true
                );
                if (mSettings.getBoolean(AppSetting.KEY_DARK_THEME, false)) {
                    sunrisePicker.setThemeDark(true);
                }
                sunrisePicker.show(getOwnerActivity().getFragmentManager(), "sunrise_dialog");
            }
        });
        rootView.findViewById(R.id.sunset_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sunsetPicker = TimePickerDialog.newInstance(
                        SchedulerDialog.this,
                        hrsSunset,
                        minSunset,
                        true
                );
                if (mSettings.getBoolean(AppSetting.KEY_DARK_THEME, false)) {
                    sunsetPicker.setThemeDark(true);
                }
                sunsetPicker.show(getOwnerActivity().getFragmentManager(), "sunset_dialog");
            }
        });
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        if (view == sunrisePicker) {
            hrsSunrise = hourOfDay;
            minSunrise = minute;
            sunriseTime.setText(String.format(Locale.getDefault(), "%1$02d:%2$02d", hrsSunrise, minSunrise));
            mSettings.putInt(AppSetting.KEY_HOURS_SUNRISE, hrsSunrise);
            mSettings.putInt(AppSetting.KEY_MINUTES_SUNRISE, minSunrise);
        } else if (view == sunsetPicker) {
            hrsSunset = hourOfDay;
            minSunset = minute;
            sunsetTime.setText(String.format(Locale.getDefault(), "%1$02d:%2$02d", hrsSunset, minSunset));
            mSettings.putInt(AppSetting.KEY_HOURS_SUNSET, hrsSunset);
            mSettings.putInt(AppSetting.KEY_MINUTES_SUNSET, minSunset);
        }
        Utility.updateAlarmSettings(getContext());
    }

}
