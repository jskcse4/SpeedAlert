package com.jskaleel.speedalert.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.jskaleel.speedalert.R;

public class AlertUtils {

    public static void showAlert(Context context, String alert, DialogInterface.OnClickListener onOkClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.app_name));
        builder.setMessage(alert);
        builder.setPositiveButton("Ok", onOkClick);
        builder.show();
    }
}
