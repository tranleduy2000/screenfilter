package com.duy.screenfilter.activities;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.duy.screenfilter.BuildConfig;
import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.model.ColorProfile;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.services.TileReceiver;
import com.duy.screenfilter.ui.SchedulerDialog;
import com.duy.screenfilter.utils.AppSetting;
import com.duy.screenfilter.utils.Utility;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class MainActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1001;
    private static final String TAG = "MainActivity";
    public boolean isRunning = false;
    private MaterialAnimatedSwitch mSwitch;
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what < 10) {
                if (mSwitch == null) {
                    mHandler.sendEmptyMessageDelayed(msg.what + 1, 100);
                } else {
                    mSwitch.toggle();
                }
            }
        }

    };
    private DiscreteSeekBar mColorTemp, mIntensity, mDim;
    private ImageButton mSchedulerBtn;
    private PopupMenu popupMenu;
    private AppSetting mSetting;
    private boolean isAnimateRunning;
    private TextView txtColorTemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetting = AppSetting.getInstance(getApplicationContext());
        initWindow();
        changeTheme();
        setContentView(R.layout.activity_setting);

        View animationView = findViewById(R.id.the_animation);
        animationView.setVisibility(View.INVISIBLE);
        bindView();
        animationView.post(new Runnable() {
            @Override
            public void run() {
                startAnimate(true, false);
            }
        });
    }

    private void changeTheme() {
        if (mSetting.getBoolean(AppSetting.KEY_DARK_THEME, false)) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    private void startAnimate(boolean open, final boolean finish) {
        View view = findViewById(R.id.the_animation);
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;
        int radius = (int) Math.hypot(cx, cy);
        Animator animator;
        if (open) {
            animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, radius);
            findViewById(R.id.the_animation).setVisibility(View.VISIBLE);
        } else {
            animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    isAnimateRunning = true;
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    findViewById(R.id.root_layout).setVisibility(View.INVISIBLE);
                    if (finish) MainActivity.super.finish();
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
        }
        animator.start();
    }

    @Override
    public void finish() {
        if (!isAnimateRunning) {
            startAnimate(false, true);
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
                mSwitch.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwitch.toggle();
                    }
                });
            }
        }
        mSwitch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isChecked) {
                if (isChecked) {
                    sendBroadcastStartService();
                } else {
                    sendBroadcastStopService();
                }
            }
        });
        setupSeekBar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }

        ImageButton menuBtn = findViewById(R.id.btn_menu);
        popupMenu = new PopupMenu(this, menuBtn);
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

//        setupScheduler();
        FrameLayout rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupScheduler() {
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
        Intent intent = new Intent();
        intent.setAction(TileReceiver.ACTION_UPDATE_STATUS);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
        sendBroadcast(intent);
        isRunning = false;
    }

    private void sendBroadcastStartService() {
        Intent intent = new Intent();
        intent.setAction(TileReceiver.ACTION_UPDATE_STATUS);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
        sendBroadcast(intent);
        isRunning = true;
    }

    private void setupSeekBar() {
        txtColorTemp = findViewById(R.id.txt_color_temp);
        mColorTemp = findViewById(R.id.seek_bar_temp);
        mIntensity = findViewById(R.id.seek_bar_intensity);
        mDim = findViewById(R.id.seek_bar_dim);

        final ColorProfile colorProfile = mSetting.getColorProfile();
        mColorTemp.setProgress(colorProfile.getColor());
        mIntensity.setProgress(colorProfile.getIntensity());
        mDim.setProgress(colorProfile.getDimLevel());

        mColorTemp.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                if (isRunning) {
                    colorProfile.setColor(value);
                    Intent intent = new Intent(MainActivity.this, MaskService.class);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE);
                    intent.putExtra(Constants.EXTRA_COLOR_PROFILE, colorProfile);
                    startService(intent);
                }
                txtColorTemp.setText((500 + value * 30f) + "k/3500k");
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(colorProfile);
                }
            }
        });
        mIntensity.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                if (isRunning) {
                    colorProfile.setIntensity(value);
                    Intent intent = new Intent(MainActivity.this, MaskService.class);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE);
                    intent.putExtra(Constants.EXTRA_COLOR_PROFILE, colorProfile);
                    startService(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(colorProfile);
                }
            }
        });
        mDim.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                if (isRunning) {
                    colorProfile.setDimLevel(value);
                    Intent intent = new Intent(MainActivity.this, MaskService.class);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE);
                    intent.putExtra(Constants.EXTRA_COLOR_PROFILE, colorProfile);
                    startService(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.saveColorProfile(colorProfile);
                }
            }
        });
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSwitch = null;
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
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.action_reading_mode:
                mColorTemp.setProgress(20);
                mIntensity.setProgress(60);
                mDim.setProgress(78);
                break;
            case R.id.action_dim_mode:
                mColorTemp.setProgress(0);
                mIntensity.setProgress(0);
                mDim.setProgress(60);
                break;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    refresh();
                }
            }
        }
    }

    private void refresh() {
        mSwitch.toggle();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwitch.toggle();
            }
        }, 500);
    }
}
