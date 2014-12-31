package com.utbm.lo52.otaupdater;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
    private TextView system_state;
    private TextView version;
    private TextView version_label;
    private Button updateButton;
    private ImageView thumb;
    private RequestQueue requestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        system_state = (TextView) findViewById(R.id.system_state);
        version = (TextView) findViewById(R.id.version);
        version_label = (TextView) findViewById(R.id.version_label);
        updateButton = (Button) findViewById(R.id.update_button);
        thumb = (ImageView) findViewById(R.id.thumb);
        ACTUAL_VERSION = Build.VERSION.RELEASE;
        version.setText(ACTUAL_VERSION);

        // RequestQueue object allowing us to do http request without cheating on the AndroidManifest or creating a new thread to handle http connection
        requestQueue = Volley.newRequestQueue(this);

        final String url = "http://www.ota.besaba.com/update/";

        //check for update on launching
        checkUpdate(url);

        // Button default's behavior
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdate(url);
            }
        });


    }

    public void checkUpdate(String url){
             final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // We make sure that the two systems are from have the same version in case the newest is a subversion and to avoid case like
                    // 4.3 < 4.0.4 when comparing integer.
                    String NEW_VERSION =String.valueOf(response.get("version"));
                    if (versionComparator(ACTUAL_VERSION,NEW_VERSION)) {
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

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        alertDialog.setMessage("Your system is already the latest available");
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        try {
                            Thread.sleep(1210);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        alertDialog.show();

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
        }, 3500);
        requestQueue.add(request);

    }

    public boolean versionComparator(String oldVersion, String newVersion){
       int oldVer = Integer.parseInt(oldVersion.replaceAll("[.]",""));
       int newVer = Integer.parseInt(newVersion.replaceAll("[.]",""));

        // set the integer to the same number of digits to avoid case like 43 < 404 (for version oldversion = 4.3 and newVersion = 4.0.4
        // this allow us to have this instead 430 > 404 or 440 < 441 (for 4.4 is out of date compared to 4.4.1)
        int coef = (int) Math.pow(10,(newVersion.replaceAll("[.]","").length()-oldVersion.replaceAll("[.]","").length()));

        // return which one of the versions is the latest
        return (oldVer * coef) < newVer;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
