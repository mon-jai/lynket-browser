package arun.com.chromer.chrometabutilites;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;

import arun.com.chromer.R;
import arun.com.chromer.services.ScannerService;
import arun.com.chromer.services.WarmupService;
import arun.com.chromer.util.PrefUtil;
import timber.log.Timber;

/**
 * Created by Arun on 06/01/2016.
 */
public class CustomTabDelegate {
    private static final String TAG = CustomTabDelegate.class.getSimpleName();

    public static CustomTabsIntent getCustomizedTabIntent(Context ctx, String url) {
        CustomTabsIntent.Builder builder;
        CustomTabsSession session = getAvailableSessions(ctx);

        if (session != null) builder = new CustomTabsIntent.Builder(session);
        else builder = new CustomTabsIntent.Builder();

        builder.setShowTitle(true);
        addActionButtonSecondary(ctx, url, builder);
        return builder.build();
    }

    private static void addActionButtonSecondary(Context ctx, String url, CustomTabsIntent.Builder builder) {
        if (url != null) {
            Intent activityIntent = new Intent(ctx, SecondaryBrowserReceiver.class);

            PendingIntent openBrowser = PendingIntent
                    .getBroadcast(ctx, 0, activityIntent,
                            PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setActionButton(
                    drawableToBitmap(ctx.getResources().getDrawable(R.drawable.ic_open_in_browser, ctx.getTheme())), "Secondary browser", openBrowser);
        }
    }

    private static CustomTabsSession getAvailableSessions(Context ctx) {
        ScannerService sService = ScannerService.getInstance();
        if (sService != null && PrefUtil.isPreFetchPrefered(ctx)) {
            Timber.d("Scanner service is running properly");
            return sService.getTabSession();
        }
        WarmupService service = WarmupService.getInstance();
        if (service != null) {
            Timber.d("Warmup service is running properly");
            return service.getTabSession();
        }
        Timber.d("No existing sessions present");
        return null;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
