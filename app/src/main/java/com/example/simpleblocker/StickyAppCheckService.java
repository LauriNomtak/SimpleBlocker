package com.example.simpleblocker;

import android.app.ActivityManager.*;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StickyAppCheckService extends Service {

    private static final String BROADCAST_ACTION = "simple_blocker_action";
    
    private static final String CHANNEL_ID = "simple_blocker_channel";
    private List<String> stalkList = new ArrayList<String>();
    private List<String> blockList = new ArrayList<String>();
    PackageManager pm;

    public StickyAppCheckService() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();
        pm = this.getPackageManager();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                final List<RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
                for (int i = 0; i < services.size(); i++) {
                    if (!stalkList.contains(services.get(i).baseActivity.getPackageName())) {
                        // you may broad cast a new application launch here.
                        stalkList.add(services.get(i).baseActivity.getPackageName());
                    }
                }

                List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
                for (int i = 0; i < procInfos.size(); i++) {

                    ArrayList<String> runningPkgs = new ArrayList<String>(Arrays.asList(procInfos.get(i).pkgList));

                    Collection diff = subtractSets(runningPkgs, stalkList);

                    if (diff != null) {
                        stalkList.removeAll(diff);
                    }
                }

                if (checkAndKill("se.onlinepizza")) {
                    Log.d("testing", "App running! Closing it now!!!");
                }

            }
        }, 5000, 6000);  // every 6 seconds


        return START_STICKY;
    }

    private RunningAppProcessInfo getForegroundApp() {
        RunningAppProcessInfo result = null, info = null;

        final ActivityManager activityManager  =  (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        List <RunningAppProcessInfo> l = activityManager.getRunningAppProcesses();
        Iterator <RunningAppProcessInfo> i = l.iterator();
        while(i.hasNext()) {
            info = i.next();
            if(info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && !isRunningService(info.processName)) {
                result = info;
                break;
            }
        }
        return result;
    }

    private boolean isRunningService(String processName) {
        if(processName == null)
            return false;

        RunningServiceInfo service;

        final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        List <RunningServiceInfo> l = activityManager.getRunningServices(9999);
        Iterator <RunningServiceInfo> i = l.iterator();
        while(i.hasNext()){
            service = i.next();
            if(service.process.equals(processName))
                return true;
        }
        return false;
    }

    private boolean isRunningApp(String processName) {
        if(processName == null)
            return false;

        RunningAppProcessInfo app;

        final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        List <RunningAppProcessInfo> l = activityManager.getRunningAppProcesses();
        Iterator <RunningAppProcessInfo> i = l.iterator();
        while(i.hasNext()){
            app = i.next();
            if(app.processName.equals(processName) && app.importance != RunningAppProcessInfo.IMPORTANCE_SERVICE)
                return true;
        }
        return false;
    }

    private boolean checkAndKill(String processName) {
        if(processName == null)
            return false;
        Log.d("Testing", "Checking if app running! " + processName);
        RunningAppProcessInfo app;

        /*ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = activityManager.getRunningAppProcesses();
        Iterator <RunningAppProcessInfo> i = runningAppProcessInfo.iterator();

        StringBuilder sb = new StringBuilder();

        while(i.hasNext()){
            app = i.next();
            Log.d("testing", app.processName);
            if(app.processName.equals(processName) && app.importance != RunningAppProcessInfo.IMPORTANCE_SERVICE)
            {
                Log.d("Testing", "Closing app! " + processName + "=" + app.processName);
                notifyUser();
                android.os.Process.killProcess(app.pid);
                return true;
            }

           sb.append(app.processName).append(", ");

        }*/

        StringBuilder sb = new StringBuilder();

        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        Iterator<ApplicationInfo> i = list.iterator();
        while(i.hasNext()) {
            ApplicationInfo info = i.next();
            Log.d("ApplicationInfo", info.processName);

            sb.append(info.processName).append(", ");

        }

        broadcast("newHeaderText", sb.toString());

        return false;
    }

    private void notifyUser(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setContentTitle("SimpleBlocker closed Foodora")
                .setContentText("Don't try and open it again!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }
    
    private void broadcast(String key, String value) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(key,value);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private boolean checkifThisIsActive(RunningAppProcessInfo target){
        boolean result = false;
        ActivityManager.RunningTaskInfo info;

        if(target == null)
            return false;

        final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        List <ActivityManager.RunningTaskInfo> l = activityManager.getRunningTasks(9999);
        Iterator <ActivityManager.RunningTaskInfo> i = l.iterator();

        while(i.hasNext()){
            info=i.next();
            if(info.baseActivity.getPackageName().equals(target.processName)) {
                result = true;
                break;
            }
        }

        return result;
    }


    // what is in b that is not in a ?
    public static Collection subtractSets(Collection a, Collection b)
    {
        Collection result = new ArrayList(b);
        result.removeAll(a);
        return result;
    }

}