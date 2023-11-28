package arun.com.chromer.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import arun.com.chromer.R;
import arun.com.chromer.chrometabutilites.CustomTabDelegate;
import arun.com.chromer.chrometabutilites.MyCustomActivityHelper;
import arun.com.chromer.util.PrefUtil;
import arun.com.chromer.util.Util;

public class TabActivity extends Activity {

    public final static MyCustomActivityHelper.CustomTabsFallback mCustomTabsFallback =
            new MyCustomActivityHelper.CustomTabsFallback() {
                @Override
                public void openUri(Activity activity, Uri uri) {
                    if (activity != null) {
                        Toast.makeText(activity,
                                activity.getString(R.string.fallback_msg),
                                Toast.LENGTH_SHORT).show();
                        try {
                            activity.startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_VIEW, uri),
                                    activity.getString(R.string.open_with)));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(activity,
                                            activity.getString(R.string.unxp_err), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
            };
    private static final String TAG = TabActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();

        if (PrefUtil.isFirstRun(context)) {
            Intent intent = new Intent();
            intent.setClass(context, arun.com.chromer.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            finish();
            return;
        }

        if (getIntent() == null || getIntent().getData() == null) {
            Toast.makeText(this, getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
            finish();
            // TODO handle no intent later
            return;
        }
        final String url = getIntent().getData().toString();
        CustomTabsIntent mCustomTabsIntent = CustomTabDelegate.getCustomizedTabIntent(
                getApplicationContext(), url);
        MyCustomActivityHelper.openCustomTab(this, mCustomTabsIntent,
                Uri.parse(getIntent().getData().toString()));
        finish();
    }

    boolean shouldBind() {
        if (PrefUtil.isPreFetchPrefered(this) && Util.isAccessibilityServiceEnabled(this)) {
            return false;
        } else if (!PrefUtil.isPreFetchPrefered(this))
            return true;
        return true;
    }
}