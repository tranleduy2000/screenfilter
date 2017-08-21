package com.duy.screenfilter.ui.shortcut;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.duy.screenfilter.Constants;
import com.duy.screenfilter.R;
import com.duy.screenfilter.services.TileReceiver;
import com.duy.screenfilter.services.MaskService;

public class ToggleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            Intent intent = new Intent();
            Parcelable icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_shortcut_switch);

            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_label_switch));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

            Intent launchIntent = new Intent(getApplicationContext(), ToggleActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);

            setResult(RESULT_OK, intent);
            finish();
        } else {
            Intent i = new Intent(this, MaskService.class);
            i.putExtra(Constants.EXTRA_ACTION, Constants.ACTION_CHECK);
            i.putExtra(Constants.EXTRA_CHECK_FROM_TOGGLE, true);
            startService(i);
            finish();
        }

    }

    public static class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int eventId = intent.getIntExtra(Constants.EXTRA_EVENT_ID, -1);
            switch (eventId) {
                case Constants.EVENT_CHECK:
                    Intent i = new Intent();
                    i.setAction(TileReceiver.ACTION_UPDATE_STATUS);
                    i.putExtra(Constants.EXTRA_ACTION, intent.getBooleanExtra("isShowing", false) ? Constants.ACTION_STOP : Constants.ACTION_START);
                    context.sendBroadcast(i);
                    break;
            }
        }

    }

}
