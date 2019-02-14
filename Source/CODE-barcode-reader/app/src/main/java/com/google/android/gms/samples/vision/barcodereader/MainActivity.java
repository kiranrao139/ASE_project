/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.vision.barcodereader;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    // use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private TextView statusMessage;
    private TextView barcodeValue;
    private TextView bookDataView;


    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "BarcodeMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusMessage = (TextView)findViewById(R.id.status_message);
        barcodeValue = (TextView)findViewById(R.id.barcode_value);

        autoFocus = (CompoundButton) findViewById(R.id.auto_focus);
        useFlash = (CompoundButton) findViewById(R.id.use_flash);

        bookDataView = (TextView)findViewById(R.id.book_data);

        findViewById(R.id.read_barcode).setOnClickListener(this);

        //new HttpRequestTask().execute();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.read_barcode) {
            // launch barcode activity.
            Intent intent = new Intent(this, BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
            intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        }

    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    statusMessage.setText(R.string.barcode_success);
                    barcodeValue.setText("ISBN Code: " +barcode.displayValue + "\n" + "Review: Good Book to read!");

                    //bookDataView.setText("Hello");
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    //new HttpRequestTask().execute();
                } else {
                    statusMessage.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    //String url = "https://www.goodreads.com/book/isbn/0441172717?key=p5SaDOvxWQtYfm4GWTAXQ";
    String url = "http://10.0.2.2:9090/bookdata";

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {


                URL myUrl = new URL(url);
                //Create a connection
                HttpURLConnection connection = (HttpURLConnection)
                        myUrl.openConnection();
                connection.connect();
                //Create a new InputStreamReader

                InputStreamReader streamReader = new
                        InputStreamReader(connection.getInputStream());

                //Create a new buffered reader and String Builder
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String output = null;
                while ((output = reader.readLine()) != null) {
                    stringBuilder.append(output);
                }

                output = stringBuilder.toString();

                reader.close();
                streamReader.close();

                return output;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }
            return null;
        }


        @Override
        protected void onPostExecute(final String output) {
            try {
                if(output != null) {
                    JSONObject json = new JSONObject(output);
                    String title = json.getString("title");
                    String review = json.getString("review");
                    bookDataView.setText(title + "\n--------\n" + review);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }


        private List<String> parseXML(XmlPullParser myParser) {
            int event;
            String text = null;
            List<String> result = new ArrayList<>();

            try {
                event = myParser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    String name = myParser.getName();

                    switch (event) {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.TEXT:
                            text = myParser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            //get country name
                            if (name.equals("title")) {
                                result.add(text);
                            } else if (name.equals("description")) { //get humidity
                                result.add(text);
                                result.add(myParser.getAttributeValue(null, "value"));

                            }

                        /*else if (name.equals("pressure")) { //get pressure
                            result.add(myParser.getAttributeValue(null, "value"));

                        } else if (name.equals("temperature")) { //get temperature
                            result.add(myParser.getAttributeValue(null, "value"));

                        } else if (name.equals("coord")) { //get location
                            result.add("(" + myParser.getAttributeValue(null, "lat") + " , "
                                    + myParser.getAttributeValue(null, "lon") + ")");
                        }*/
                            break;
                    }
                    event = myParser.next();
                }
                Log.i("Key", result.toString());
                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private ScheduledThreadPoolExecutor exec = startThreadPool();


        public ScheduledThreadPoolExecutor startThreadPool() {
            exec = new ScheduledThreadPoolExecutor(1);
            exec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    // code to execute repeatedly
                    new HttpRequestTask().execute();
                }
            }, 0, 3, TimeUnit.SECONDS);
            return exec;

        }

        public void stopWalking(View view) {
            exec.shutdown();
        }
    }

}
