package org.openhds.mobile.utilities;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import org.openhds.mobile.OpenHDS;
import org.openhds.mobile.R;
import org.openhds.mobile.provider.FormsProviderAPI;

import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static org.openhds.mobile.syncadpt.Constants.ACCOUNT_TYPE;
import static org.openhds.mobile.syncadpt.Constants.AUTHTOKEN_TYPE_DEVICE;
import static org.openhds.mobile.utilities.LoginUtils.launchLogin;
import static org.openhds.mobile.utilities.SyncUtils.downloadedContentBefore;

public class SetupUtils {

    private static final String TAG = SetupUtils.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};

    public static boolean setupRequirementsMet(Context ctx) {
        return hasRequiredPermissions(ctx)
                && isODKInstalled(ctx.getPackageManager())
                && isAccountInstalled(ctx)
                && isDataAvailable(ctx);
    }

    public static boolean hasRequiredPermissions(Context ctx) {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(ctx, perm) != PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void startApp(final Activity source) {
        createNotificationChannels(source);
        launchLogin(source);
    }

    private static void createNotificationChannels(Context ctx) {
        NotificationUtils.createChannels(ctx.getApplicationContext());
    }

    public static void askForPermissions(Activity activity, int requestCode) {
        if (needsPermissions(activity)) {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, requestCode);
        }
    }

    private static boolean needsPermissions(Context ctx) {
        return !hasRequiredPermissions(ctx);
    }

    public static boolean isDataAvailable(Context ctx) {
        return downloadedContentBefore(ctx);
    }

    public static boolean isAccountInstalled(Context ctx) {
        return AccountManager.get(ctx).getAccountsByType(ACCOUNT_TYPE).length > 0;
    }

    public static boolean isODKInstalled(PackageManager manager) {
        Intent odkFormsIntent = new Intent(Intent.ACTION_EDIT, FormsProviderAPI.FormsColumns.CONTENT_URI);
        List<ResolveInfo> intentMatches = manager.queryIntentActivities(odkFormsIntent, 0);
        return !intentMatches.isEmpty();
    }

    public static void promptODKInstall(final Activity activity) {

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    launchODKMarketInstall();
                }
                activity.finish();
            }

            private void launchODKMarketInstall() {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=org.odk.collect.android"));
                activity.startActivity(intent);
            }
        };

        DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                activity.finish();
            }
        };

        new AlertDialog.Builder(activity)
                .setTitle(R.string.odk_required)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.odk_install_prompt)
                .setNegativeButton(R.string.quit_label, clickListener)
                .setPositiveButton(R.string.install_label, clickListener)
                .setOnCancelListener(cancelListener)
                .show();
    }

    public static void getToken(final Activity activity) {
        AccountManager
                .get(activity.getBaseContext())
                .getAuthTokenByFeatures(ACCOUNT_TYPE, AUTHTOKEN_TYPE_DEVICE, null, activity, null, null,
                        new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> future) {
                                try {
                                    Bundle result = future.getResult();
                                    result.getString(AccountManager.KEY_AUTHTOKEN);
                                } catch (Exception e) {
                                    Log.e(TAG, "failed to retrieve token", e);
                                }
                            }
                        }, null);
    }

}