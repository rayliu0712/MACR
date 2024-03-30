package rl.macr;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    private final int CHOOSE_IMAGE = 0;
    private final int CHOOSE_VIDEO = 1;
    private final int CHOOSE_AUDIO = 2;
    private Bitmap bitmap;
    private String filename;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.spinner);

        findViewById(R.id.choose_image).setOnClickListener(v -> {
            Intent intent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent.setType("image/*");
            startActivityForResult(intent, CHOOSE_IMAGE);
        });

        findViewById(R.id.save).setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + spinner.getSelectedItem().toString().toLowerCase());
            Log.d("666", spinner.getSelectedItem().toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Handler handler = new Handler();

                new Thread(() -> {
                    try {
                        boolean result = false;
                        OutputStream out = getContentResolver().openOutputStream(
                                getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        );

                        switch (spinner.getSelectedItem().toString()) {
                            case "PNG":
                                result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                break;
                            case "JPEG":
                                result = bitmap.compress(Bitmap.CompressFormat.JPEG, 0, out);
                                break;
                            case "WebP":
                                result = bitmap.compress(Bitmap.CompressFormat.WEBP, 0, out);
                                break;
                        }

                        boolean fResult = result;
                        Log.d("666", String.valueOf(result));
                        handler.post(() -> {
                            Toast.makeText(this, fResult ? "Succeed" : "Failed", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception ignored) {
                        Log.d("666", "shit");
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();

                Handler handler = new Handler();
                new Thread(() -> {
                    try {
                        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                        int nameIndex = cursor.getColumnIndex("_display_name");
                        int sizeIndex = cursor.getColumnIndex("_size");
                        cursor.moveToFirst();
                        filename = cursor.getString(nameIndex);

                        int i = filename.length() - 1;
                        for (; filename.charAt(i) != '.'; i--) ;
                        filename = filename.substring(0, i);

                        handler.post(() -> {
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            } catch (Exception ignored) {
                            }
                            Toast.makeText(this, filename + ' ' + cursor.getLong(sizeIndex) / 1024 + "KiB", Toast.LENGTH_SHORT).show();
                            Glide.with(this).load(bitmap).into((ImageView) findViewById(R.id.iv));
                        });
                    } catch (Exception ignored) {
                    }
                }).start();
            }
        }

    }
}