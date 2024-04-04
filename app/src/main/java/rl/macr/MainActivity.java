package rl.macr;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_WRITE_PERMISSION = 6;

    private final int CHOOSE_IMAGE = 0;
    private final int RECEIVED_IMAGE = 3;
    private Button chooseImage;
    private ImageFragment imageFragment;

    public TextView terminal;
    public Spinner spinner;
    public Button saveBtn;
    public Button shareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File[] files = getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        Intent startIntent = getIntent();
        boolean isStartByReceiving = Intent.ACTION_SEND.equals(startIntent.getAction()) && startIntent.getType() != null;

        findViewById(R.id.container).setOnClickListener(v ->
                ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(v.getWindowToken(), 0));

        terminal = findViewById(R.id.terminal);
        spinner = findViewById(R.id.spinner);
        chooseImage = findViewById(R.id.choose_image);
        saveBtn = findViewById(R.id.save_btn);
        shareBtn = findViewById(R.id.share_btn);

        if (isStartByReceiving) {
            findViewById(R.id.choose_vg).setVisibility(View.GONE);
            onActivityResult(RECEIVED_IMAGE, RESULT_OK, startIntent);
        } else {
            chooseImage.setOnClickListener(v -> {
                Intent intent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                else
                    intent = new Intent(Intent.ACTION_GET_CONTENT);

                intent.setType("*/*");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/png", "image/jpeg", "image/webp"});
                }
                startActivityForResult(intent, CHOOSE_IMAGE);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || resultCode == RESULT_CANCELED)
            return;

        if (requestCode == CHOOSE_IMAGE || requestCode == RECEIVED_IMAGE) {
            Uri uri = requestCode == CHOOSE_IMAGE ? data.getData() : data.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null)
                return;

            Cursor cursor = getContentResolver().query(
                    uri,
                    new String[]{"_display_name", "_size"},
                    null,
                    null,
                    null
            );
            cursor.moveToFirst();
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow("_size"));
            cursor.close();

            String filename, fileExtension;
            for (int i = displayName.length() - 2; ; i--) {
                if (displayName.charAt(i) == '.') {
                    filename = displayName.substring(0, i);
                    fileExtension = displayName.substring(i + 1);
                    break;
                }
            }

            setFragment(requestCode, uri, filename, fileExtension, size);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults[0] == 0) {
                imageFragment.save(this);
            }
        }
    }

    private void setFragment(int fragment, Uri uri, String filename, String fileExtension, long size) {
        if (fragment == CHOOSE_IMAGE || fragment == RECEIVED_IMAGE) {
            imageFragment = new ImageFragment(uri, filename, String.format("%s/%s", fileExtension, L.byteString(size)));
            getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
        }
    }

    public void setViewEnabled(boolean enabled) {
        L.handler(() -> {
            chooseImage.setEnabled(enabled);
            terminal.setEnabled(enabled);
            spinner.setEnabled(enabled);
            saveBtn.setEnabled(enabled);
            shareBtn.setEnabled(enabled);
            imageFragment.qualityET.setEnabled(enabled);
            imageFragment.ifSaveMemCB.setEnabled(enabled);
        });
    }
}