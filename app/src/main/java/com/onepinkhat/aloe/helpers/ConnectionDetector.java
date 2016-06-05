package com.onepinkhat.aloe.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.onepinkhat.aloe.app.AloeApp;

/**
 * Helper for determining whether the app is connected to a data network or wifi
 *
 * Created by jay on 5/22/16.
 */
public class ConnectionDetector {

    /**
     * Checks whether the device is connected to the network
     *
     * @return true if the device has connectivity, false otherwise
     */
    public static boolean hasConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) AloeApp.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
