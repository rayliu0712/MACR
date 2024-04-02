package rl.macr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class L {
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static String str(Object obj) {
        return String.valueOf(obj);
    }

    public static String byteString(long _bytes) {
        float bytes = Float.parseFloat(Long.toString(_bytes));

        char i = 0;
        String unit = "B";
        while (bytes >= 1024) {
            bytes /= 1024;
            i += 1;
        }

        switch (i) {
            case 1:
                unit = "KB";
                break;
            case 2:
                unit = "MB";
                break;
            case 3:
                unit = "GB";
                break;
        }

        return (float) Math.round(bytes * 10) / 10 + unit;
    }

    public static void log(Object obj) {
        Log.d("666", String.valueOf(obj));
    }

    public static void thread(Runnable r) {
        new Thread(r).start();
    }

    public static void handler(Runnable r) {
        handler.post(r);
    }

    public static void toast(Context context, Object obj) {
        handler(() -> Toast.makeText(context, String.valueOf(obj), Toast.LENGTH_SHORT).show());
    }
}
