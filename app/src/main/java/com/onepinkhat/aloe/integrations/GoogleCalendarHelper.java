package com.onepinkhat.aloe.integrations;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;
import com.onepinkhat.aloe.R;
import com.onepinkhat.aloe.app.AloeApp;
import com.onepinkhat.aloe.helpers.ConnectionDetector;
import com.onepinkhat.aloe.helpers.UIUtils;
import com.onepinkhat.aloe.models.EventsResult;

import java.io.IOException;
import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Helper for interacting with the Google Calendar API
 *
 * Created by jay on 5/22/16.
 */
public class GoogleCalendarHelper {

    public static final String TAG = GoogleCalendarHelper.class.getSimpleName();

    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    private static final int REQUEST_ACCOUNT_PICKER = 100;
    private static final int REQUEST_AUTHORIZATION = 101;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 102;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 103;

    private static final int DEFAULT_MAX_EVENT_RESULTS = 10;

    private static final String PREF_ACCOUNT_NAME_KEY = "accountNameKey";

    private Context context;

    private GoogleAccountCredential credential;
    private GoogleCalendarHelperCallback callback;
    private GoogleApiAvailability apiAvailability;

    private SharedPreferences preferences;

    public interface GoogleCalendarHelperCallback {
        void startActivityForResult(Intent accountPickerIntent, int requestCode);
        void onLoadFinished(EventsResult eventsResult);
        void showErrorDialog(int connectionStatusCode, int requestCode);
    }

    /**
     * Creates a new Google Calendar helper
     */
    public GoogleCalendarHelper(Context context, GoogleCalendarHelperCallback callback) {
        this.context = context;
        this.callback = callback;

        preferences = AloeApp.getAppContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        apiAvailability = GoogleApiAvailability.getInstance();
        credential = GoogleAccountCredential.usingOAuth2(
                AloeApp.getAppContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff());
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                handleGooglePlayServicesResult(resultCode);
                break;
            case REQUEST_ACCOUNT_PICKER:
                handleAccountPickerResult(resultCode, data);
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    private void handleGooglePlayServicesResult(int resultCode) {
        if (resultCode != Activity.RESULT_OK) {
            UIUtils.showToast(context, R.string.error_need_play_services);
        } else {
            getResultsFromApi();
        }
    }

    private void handleAccountPickerResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null) {
                preferences.edit().putString(PREF_ACCOUNT_NAME_KEY, accountName).apply();
                credential.setSelectedAccountName(accountName);
                getResultsFromApi();
            }
        }
    }

    /**
     * First verifies that all the preconditions are satisfied, then attempts to call the API. The
     * preconditions are: Google Play Services installed, an account was selected and the device
     * currently has online access. If any of the preconditions are not satisfied, the app will
     * prompt the user as appropriate.
     */
    public void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!ConnectionDetector.hasConnectivity()) {
            UIUtils.showNetworkRetryDialog(context, new Runnable() {
                @Override
                public void run() {
                    getResultsFromApi();
                }
            }, null);
        } else {
            new MakeRequestTask(credential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account name was previously
     * saved it will use that one; otherwise an account picker dialog will be shown to the user. Note
     * that the setting the account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already present. The
     * AfterPermissionGranted annotation indicates that this function will be rerun automatically
     * whenever the GET_ACCOUNTS permission is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = preferences.getString(PREF_ACCOUNT_NAME_KEY, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                callback.startActivityForResult(credential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    UIUtils.getString(R.string.request_google_account_via_contacts),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google Play Services
     * installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of) Google Play Services
     *                             on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        callback.showErrorDialog(connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
    }

    /**
     * An asynchronous task that handles the Google Calendar API call. Placing the API calls in
     * their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, EventsResult> {

        private com.google.api.services.calendar.Calendar service = null;
        private Exception lastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            service = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(UIUtils.getString(R.string.app_name))
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected EventsResult doInBackground(Void... params) {
            try {
                return getEvents(DEFAULT_MAX_EVENT_RESULTS);
            } catch (Exception e) {
                lastError = e;
                cancel(true);
                return null;
            }
        }

        private EventsResult getEvents(int maxResults) throws IOException {
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setMaxResults(maxResults)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            return new EventsResult(events);
        }


        @Override
        protected void onPreExecute() {
            /* NOP */
        }

        @Override
        protected void onPostExecute(EventsResult output) {
            EventsResult result = new EventsResult(output.getEvents());
            if (output == null || output.getEventItems().size() == 0) {
                result.setErrorMessage("No results returned.");
            }
            callback.onLoadFinished(result);
        }

        @Override
        protected void onCancelled() {
            EventsResult result = new EventsResult();
            if (lastError != null) {
                if (lastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) lastError)
                                    .getConnectionStatusCode());
                } else if (lastError instanceof UserRecoverableAuthIOException) {
                    callback.startActivityForResult(
                            ((UserRecoverableAuthIOException) lastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    result.setErrorMessage("The following error occurred:\n"
                            + lastError.getMessage());
                }
            } else {
                result.setErrorMessage("Request cancelled.");
            }
            callback.onLoadFinished(result);
        }
    }
}
