package com.duy.screenfilter.activities;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import com.duy.screenfilter.BuildConfig;
import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.model.ColorProfile;
import com.duy.screenfilter.monitor.CurrentAppMonitoringThread;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.ui.SchedulerDialog;
import com.duy.screenfilter.utils.AppSetting;
import com.duy.screenfilter.utils.Utility;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_USAGE_ACCESS = 1002;

    private static final String TAG = "MainActivity";
    public boolean isRunning = false;
    private Switch mSwitch;
    private DiscreteSeekBar mColorTemp, mIntensity, mDim;
    private ImageButton mSchedulerBtn;
    private AppSetting mSetting;
    private boolean isAnimateRunning;
    private TextView txtColorTemp;
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private ColorProfile mColorProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetting = AppSetting.newInstance(getApplicationContext());
        initWindow();
        changeTheme();
        setContentView(R.layout.activity_setting);

        View animationView = findViewById(R.id.the_animation);
        animationView.setVisibility(View.INVISIBLE);
        bindView();
        animationView.post(new Runnable() {
            @Override
            public void run() {
                startAnimate();
            }
        });

        checkPermission();
    }

    private void closeAnimate(int x, int y) {
        final View view = findViewById(R.id.the_animation);
        int cx = x == -1 ? view.getWidth() / 2 : x;
        int cy = y == -1 ? view.getHeight() / 2 : y;
        int radius = (int) Math.hypot(cx, cy);
        Animator animator;
        animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0);
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimateRunning = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.INVISIBLE);
                MainActivity.super.finish();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();

    }

    private void changeTheme() {
        if (mSetting.getBoolean(AppSetting.KEY_DARK_THEME, false)) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    private void startAnimate() {
        View view = findViewById(R.id.the_animation);
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;
        int radius = (int) Math.hypot(cx, cy);
        Animator animator;
        animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, radius);
        findViewById(R.id.the_animation).setVisibility(View.VISIBLE);
        animator.start();
    }

    @Override
    public void finish() {
        if (!isAnimateRunning) {
            closeAnimate(-1, -1);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void bindView() {
        mSwitch = findViewById(R.id.toggle);
        if (Utility.isScreenFilterServiceRunning(this)) {
            if (!mSwitch.isChecked()) {
                mSwitch.setChecked(true);
            } else {
                mSwitch.setChecked(false);
            }
        }
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    sendBroadcastStartService();
                } else {
                    sendBroadcastStopService();
                }
            }
        });
        setupSeekBar();
        ImageButton menuBtn = findViewById(R.id.btn_menu);
        final PopupMenu popupMenu = new PopupMenu(this, menuBtn);
        popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());
        popupMenu.getMenu()
                .findItem(R.id.action_dark_theme)
                .setChecked(mSetting.getBoolean(AppSetting.KEY_DARK_THEME, false));
        popupMenu.setOnMenuItemClickListener(this);
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });
        menuBtn.setOnTouchListener(popupMenu.getDragToOpenListener());

        setupSchedulerDialog();
        FrameLayout rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    closeAnimate((int) motionEvent.getX(), (int) motionEvent.getY());
                    return true;
                }
                return false;
            }
        });

    }

    private void checkPermission() {
        Log.d(TAG, "checkPermission() called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.need_permission)
                        .setMessage(R.string.msg_draw_permission)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Uri uri = Uri.parse("package:" + getPackageName());
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                dialogInterface.cancel();
                            }
                        })
                        .create()
                        .show();
                return;
            }
        }
        if (CurrentAppMonitoringThread.getCurrentAppUsingUsageStats(this) == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.need_permission)
                    .setMessage(R.string.msg_auto_suspend)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            startActivityForResult(intent, REQUEST_USAGE_ACCESS);
                            dialogInterface.cancel();
                        }
                    })
                    .create()
                    .show();
        }
    }


    private void setupSchedulerDialog() {
        mSchedulerBtn = findViewById(R.id.btn_scheduler);
        if (mSetting.getBoolean(AppSetting.KEY_AUTO_MODE, false)) {
            mSchedulerBtn.setImageResource(R.drawable.ic_alarm_black_24dp);
        } else {
            mSchedulerBtn.setImageResource(R.drawable.ic_alarm_off_black_24dp);
        }
        mSchedulerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManager pm = getSystemService(PowerManager.class);
                    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.dialog_ignore_battery_opt_title)
                                .setMessage(R.string.dialog_ignore_battery_opt_msg)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        showSchedulerDialog();
                                    }
                                })
                                .setNeutralButton(R.string.dialog_button_go_to_set, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                            intent.setData(Uri.parse("package:" + getPackageName()));
                                            startActivity(intent);
                                        } catch (ActivityNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .show();
                        return;
                    }
                    showSchedulerDialog();
                } else {
                    showSchedulerDialog();
                }
            }
        });

    }

    private void sendBroadcastStopService() {
        Intent intent = new Intent(this, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
        startService(intent);
        isRunning = false;
        mSetting.setRunning(false);
    }

    private void sendBroadcastStartService() {
        Utility.updateAlarmSettings(this);
        Intent intent = new Intent(this, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
        intent.putExtra(Constants.EXTRA_COLOR_PROFILE, mColorProfile);
        startService(intent);
        isRunning = true;
        mSetting.setRunning(true);
    }

    private void setupSeekBar() {
        txtColorTemp = findViewById(R.id.txt_color_temp);
        mColorTemp = findViewById(R.id.seek_bar_temp);
        mIntensity = findViewById(R.id.seek_bar_intensity);
        mDim = findViewById(R.id.seek_bar_dim);

        mColorProfile = mSetting.getColorProfile();
        mColorTemp.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                mColorProfile.setColor(value);
                if (isRunning) if (fromUser) updateColorFilter();
                txtColorTemp.setText((500 + value * 30) + "k/3500k");
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(mColorProfile);
                }
            }
        });
        mIntensity.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                mColorProfile.setIntensity(value);
                if (isRunning) if (fromUser) updateColorFilter();
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(mColorProfile);
                }
            }
        });
        mDim.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                mColorProfile.setDimLevel(value);
                if (isRunning) if (fromUser) updateColorFilter();
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(mColorProfile);
                }
            }
        });

        mColorTemp.setProgress(mColorProfile.getColor());
        mIntensity.setProgress(mColorProfile.getIntensity());
        mDim.setProgress(mColorProfile.getDimLevel());
    }

    private void updateColorFilter() {
        Intent intent = new Intent(MainActivity.this, MaskService.class);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE);
        intent.putExtra(Constants.EXTRA_COLOR_PROFILE, mColorProfile);
        startService(intent);
    }

    private void initWindow() {
        // Don't worry too much. Min SDK is 21.
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    private void showSchedulerDialog() {
        new SchedulerDialog(this, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mSetting.getBoolean(AppSetting.KEY_AUTO_MODE, false)) {
                    mSchedulerBtn.setImageResource(R.drawable.ic_alarm_black_24dp);
                } else {
                    mSchedulerBtn.setImageResource(R.drawable.ic_alarm_off_black_24dp);
                }
            }
        }).show();
    }

    @Override
    public boolean onMenuItemClick(final MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_dark_theme:
                mSetting.putBoolean(AppSetting.KEY_DARK_THEME, !menuItem.isChecked());
                menuItem.setChecked(!menuItem.isChecked());
                recreate();
                return true;
            case R.id.action_playstore:
                try {
                    Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.action_reading_mode: //20 60 78
                mColorTemp.setProgress(20);
                mIntensity.setProgress(60);
                mDim.setProgress(78);
                if (isRunning) updateColorFilter();
                else sendBroadcastStartService();
                break;
            case R.id.action_dim_mode: //0 0 60
                mColorTemp.setProgress(0);
                mIntensity.setProgress(0);
                mDim.setProgress(60);
                if (isRunning) updateColorFilter();
                else sendBroadcastStartService();
                break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_START);
        intentFilter.addAction(Constants.ACTION_STOP);
        intentFilter.addAction(Constants.ACTION_UPDATE_FROM_NOTIFICATION);
        registerReceiver(mStatusReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStatusReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    mSwitch.post(new Runnable() {
                        @Override
                        public void run() {
                            mSwitch.setChecked(true);
                        }
                    });
                } else {
                    finish();
                }
            }
        } else if (requestCode == REQUEST_USAGE_ACCESS) {
            if (CurrentAppMonitoringThread.getCurrentAppUsingUsageStats(this) == null) {
                finish();
            }
        }
    }

    private class StatusReceiver extends BroadcastReceiver {
        private static final String TAG = "StatusReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");

            if (intent != null) {
                if (intent.getAction().equals(Constants.ACTION_UPDATE_FROM_NOTIFICATION)) {
                    String action = intent.getStringExtra(Constants.EXTRA_ACTION);
                    switch (action) {
                        case Constants.ACTION_START:
                            if (!mSwitch.isChecked()) mSwitch.setChecked(true);
                            break;
                        case Constants.ACTION_STOP:
                            if (mSwitch.isChecked()) mSwitch.setChecked(false);
                            break;
                    }
                }
            }
        }
    }
}
