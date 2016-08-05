package com.jskaleel.speedalert.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AppLog {

    public static void print(String str) {
        createLogFile(str);
    }

    private static void createLogFile(String str) {
        File root = new File(Environment.getExternalStorageDirectory(), "AppLog");

        if (!root.exists()) {
            root.mkdirs();
        }
        File feezLogFile = new File(root, "AppLog_" + getCurrentDate() + ".txt");
        try {
            FileOutputStream fOut = new FileOutputStream(feezLogFile, true);
            OutputStreamWriter writer = new OutputStreamWriter(fOut);
            writer.append("\n").append(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentDate() {
        String currentDateandTime = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        currentDateandTime = dateFormat.format(cal.getTime());

        return currentDateandTime;
    }
}
