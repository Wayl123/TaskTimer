package com.waynelee.tasktimer;

import android.app.DatePickerDialog;
import android.content.Context;

public class UnbuggyDatePickerDialog extends DatePickerDialog {
    public UnbuggyDatePickerDialog(Context context, OnDateSetListener callBack, int year, int month, int dayOfMonth) {
        super(context, callBack, year, month, dayOfMonth);
    }

    @Override
    protected void onStop() {
        // Do nothing - do not call super method
    }
}
