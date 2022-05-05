package com.example.simpleblocker;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    TextView text;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final TextView headerText = (TextView) findViewById(R.id.headerText);
            String newText = intent.getStringExtra("newHeaderText");
            headerText.setText(newText);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, StickyAppCheckService.class));

        registerReceiver(broadcastReceiver, new IntentFilter("simple_blocker_action"));

        setContentView(R.layout.activity_main);

        // initialise layout

        final TextView headerText = (TextView) findViewById(R.id.headerText);
        headerText.setText("Blocking Foodora app from opening");
    }

    public void getallapps(View view) {
        // get list of all the apps installed
        List<ApplicationInfo> infos = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        // create a list with size of total number of apps
        String[] apps = new String[infos.size()];
        int i = 0;
        // add all the app name in string list
        for (ApplicationInfo info : infos) {
            apps[i] = info.packageName;
            i++;
        }
        // set all the apps name in list view
        listView.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, apps));
        // write total count of apps available.
        text.setText(infos.size() + " Apps are installed");
    }

    public void runProcess(View view) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("se.onlinepizza");
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }

    public void changeHeaderText(String text) {
        final TextView headerText = (TextView) findViewById(R.id.headerText);
        headerText.setText(text);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}
