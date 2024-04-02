package rl.macr;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageFragment extends Fragment {
    private final Uri uri;
    private final String filename;

    private final String[] terminalTextArr = new String[2];
    private int terminalClickState = -1;
    private Bitmap originalBitmap, compressedBitmap;
    private PhotoView iv;
    private EditText qualityET;

    public ImageFragment(Uri uri, String filename, String _s) {
        this.uri = uri;
        this.filename = filename;
        this.terminalTextArr[0] = _s;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity ma = (MainActivity) getActivity();
        iv = view.findViewById(R.id.iv);
        qualityET = view.findViewById(R.id.quality_et);

        setTerminalText(ma);
        ma.terminal.setOnClickListener(v -> {
            if (terminalClickState == -1)
                return;

            terminalClickState = (terminalClickState + 1) % 2;  // 0->1, 1->0

            setTerminalText(ma);
            glide();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(ma, R.array.image, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ma.spinner.setAdapter(adapter);
        ma.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    qualityET.setText("100");
                    qualityET.setEnabled(false);
                } else
                    qualityET.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ma.saveBtn.setOnClickListener(v -> L.thread(() -> {
            L.toast(ma, "start");
            L.handler(() -> ma.saveBtn.setEnabled(false));

            String format = ma.spinner.getSelectedItem().toString().toLowerCase();
            int quality = Integer.parseInt(qualityET.getText().toString());

            if (originalBitmap != null && compressedBitmap != null) {
                terminalTextArr[0] = terminalTextArr[1];
                terminalTextArr[1] = null;

                terminalClickState = -1;

                originalBitmap = compressedBitmap.copy(compressedBitmap.getConfig(), compressedBitmap.isMutable());
                compressedBitmap = null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + format);
                    OutputStream out = ma.getContentResolver().openOutputStream(
                            ma.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    );

                    boolean ifSaveMem = ((CheckBox) view.findViewById(R.id.if_save_mem)).isChecked();
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                    switch (format) {
                        case "png":
                            originalBitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                    ifSaveMem ? out : byteOut);
                            break;
                        case "jpeg":
                            originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality,
                                    ifSaveMem ? out : byteOut);
                            break;
                        case "webp":
                            originalBitmap.compress(Bitmap.CompressFormat.WEBP, quality,
                                    ifSaveMem ? out : byteOut);
                            break;
                    }
                    format = format.equals("jpeg") ? "jpg" : format;

                    int outSize = byteOut.size();
                    String outByteString = L.byteString(outSize);
                    if (ifSaveMem || outSize == Integer.MAX_VALUE) {
                        terminalClickState = -1;
                        terminalTextArr[1] = format;
                        L.toast(ma, "finished");
                    } else {
                        terminalClickState = 0;
                        byteOut.writeTo(out);
                        compressedBitmap = BitmapFactory.decodeByteArray(byteOut.toByteArray(), 0, outSize);
                        terminalTextArr[1] = format + '/' + outByteString;
                        L.toast(ma, "finished. wrote " + outByteString);
                    }
                    setTerminalText(ma);
                    L.handler(() -> ma.saveBtn.setEnabled(true));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // TODO
            }
        }));

        L.thread(() -> {
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(ma.getContentResolver(), uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            glide();
        });
    }

    private void glide() {
        L.handler(() -> iv.setImageBitmap(terminalClickState == 1 ? compressedBitmap : originalBitmap));
    }

    private void setTerminalText(MainActivity ma) {
        L.handler(() -> {
            switch (terminalClickState) {
                case -1:
                    if (terminalTextArr[1] == null)
                        ma.terminal.setText(terminalTextArr[0]);
                    else
                        ma.terminal.setText(String.format("%s >> %s", terminalTextArr[0], terminalTextArr[1]));
                    break;
                case 0:
                    ma.terminal.setText(String.format("%s >> %s\n(click to view)", terminalTextArr[0], terminalTextArr[1]));
                    break;
                case 1:
                    ma.terminal.setText(String.format("%s >> %s\n(click to back)", terminalTextArr[0], terminalTextArr[1]));
                    break;
            }
        });
    }
}