package arun.com.chromer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import arun.com.chromer.chrometabutilites.CustomTabDelegate;
import arun.com.chromer.chrometabutilites.MyCustomActivityHelper;
import arun.com.chromer.chrometabutilites.MyCustomTabHelper;
import arun.com.chromer.extra.Licenses;
import arun.com.chromer.services.ScannerService;
import arun.com.chromer.services.WarmupService;
import arun.com.chromer.util.PrefUtil;
import arun.com.chromer.util.Util;
import de.psdev.licensesdialog.LicensesDialog;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final String GOOGLE_URL = "http://www.google.com/";
    private static final String CUSTOM_TAB_URL = "https://developer.chrome.com/multidevice/android/customtabs#whentouse";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    private MyCustomActivityHelper mCustomTabActivityHelper;

    private View mColorView;
    private SwitchCompat mWarmUpSwitch;
    private SwitchCompat mPrefetchSwitch;
    private SwitchCompat mWifiSwitch;
    private ImageView mSecondaryBrowser;
    private SwitchCompat mDynamicSwitch;

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldBind()) {
            mCustomTabActivityHelper.bindCustomTabsService(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mCustomTabActivityHelper.unbindCustomTabsService(this);
        } catch (Exception e) {
            /* Best effort */
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences mPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        setupFAB();

        setupCustomTab();

        findViewById(R.id.set_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDefaultBehaviour();
            }
        });

        setupDefaultProvider();

        setUpSecondaryBrowser();

        checkAndEducateUser();

        populateUIBasedOnPreferences();

        takeCareOfServices();
    }

    private void setUpSecondaryBrowser() {
        mSecondaryBrowser = findViewById(R.id.secondary_browser_view);

        try {
            mSecondaryBrowser.setImageDrawable(
                    getPackageManager()
                            .getApplicationIcon(PrefUtil.getSecondaryPref(this)));
        } catch (PackageManager.NameNotFoundException e) {

        }

        View click = findViewById(R.id.secondary_browser);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
                List<ResolveInfo> resolvedActivityList = getPackageManager()
                        .queryIntentActivities(activityIntent, PackageManager.MATCH_ALL);
                final List<String> packages = new ArrayList<>();
                for (ResolveInfo info : resolvedActivityList) {
                    if (!info.activityInfo.packageName.equalsIgnoreCase(getPackageName()))
                        packages.add(info.activityInfo.packageName);
                }
                String[] pack = Util.getAppNameFromPackages(MainActivity.this, packages);
                int choice = -1;

                String secondaryPref = PrefUtil.getSecondaryPref(MainActivity.this);
                if (Util.isPackageInstalled(getApplicationContext(), secondaryPref)) {
                    choice = packages.indexOf(secondaryPref);
                }

                new MaterialDialog.Builder(MainActivity.this)
                        .title(getString(R.string.choose_secondary_browser))
                        .items(pack)
                        .itemsCallbackSingleChoice(choice,
                                new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View itemView,
                                                               int which, CharSequence text) {
                                        if (packages != null) {
                                            PrefUtil.setSecondaryPref(MainActivity.this,
                                                    packages.get(which));
                                            try {
                                                mSecondaryBrowser.setImageDrawable(
                                                        getPackageManager()
                                                                .getApplicationIcon(packages.get(which)));
                                            } catch (PackageManager.NameNotFoundException e) {
                                                // Ignore, should not happen
                                            }
                                        }
                                        return true;
                                    }
                                })
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        linkAccessiblityAndPrefetch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_license) {
            new LicensesDialog.Builder(this)
                    .setNotices(Licenses.getNotices())
                    .setTitle(R.string.licenses)
                    .build()
                    .show();

            return true;
        } else {
            // The user's action isn't recognized.
            // Invoke the superclass to handle it.
            return super.onOptionsItemSelected(item);

        }
    }

    private void populateUIBasedOnPreferences() {
        linkAccessiblityAndPrefetch();

        mWarmUpSwitch = findViewById(R.id.warm_up_switch);
        mWarmUpSwitch.setChecked(PrefUtil.isWarmUpPreferred(this));
        mWarmUpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtil.setWarmUpPreference(MainActivity.this, isChecked);
                takeCareOfServices();
            }
        });

        mPrefetchSwitch = findViewById(R.id.pre_fetch_switch);
        mPrefetchSwitch.setChecked(PrefUtil.isPreFetchPrefered(this));
        linkWarmAndPrefetch(PrefUtil.isPreFetchPrefered(this));
        mPrefetchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean warmup = !isChecked && PrefUtil.isWarmUpPreferred(MainActivity.this);

                if (!Util.isAccessibilityServiceEnabled(MainActivity.this)) {
                    mPrefetchSwitch.setChecked(false);
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.accesiblity_dialog_title)
                            .content(R.string.accesiblity_dialog_desc)
                            .positiveText(R.string.open_settings)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                                }
                            })
                            .show();
                } else {
                    mWarmUpSwitch.setChecked(warmup);
                    PrefUtil.setWarmUpPreference(MainActivity.this, warmup);
                    linkWarmAndPrefetch(isChecked);
                }
                PrefUtil.setPrefetchPreference(MainActivity.this, isChecked);
                takeCareOfServices();
            }
        });

        mWifiSwitch = findViewById(R.id.only_wifi_switch);
        mWifiSwitch.setChecked(PrefUtil.isWifiPreferred(this));
        mWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtil.setWifiPrefetch(MainActivity.this, isChecked);
                takeCareOfServices();
            }
        });
    }

    private void linkAccessiblityAndPrefetch() {
        if (Util.isAccessibilityServiceEnabled(this)) {
            Timber.d("Scanning permission granted");
            if (mPrefetchSwitch != null)
                mPrefetchSwitch.setChecked(PrefUtil.isPreFetchPrefered(this));
        } else {
            // Turn off preference
            if (mPrefetchSwitch != null)
                mPrefetchSwitch.setChecked(false);
            PrefUtil.setPrefetchPreference(MainActivity.this, false);
        }
    }

    private void linkWarmAndPrefetch(boolean isChecked) {
        mWarmUpSwitch.setEnabled(!isChecked);
    }

    private void takeCareOfServices() {
        if (PrefUtil.isWarmUpPreferred(this))
            startService(new Intent(this, WarmupService.class));
        else
            stopService(new Intent(this, WarmupService.class));

        try {
            if (PrefUtil.isPreFetchPrefered(this))
                startService(new Intent(this, ScannerService.class));
            else
                stopService(new Intent(this, ScannerService.class));
        } catch (Exception e) {
            Timber.d("Ignoring startup exception of accessibility service");
        }

    }

    private void setupDefaultProvider() {
        findViewById(R.id.default_provider).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] packagesArray = new String[0];
                final List<String> suppPackages = MyCustomTabHelper.
                        getCustomTabSupportingPackages(getApplicationContext());
                if (suppPackages != null) {
                    packagesArray = Util.getAppNameFromPackages(getApplicationContext(), suppPackages);
                }
                int choice = -1;
                String pack = PrefUtil.getPreferredTabApp(MainActivity.this);
                if (suppPackages != null && Util.isPackageInstalled(getApplicationContext(),
                        pack)) {
                    choice = suppPackages.indexOf(pack);
                }
                new MaterialDialog.Builder(MainActivity.this)
                        .title(getString(R.string.choose_default_provider))
                        .items(packagesArray)
                        .itemsCallbackSingleChoice(choice,
                                new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View itemView,
                                                               int which, CharSequence text) {
                                        if (suppPackages != null) {
                                            PrefUtil.setPreferredTabApp(MainActivity.this,
                                                    suppPackages.get(which));
                                        }
                                        return true;
                                    }
                                })
                        .show();
            }
        });
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCustomTab(GOOGLE_URL);
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private void handleDefaultBehaviour() {
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_URL));
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(launchIntent,
                PackageManager.MATCH_DEFAULT_ONLY);

        String packageName = resolveInfo != null ? resolveInfo.activityInfo.packageName : "";
        if (packageName != null) {
            if (packageName.trim().equalsIgnoreCase(getPackageName())) {
                Timber.d("Chromer defaulted");
                Snackbar.make(mColorView, "Already set!", Snackbar.LENGTH_SHORT).show();
            } else if (packageName.equalsIgnoreCase("android") && Util.isPackageInstalled(this, packageName)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_URL)));
            } else {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse(
                        "package:" + packageName));
                Toast.makeText(this,
                        Util.getAppNameWithPackage(this, packageName)
                                + " "
                                + getString(R.string.default_clear_msg), Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        }
    }

    private void launchCustomTab(String url) {
        CustomTabsIntent mCustomTabsIntent = CustomTabDelegate.getCustomizedTabIntent(getApplicationContext(), url);
        MyCustomActivityHelper.openCustomTab(this, mCustomTabsIntent, Uri.parse(url));
    }

    private void setupCustomTab() {
        mCustomTabActivityHelper = new MyCustomActivityHelper();

        if (!shouldBind()) {
            try {
                boolean ok = ScannerService.getInstance().mayLaunchUrl(Uri.parse(GOOGLE_URL));
                if (ok) {
                    return;
                }
            } catch (Exception e) {
                // Ignored - best effort
            }
        }

        mCustomTabActivityHelper.setConnectionCallback(
                new MyCustomActivityHelper.ConnectionCallback() {
                    @Override
                    public void onCustomTabsConnected() {
                        Timber.d("Connect to custom tab");
                        try {
                            mCustomTabActivityHelper.mayLaunchUrl(Uri.parse(GOOGLE_URL), null, null);
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onCustomTabsDisconnected() {
                    }
                });
    }

    private void checkAndEducateUser() {
        List packages = MyCustomTabHelper.getCustomTabSupportingPackages(this);
        if (packages.size() == 0) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.custom_tab_provider_not_found))
                    .content(getString(R.string.custom_tab_provider_not_found_expln))
                    .positiveText(getString(R.string.install))
                    .negativeText(getString(android.R.string.cancel))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Util.openPlayStore(MainActivity.this, CHROME_PACKAGE);
                        }
                    }).show();
        }
    }

    private boolean shouldBind() {
        if (PrefUtil.isPreFetchPrefered(this) && Util.isAccessibilityServiceEnabled(this)) {
            return false;
        } else if (!PrefUtil.isPreFetchPrefered(this))
            return true;

        return true;
    }
}
