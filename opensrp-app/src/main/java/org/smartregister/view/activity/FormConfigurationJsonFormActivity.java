package org.smartregister.view.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.vijay.jsonwizard.activities.JsonFormActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.R;
import org.smartregister.util.AppExecutors;
import org.smartregister.util.FormUtils;

import java.io.BufferedReader;
import java.io.IOException;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 07-05-2020.
 */
public class FormConfigurationJsonFormActivity extends JsonFormActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JSONObject jsonObject = getmJSONObject();
        if (FormUtils.isFormNew(jsonObject)) {
            showFormVersionUpdateDialog(getString(R.string.form_update_title), getString(R.string.form_update_message));
        }
    }

    public void showFormVersionUpdateDialog(@NonNull String title, @NonNull String message) {
        int clientId = FormUtils.getClientFormId(getmJSONObject());
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        negateIsNewClientForm(clientId);
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        negateIsNewClientForm(clientId);
                    }
                })
                .show();
    }

    @VisibleForTesting
    protected void negateIsNewClientForm(int clientFormId) {
        AppExecutors appExecutors = new AppExecutors();

        appExecutors.diskIO()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        CoreLibrary.getInstance().context().getClientFormRepository()
                                .setIsNew(false, clientFormId);
                    }
                });
    }

    @Nullable
    @Override
    public BufferedReader getRules(@NonNull Context context, @NonNull String fileName) throws IOException {
        try {
            FormUtils formUtils = FormUtils.getInstance(context);
            BufferedReader bufferedReader = formUtils.getRulesFromRepository(fileName);
            if (bufferedReader != null) {
                return bufferedReader;
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return super.getRules(context, fileName);
    }

    @Nullable
    @Override
    public JSONObject getSubForm(String formIdentity, String subFormsLocation, Context context, boolean translateSubForm) throws Exception {
        FormUtils formUtils = FormUtils.getInstance(context);
        JSONObject dbForm = null;
        try {
            dbForm = formUtils.getSubFormJsonFromRepository(formIdentity, subFormsLocation, context, translateSubForm);

        } catch (JSONException ex) {
            Timber.e(ex);
            handleFormError(false, formIdentity);
            return null;
        }

        if (dbForm == null) {
            return super.getSubForm(formIdentity, subFormsLocation, context, translateSubForm);
        }

        return dbForm;
    }

    @Override
    public void handleFormError(boolean isRulesFile, @NonNull String formIdentifier) {
        FormUtils formUtils = null;
        try {
            formUtils = FormUtils.getInstance(this);
        } catch (Exception e) {
            Timber.e(e);
        }

        if (formUtils != null) {
            formUtils.handleJsonFormOrRulesError(isRulesFile, formIdentifier, form -> {
                if (form != null) {
                    Toast.makeText(this, R.string.form_changed_reopen_to_take_effect, Toast.LENGTH_LONG)
                            .show();
                }

                FormConfigurationJsonFormActivity.this.finish();
            });
        }
    }
}
