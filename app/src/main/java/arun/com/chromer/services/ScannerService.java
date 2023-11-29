package arun.com.chromer.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arun.com.chromer.chrometabutilites.MyCustomActivityHelper;
import arun.com.chromer.util.PrefUtil;
import timber.log.Timber;

/**
 * Created by Arun on 06/01/2016.
 */
public class ScannerService extends AccessibilityService implements MyCustomActivityHelper.ConnectionCallback {

    private static ScannerService mScannerService = null;
    private String lastWarmedUpUrl = "";
    private MyCustomActivityHelper myCustomActivityHelper;

    public static ScannerService getInstance() {
        return mScannerService;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mScannerService = this;
        myCustomActivityHelper = new MyCustomActivityHelper();
        myCustomActivityHelper.setConnectionCallback(this);
        myCustomActivityHelper.setNavigationCallback(new MyCustomActivityHelper.NavigationCallback() {
            // Do nothing
        });
        boolean success = myCustomActivityHelper.bindCustomTabsService(this);
        Timber.d("Was binded " + success);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mScannerService = null;
        myCustomActivityHelper.unbindCustomTabsService(this);
        Timber.d("Unbinding");
        return super.onUnbind(intent);
    }

    public CustomTabsSession getTabSession() {
        if (myCustomActivityHelper != null) {
            return myCustomActivityHelper.getSession();
        }
        return null;
    }


    public boolean mayLaunchUrl(Uri uri) {
        boolean ok = myCustomActivityHelper.mayLaunchUrl(uri, null, null);
        Timber.d("Warmup " + ok);
        return ok;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mScannerService = this;

        if (PrefUtil.isPreFetchPrefered(this) && shouldHonourWifi()) {
            try {
                stopService(new Intent(this, WarmupService.class));
            } catch (Exception e) {
            }
            TextProcessorTask mTextProcessor = new TextProcessorTask(getRootInActiveWindow());

            mTextProcessor.execute();
        } else {
            // Do nothing
        }
    }

    @Override
    public void onInterrupt() {
        // Nothing
    }

    @Override
    public void onCustomTabsConnected() {
        Timber.d("connected");
    }

    @Override
    public void onCustomTabsDisconnected() {

    }

    public boolean shouldHonourWifi() {
        if (PrefUtil.isWifiPreferred(this)) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            // TODO fix this deprecated call
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return mWifi.isConnected();
        } else
            return true;
    }

    private class TextProcessorTask extends AsyncTask<Void, String, Void> {
        private final AccessibilityNodeInfo info;
        private final Stack<AccessibilityNodeInfo> tree;
        private final int maxUrl = 4;
        private final List<String> urls = new ArrayList<>();
        private int extractedCount = 0;

        TextProcessorTask(AccessibilityNodeInfo nodeInfo) {
            info = nodeInfo;
            tree = new Stack<>();
        }

        private void actOnCurrentNode(AccessibilityNodeInfo node) {
            if (node != null && node.getText() != null) {
                String currNodeText = node.getText().toString();
                // Now attempt to get all the URLS in this string
                extractURL(currNodeText);
                if (urls != null && urls.size() != 0) {
                    extractedCount += urls.size();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Timber.d("Background");
            if (info == null) return null;

            tree.push(info);

            while (!tree.empty() && extractedCount < maxUrl) {
                AccessibilityNodeInfo currNode = tree.pop();
                if (currNode != null) {
                    actOnCurrentNode(currNode);
                    for (int i = 0; i < currNode.getChildCount(); i++) {
                        tree.push(currNode.getChild(i));
                    }
                }
            }
            Timber.d("End");
            return null;
        }

        void extractURL(String string) {
            if (string == null) {
                return;
            }
            Matcher m = Pattern.compile("\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))", Pattern.CASE_INSENSITIVE)
                    .matcher(string);
            while (m.find()) {
                String url = m.group();
                if (!url.toLowerCase().matches("^\\w+://.*")) {
                    url = "http://" + url;
                }
                urls.add(url);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Timber.d("On post execute");
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                Timber.d("Extracted " + url);
            }

            Collections.reverse(urls);

            if (urls.size() != 0) {
                int first = 0;
                String priorityUrl = null;
                List<Bundle> possibleUrls = new ArrayList<>();
                for (String url : urls) {
                    if (first == 0) {
                        priorityUrl = url;
                        first++;
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(CustomTabsService.KEY_URL, Uri.parse(url));
                        possibleUrls.add(bundle);
                    }
                    boolean success;
                    if (!priorityUrl.equalsIgnoreCase(lastWarmedUpUrl)) {
                        success = myCustomActivityHelper.mayLaunchUrl(Uri.parse(priorityUrl), null, possibleUrls);
                        if (success) lastWarmedUpUrl = priorityUrl;
                    } else {
                        Timber.d("Ignored, already warmed up");
                    }
                }
            }

        }
    }
}
