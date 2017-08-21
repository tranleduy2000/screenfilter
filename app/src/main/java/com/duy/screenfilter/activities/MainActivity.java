package com.duy.screenfilter.activities;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.adapter.ColorAdapter;
import com.duy.screenfilter.adapter.ModeListAdapter;
import com.duy.screenfilter.services.MaskService;
import com.duy.screenfilter.ui.SchedulerDialog;
import com.duy.screenfilter.utils.AppSetting;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import static com.duy.screenfilter.services.TileReceiver.ACTION_UPDATE_STATUS;

public class MainActivity extends Activity implements PopupMenu.OnMenuItemClickListener, ColorAdapter.OnColorClickListener {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1001;
    public static boolean isRunning = false;
    private static MaterialAnimatedSwitch mSwitch;
    private static final Handler mHandler = new Handler() {

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
    private boolean hasDismissFirstRunDialog = false;
    private DiscreteSeekBar mSeekbar;
    private TextView mModeText;
    private ImageButton mSchedulerBtn;
    private PopupMenu popupMenu;
    private AlertDialog mAlertDialog, mModeDialog;
    private int targetMode;
    private AppSetting mSetting;
    private boolean isAnimateRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetting = AppSetting.getInstance(getApplicationContext());
        initWindow();
        if (mSetting.getBoolean(AppSetting.KEY_DARK_THEME, false)) {
            setTheme(R.style.AppTheme_Dark);
        }
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
                    isAnimateRunning = false;
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

        Intent i = new Intent(this, MaskService.class);
        startService(i);

        mSwitch = findViewById(R.id.toggle);
        mSwitch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean b) {
                if (b) {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_UPDATE_STATUS);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_START);
                    intent.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, true);
                    sendBroadcast(intent);
                    isRunning = true;

                    // For safe
                    if (mSetting.getBoolean(AppSetting.KEY_FIRST_RUN, true)) {
                        if (mAlertDialog != null && mAlertDialog.isShowing()) {
                            return;
                        }
                        hasDismissFirstRunDialog = false;
                        mAlertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.dialog_first_run_title)
                                .setMessage(R.string.dialog_first_run_message)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        hasDismissFirstRunDialog = true;
                                        mSetting.putBoolean(AppSetting.KEY_FIRST_RUN, false);
                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        if (hasDismissFirstRunDialog) return;
                                        hasDismissFirstRunDialog = true;
                                        mSwitch.toggle();
                                        if (mSetting.getBoolean(AppSetting.KEY_FIRST_RUN, true)) {
                                            Intent intent = new Intent(MainActivity.this, MaskService.class);
                                            intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
                                            stopService(intent);
                                            isRunning = false;
                                        }
                                    }
                                })
                                .show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mAlertDialog.isShowing() && !hasDismissFirstRunDialog) {
                                    mAlertDialog.dismiss();
                                }
                            }
                        }, 5000);
                    }
                } else {
                    Intent intent = new Intent(MainActivity.this, MaskService.class);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_STOP);
                    intent.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, true);
                    stopService(intent);
                    isRunning = false;
                }
            }
        });

        mSeekbar = findViewById(R.id.seek_bar);
        mSeekbar.setProgress(mSetting.getInt(AppSetting.KEY_BRIGHTNESS, 50));
        mSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            int v = -1;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                v = value;
                if (isRunning) {
                    Intent intent = new Intent(MainActivity.this, MaskService.class);
                    intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE_BRIGHTNESS);
                    intent.putExtra(Constants.EXTRA_BRIGHTNESS, mSeekbar.getProgress());
                    intent.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, true);
                    startService(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (v != -1) {
                    mSetting.putInt(AppSetting.KEY_BRIGHTNESS, v);
                }
            }
        });

        mModeText = findViewById(R.id.mode_view);
        int mode = mSetting.getInt(AppSetting.KEY_MODE, Constants.MODE_NORMAL);
        String msg = getResources().getStringArray(R.array.mode_text)[mode]
                + ((mode == Constants.MODE_NORMAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ? " " + getString(R.string.mode_text_no_permission_warning)
                : "");
        mModeText.setText(msg);
        findViewById(R.id.mode_view_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = mSetting.getInt(AppSetting.KEY_MODE, Constants.MODE_NORMAL);
                mModeDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.dialog_choose_mode)
                        .setSingleChoiceItems(
                                new ModeListAdapter(current), current,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // http://stackoverflow.com/questions/32061934/permission-from-manifest-doesnt-work-in-android-6/32065680#32065680
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (!Settings.canDrawOverlays(MainActivity.this)) {
                                                targetMode = which;
                                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:" + getPackageName()));
                                                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                                            } else {
                                                applyNewMode(which);
                                            }
                                        } else {
                                            targetMode = which;
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setTitle(R.string.dialog_overlay_enable_title)
                                                    .setMessage(R.string.dialog_overlay_enable_message)
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            applyNewMode(targetMode);
                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            // Do nothing....
                                                        }
                                                    })
                                                    .show();
                                        }
                                        mModeDialog.dismiss();
                                    }
                                }
                        )
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

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

        RecyclerView recyclerView = findViewById(R.id.container_color);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new ColorAdapter(this, this));

        FrameLayout rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
        mSetting.putInt(AppSetting.KEY_BRIGHTNESS, mSeekbar.getProgress());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSwitch = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    applyNewMode(targetMode);
                }
            }
        }
    }


    private void applyNewMode(int targetMode) {
        if (isRunning && targetMode != mSetting.getInt(AppSetting.KEY_MODE, Constants.MODE_NORMAL)) {
            mSwitch.toggle();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSwitch.toggle();
                }
            }, 500);
        }
        mSetting.putInt(AppSetting.KEY_MODE, targetMode);
        String msg = getResources().getStringArray(R.array.mode_text)[targetMode]
                + ((targetMode == Constants.MODE_NORMAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ? " " + getString(R.string.mode_text_no_permission_warning)
                : "");
        mModeText.setText(msg);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_dark_theme) {
            mSetting.putBoolean(AppSetting.KEY_DARK_THEME, !menuItem.isChecked());
            menuItem.setChecked(!menuItem.isChecked());
            finish();
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public void onColorClicked(int color) {
        mSetting.setFilterColor(color);
        if (isRunning) {
            Intent intent = new Intent(this, MaskService.class);
            intent.setAction(ACTION_UPDATE_STATUS);
            intent.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_UPDATE_COLOR);
            intent.putExtra(Constants.EXTRA_COLOR, color);
            intent.putExtra(Constants.EXTRA_DO_NOT_SEND_CHECK, true);
            startService(intent);
        }
    }

    public static class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSwitch == null) return;
            int eventId = intent.getIntExtra(Constants.EXTRA_EVENT_ID, -1);
            switch (eventId) {
                case Constants.EVENT_CANNOT_START:
                    // Receive a error from MaskService
                    isRunning = false;
                    mSwitch.toggle();
                    Toast.makeText(context, R.string.mask_fail_to_start, Toast.LENGTH_LONG).show();
                    break;
                case Constants.EVENT_DESTORY_SERVICE:
                    if (isRunning) {
                        mSwitch.toggle();
                        isRunning = false;
                    }
                    break;
                case Constants.EVENT_CHECK:
                    Log.i("C", "Checked" + intent.getBooleanExtra("isShowing", false));
                    if (isRunning = intent.getBooleanExtra("isShowing", false) != mSwitch.isChecked()) {
                        // If I don't use postDelayed, Switch may cause a NPE because its animator wasn't initialized.
                        mHandler.sendEmptyMessageDelayed(1, 100);
                    }
                    break;
            }
        }

    }

}
