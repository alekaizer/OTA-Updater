package com.utbm.lo52.otaupdater;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Achille AROUKO Lekaizer
 *         OTAUpdater made for LO52 class at UTBM
 *         Teacher: Fabien Brisset
 */

public class MainActivity extends ActionBarActivity {
    private String ACTUAL_VERSION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView system_state = (TextView) findViewById(R.id.system_state);
        final TextView version = (TextView) findViewById(R.id.version);
        final TextView version_label = (TextView) findViewById(R.id.version_label);
        final Button updateButton = (Button) findViewById(R.id.update_button);
        final ImageView thumb = (ImageView) findViewById(R.id.thumb);
        ACTUAL_VERSION = Build.VERSION.INCREMENTAL;
        version.setText(ACTUAL_VERSION);

        // RequestQueue object allowing us to do http request without cheating on the AndroidManifest or creating a new thread to handle http connection
        final RequestQueue requestQueue = Volley.newRequestQueue(this);

        String url = "http://www.ota.besaba.com/update/";

        // the request to be executed
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (Integer.parseInt(ACTUAL_VERSION.substring(ACTUAL_VERSION.lastIndexOf(".") + 1)) < Integer.parseInt(String.valueOf(response.get("version")))) {
                        // The system is out of date, update needed.

                        system_state.setText(R.string.need_update);
                        version_label.setText(R.string.version_label_2);
                        version.setText(response.get("version").toString());
                        thumb.setImageResource(R.drawable.unchecked);
                        updateButton.setText(R.string.update_text);
                        updateButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                                final PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakelock");
                                // The wakelock object created above and used below, acquire a wakelock in order to keep the CPU on during the update process
                                wakelock.acquire();
                                // this timer object to simulate an update process during 3.5 seconds
                                final Timer rebootTimer = new Timer();
                                final ProgressDialog rebootDialog = new ProgressDialog(MainActivity.this);
                                rebootDialog.setMessage(getResources().getString(R.string.reboot));
                                rebootDialog.setTitle(R.string.updating);
                                rebootDialog.show();
                                rebootTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        // the wakelock acquired above is released in order to keep the CPU on and to alterate the battery, after that
                                        // progressDialog is closed and we call a rebooting instruction
                                        wakelock.release();
                                        rebootDialog.dismiss();
                                        rebootTimer.cancel();
                                    }
                                }, 4500);
                                // As the app isn't allowed to reboot the device as it isn't signed and I didn't put the LOCAL_CERTIFICATE := platform
                                // on Android.mk I commented the line below, this line is supposed to reboot the device

                                //powerManager.reboot(null);
                            }
                        });
                    } else {
                        // The system is up to date, no update needed
                        system_state.setText(R.string.updated);
                        version_label.setText(R.string.version_label);
                        thumb.setImageResource(R.drawable.checked);
                        updateButton.setText(R.string.fetch);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },
                // Error Handler
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        system_state.setText(R.string.error);
                        thumb.setImageResource(R.drawable.connection_error);
                    }
                });

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.fetching));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final Timer delay = new Timer();
        delay.schedule(new TimerTask() {
            @Override
            public void run() {
                progressDialog.dismiss();
                delay.cancel();
            }
        }, 2000);
        requestQueue.add(request);

        // Button default's behavior
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestQueue.add(request);
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
