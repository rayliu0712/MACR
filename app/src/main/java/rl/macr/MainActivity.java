package rl.macr;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    public TextView terminal;
    public Spinner spinner;
    public Button saveBtn;

    private final int CHOOSE_IMAGE = 0;
    private ImageFragment imageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        terminal = findViewById(R.id.terminal);
        spinner = findViewById(R.id.spinner);
        saveBtn = findViewById(R.id.save_btn);
        findViewById(R.id.choose_image).setOnClickListener(v -> {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            else
                intent = new Intent(Intent.ACTION_GET_CONTENT);

            intent.setType("image/*");
            startActivityForResult(intent, CHOOSE_IMAGE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || resultCode == RESULT_CANCELED)
            return;

        Uri uri = data.getData();

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

        setFragment(requestCode, data.getData(), filename, fileExtension, size);
    }

    private void setFragment(int fragment, Uri uri, String filename, String fileExtension, long size) {
        if (fragment == CHOOSE_IMAGE && !fileExtension.equals("gif")) {
            imageFragment = new ImageFragment(uri, filename, String.format("%s/%s", fileExtension, L.byteString(size)));
            getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
        }
    }
}