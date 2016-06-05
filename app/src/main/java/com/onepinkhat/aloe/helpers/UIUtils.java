package com.onepinkhat.aloe.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.onepinkhat.aloe.app.AloeApp;

/**
 * Helper class for UI changes
 *
 * Created by jay on 5/27/16.
 */
public class UIUtils {

    /**
     * Returns a strings resource using a resource id
     *
     * @param resId Resource ID of the string to retrieve
     * @return text of the retrieved string resource
     */
    public static String getString(int resId) {
        return AloeApp.getAppContext().getString(resId);
    }

    /**
     * Shows a short toast using the context and string resource ID provided
     *
     * @param context Context to display the toast in
     * @param resId Resource ID for the string to display
     */
    public static void showToast(Context context, int resId) {
        showToast(context, getString(resId));
    }

    /**
     * Shows a short toast using the context and text provided
     *
     * @param context Context to display the toast in
     * @param text String to display
     */
    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a dialog allowing a user to retry doing an action if there is initially no network
     * connection
     *
     * @param context Context to display the dialog
     * @param onRetry Code block to run if the user attempts to retry
     * @param onCancel Code block to run if the user cancels, null if there is nothing to be done
     */
    public static void showNetworkRetryDialog(Context context,
                                              final Runnable onRetry,
                                              @Nullable final Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Please check your network connection then try again.");
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onRetry.run();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onCancel.run();
            }
        });
        builder.create().show();
    }

    /**
     * Creates an error message when attempting to cast an activity as an interface that the class
     * has not implemented
     *
     * @param activityClass Class of the activity being casted
     * @param interfaceClass Class of the interface
     * @return an error message to display in a {@link ClassCastException}
     */
    public static String getClassCastExceptionMessage(Class<?> activityClass,
                                                    Class<?> interfaceClass) {
        return activityClass.getSimpleName() + " must implement " + interfaceClass.getSimpleName();
    }
}
