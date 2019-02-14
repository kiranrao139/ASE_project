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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
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

        //bookDataView = (TextView)findViewById(R.id.book_data);

        findViewById(R.id.read_barcode).setOnClickListener(this);

        useFlash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                 @Override
                 public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                     if(isChecked) {
                         openApp(getApplicationContext(), arappname);
                     }
                 }
             }
        );

        //new HttpRequestTask().execute();
    }

    String arappname = "com.PMD.BookReview";

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

                    barcodeValue.setText("Fetching data for : " +barcode.displayValue + " ...");

                    //bookDataView.setText("Hello");
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    if(barcode.displayValue != null && barcode.displayValue != "") {
                        new HttpRequestTask(barcode.displayValue).execute();
                    }
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


    public boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new ActivityNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    //String url = "https://www.goodreads.com/book/isbn/0441172717?key=p5SaDOvxWQtYfm4GWTAXQ";
    //String url = "http://10.0.2.2:9090/bookdata";
    String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    String getBookUrl(String isbn) {
        return url + isbn;
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {

        String bookurl;
        String isbn;

        HttpRequestTask(String isbn) {
            this.isbn = isbn;
            bookurl = getBookUrl(isbn);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL myUrl = new URL(bookurl);
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
                    String response = getBookData(output);
                    barcodeValue.setText(response);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String getBookData(String response) {
            StringBuilder result = new StringBuilder();
            try {
                JSONObject json = new JSONObject(response);
                //String title = json.getString("title");
                //String review = json.getString("review");
                JSONArray arr = json.getJSONArray("items");
                JSONObject book = arr.getJSONObject(0).getJSONObject("volumeInfo");

                result.append("ISBN: " + isbn + "\n");

                String title = book.getString("title");
                String authors = book.getString("authors");
                result.append("Title: " + title + "\n");
                result.append("Authors: " + authors + "\n");

                if( book.has("averageRating")) {
                    String rating = book.getString("averageRating");
                    result.append("Rating: " + rating + "\n");
                }

                if(book.has("ratingsCount")) {
                    String ratingCount = book.getString("ratingsCount");
                    result.append("RatingCount: " + ratingCount + "\n");
                }

            }catch(Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Barcode read: " + isbn);
                return "Invalid scancode : " + isbn;
            }

            return result.toString();
        }


    }

}
