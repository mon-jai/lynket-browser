package arun.com.chromer.chrometabutilites;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import java.util.List;

import arun.com.chromer.util.PrefUtil;
import arun.com.chromer.util.Util;
import timber.log.Timber;

/**
 * Created by Arun on 18/12/2015.
 */
public class MyCustomActivityHelper implements ServiceConnectionCallback {

    private static final String TAG = MyCustomActivityHelper.class.getSimpleName();

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;
    private ConnectionCallback mConnectionCallback;
    private NavigationCallback mNavigationCallback;

    /**
     * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView.
     *
     * @param activity         The host activity.
     * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available.
     * @param uri              the Uri to be opened.
     */
    @SuppressWarnings("SameParameterValue")
    public static void openCustomTab(Activity activity, CustomTabsIntent customTabsIntent, Uri uri) {
        // The package name to use
        String packageName;

        // first check user preferred custom provider is there
        String userPrefProvider = PrefUtil.getPreferredTabApp(activity);
        if (userPrefProvider != null && Util.isPackageInstalled(activity, userPrefProvider)) {
            Timber.d("Valid user preferred custom tab provider present");
            packageName = userPrefProvider;
        } else {
            // getting all the packages here
            Timber.d("Valid user choice not present, defaulting to conventional method");
            packageName = MyCustomTabHelper.getPackageNameToUse(activity);
        }

        //If we cant find a package name, it means there's no browser that supports
        //Chrome Custom Tabs installed. So, we fallback to the webview
        if (packageName == null) {
            throw new Error("No package found!");
        }

        customTabsIntent.intent.setPackage(packageName);
        try {
            customTabsIntent.launchUrl(activity, uri);
            Timber.d("Launched url:" + uri);
        } catch (Exception e) {
            Timber.d("Called fallback even though package was found, weird Exception :" + e);
        }
    }

    private static void callFallback(Activity activity, Uri uri, CustomTabsFallback fallback) {
        if (fallback != null) {
            fallback.openUri(activity, uri);
        }
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service.
     *
     * @param context the activity that is connected to the service.
     */
    public void unbindCustomTabsService(Context context) {
        Timber.d("Attempting to unbind service!");
        if (mConnection == null) return;
        context.unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
        mConnection = null;
        Timber.d("Unbounded service!");
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    @SuppressWarnings("WeakerAccess")
    public CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(mNavigationCallback);
        }
        return mCustomTabsSession;
    }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service.
     *
     * @param connectionCallback
     */
    public void setConnectionCallback(ConnectionCallback connectionCallback) {
        this.mConnectionCallback = connectionCallback;
    }

    /**
     * Register a Callback to be called when Navigation occurs..
     *
     * @param navigationCallback
     */
    public void setNavigationCallback(NavigationCallback navigationCallback) {
        this.mNavigationCallback = navigationCallback;
    }

    /**
     * Binds the Activity to the Custom Tabs Service.
     *
     * @param context the activity to be binded to the service.
     */
    public boolean bindCustomTabsService(Context context) {
        Timber.d("Attempting to bind custom tabs service");
        if (mClient != null) return false;

        String packageName = MyCustomTabHelper.getPackageNameToUse(context);
        if (packageName == null) return false;

        mConnection = new ServiceConnection(this);
        boolean ok = CustomTabsClient.bindCustomTabsService(context, packageName, mConnection);
        if (ok) {
            Timber.d("Bound successfully");
        } else
            Timber.d("Did not bind, something wrong");
        return ok;
    }

    @SuppressWarnings("SameParameterValue")
    public boolean mayLaunchUrl(Uri uri, Bundle extras, List<Bundle> otherLikelyBundles) {
        Timber.d("Attempting may launch url");
        if (mClient == null) return false;

        CustomTabsSession session = getSession();
        if (session == null) return false;

        boolean ok = session.mayLaunchUrl(uri, extras, otherLikelyBundles);
        if (ok) {
            Timber.d("Successfully warmed up with may launch URL:" + uri.toString());
        } else {
            Timber.d("May launch url was a failure for " + uri.toString());
        }
        return ok;
    }

    @Override
    public void onServiceConnected(CustomTabsClient client) {
        Timber.d("Service connected properly!");
        mClient = client;
        mClient.warmup(0L);
        if (mConnectionCallback != null) mConnectionCallback.onCustomTabsConnected();
    }


    public boolean requestWarmUp() {
        boolean x = false;
        if (mClient != null) {
            x = mClient.warmup(0L);
            Timber.d("Warmup status " + x);
        }
        return false;
    }

    @Override
    public void onServiceDisconnected() {
        Timber.d("Service disconnected!");
        mClient = null;
        mCustomTabsSession = null;
        if (mConnectionCallback != null) mConnectionCallback.onCustomTabsDisconnected();
    }

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected.
     */
    public interface ConnectionCallback {
        /**
         * Called when the service is connected.
         */
        void onCustomTabsConnected();

        /**
         * Called when the service is disconnected.
         */
        void onCustomTabsDisconnected();
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available.
     */
    public interface CustomTabsFallback {
        /**
         * @param activity The Activity that wants to open the Uri.
         * @param uri      The uri to be opened by the fallback.
         */
        void openUri(Activity activity, Uri uri);
    }

    public static class NavigationCallback extends CustomTabsCallback {
        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {
            // Log.w(TAG, "onNavigationEvent: Code = " + navigationEvent);
        }
    }
}
