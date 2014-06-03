/*
 * Copyright (C) 2014 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.iwedia.callbacks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.iwedia.dtv.DVBManager;
import com.iwedia.dtv.parental.dvb.IParentalCallbackDvb;
import com.iwedia.dtv.parental.dvb.ParentalAgeEvent;
import com.iwedia.dtv.types.InternalException;

/**
 * Parental control callback.
 */
public class ParentalCallback implements IParentalCallbackDvb {
    private AlertDialog mAlertDialog;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mAlertDialog.show();
        };
    };
    private static ParentalCallback sInstance;

    public static ParentalCallback getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParentalCallback(context);
        }
        return sInstance;
    }

    private ParentalCallback(final Context context) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle("Enter PIN code");
        final EditText editText = new EditText(context);
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(4);
        editText.setFilters(new InputFilter[] { maxLengthFilter });
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        builderSingle.setView(editText);
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (DVBManager
                                    .getInstance()
                                    .getParentalManager()
                                    .checkPin(
                                            Integer.valueOf(editText.getText()
                                                    .toString()))) {
                                dialog.dismiss();
                            } else {
                                Toast.makeText(context, "Wrong PIN entered!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        } catch (InternalException e) {
                            e.printStackTrace();
                        }
                    }
                });
        mAlertDialog = builderSingle.create();
    }

    @Override
    public void ageLocked(ParentalAgeEvent arg0) {
        Log.d("ParentalCallback", "AGE LOCKED CALLBACK HAPPENED, FOR AGE: "
                + arg0.getAge());
        mHandler.sendEmptyMessage(0);
    }

    @Override
    public void channelLocked(int arg0, boolean arg1) {
        Log.d("ParentalCallback",
                "CHANNEL LOCKED CALLBACK HAPPENED, FOR CHANNEL: " + arg0 + " "
                        + arg1);
        if (arg1) {
            mHandler.sendEmptyMessage(0);
        }
    }
}
