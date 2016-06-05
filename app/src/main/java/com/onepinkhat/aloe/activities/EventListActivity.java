package com.onepinkhat.aloe.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.google.android.gms.common.GoogleApiAvailability;
import com.onepinkhat.aloe.R;
import com.onepinkhat.aloe.helpers.UIUtils;
import com.onepinkhat.aloe.integrations.GoogleCalendarHelper;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EventListActivity extends AppCompatActivity {

    @BindView(R.id.btnGoogleLogin)
    Button btnGoogleLogin;

    private GoogleCalendarHelper calendarHelper;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);
        ButterKnife.bind(this);

        setupViews();
        calendarHelper = new GoogleCalendarHelper(this,
                new GoogleCalendarHelper.GoogleCalendarHelperCallback() {
                    @Override
                    public void startActivityForResult(Intent accountPickerIntent, int requestCode) {
                        startActivityForResult(accountPickerIntent, requestCode);
                    }

                    @Override
                    public void onLoadFinished(String events) {
                        if (progressDialog != null) {
                            progressDialog.hide();
                            btnGoogleLogin.setEnabled(true);

                            if (StringUtils.isNotEmpty(events)) {
                                UIUtils.showToast(EventListActivity.this, events);
                            }
                        }
                    }

                    @Override
                    public void showErrorDialog(int connectionStatusCode, int requestCode) {
                        Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                                EventListActivity.this,
                                connectionStatusCode,
                                requestCode);
                        dialog.show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        calendarHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.btnGoogleLogin)
    public void onClickGoogleLogin() {
        btnGoogleLogin.setEnabled(false);
        progressDialog.show();
        calendarHelper.getResultsFromApi();
    }

    @Override
    public void startActivityForResult(Intent accountPickerIntent, int requestCode) {
        startActivityForResult(accountPickerIntent, requestCode);
    }

    private void setupViews() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));
    }
}
