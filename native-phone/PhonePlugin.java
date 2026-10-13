package com.ishhf.aichat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "PhonePlugin")
public class PhonePlugin extends Plugin {
    @PluginMethod
    public void agentStart(PluginCall call) {
        String sys = call.getString("sys", "");
        String task = call.getString("task", "");
        String url = call.getString("url", "");
        String dev = call.getString("dev", "");
        int max = call.getInt("max", 12);
        boolean ok = QAgent.start(getContext().getApplicationContext(), sys, task, url, dev, max);
        JSObject r = new JSObject();
        r.put("ok", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void agentStatus(PluginCall call) {
        JSObject r = new JSObject();
        r.put("running", QAgent.running);
        r.put("log", QAgent.log);
        r.put("result", QAgent.result);
        call.resolve(r);
    }

    @PluginMethod
    public void agentStop(PluginCall call) {
        QAgent.stop = true;
        call.resolve();
    }

    @PluginMethod
    public void accEnabled(PluginCall call) {
        JSObject r = new JSObject();
        r.put("on", QAccService.inst != null);
        call.resolve(r);
    }

    @PluginMethod
    public void accSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e));
        }
    }

    @PluginMethod
    public void appInfo(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e));
        }
    }

    @PluginMethod
    public void transOn(PluginCall call) {
        boolean ok = QAccService.transOn(call.getString("url", ""), call.getString("dev", ""), call.getBoolean("auto", false));
        JSObject r = new JSObject();
        r.put("ok", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void transOff(PluginCall call) {
        QAccService.transOff();
        call.resolve();
    }

    @PluginMethod
    public void transAuto(PluginCall call) {
        QAccService.transAuto(call.getBoolean("auto", false));
        call.resolve();
    }

    @PluginMethod
    public void transState(PluginCall call) {
        JSObject r = new JSObject();
        r.put("on", QAccService.transIsOn());
        call.resolve(r);
    }

    private static OutputStream vos;

    // حفظ فيديو على دفعات (base64) داخل Movies/Qasim
    @PluginMethod
    public void saveChunk(PluginCall call) {
        try {
            String name = call.getString("name", "qasim.webm");
            String mime = call.getString("mime", "video/webm");
            String data = call.getString("data", "");
            boolean first = call.getBoolean("first", false);
            boolean last = call.getBoolean("last", false);
            byte[] b = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
            if (first) {
                if (vos != null) {
                    try { vos.close(); } catch (Exception e) {}
                    vos = null;
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentResolver cr = getContext().getContentResolver();
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Video.Media.DISPLAY_NAME, name);
                    v.put(MediaStore.Video.Media.MIME_TYPE, mime);
                    v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Qasim");
                    Uri uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) { call.reject("insert failed"); return; }
                    vos = cr.openOutputStream(uri);
                } else {
                    File d = getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                    if (d != null) d.mkdirs();
                    vos = new FileOutputStream(new File(d, name));
                }
            }
            if (vos == null) { call.reject("no stream"); return; }
            vos.write(b);
            if (last) {
                vos.close();
                vos = null;
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e));
        }
    }
}
