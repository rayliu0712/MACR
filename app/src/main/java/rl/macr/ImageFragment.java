package rl.macr;

import android.content.ContentValues;
import android.content.Intent;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageFragment extends Fragment {
    private final Uri chosedUri;
    private final String filename;
    private final String[] terminalTextArr = new String[2];

    private String subtype;
    private int terminalClickState = -1;
    private Bitmap originalBitmap, compressedBitmap;
    private OutputStream out;
    private ByteArrayOutputStream byteOut;
    private PhotoView iv;
    private EditText qualityET;
    private CheckBox ifSaveMemCB;

    public ImageFragment(Uri chosedUri, String filename, String terminalTextArr0) {
        this.chosedUri = chosedUri;
        this.filename = filename;
        this.terminalTextArr[0] = terminalTextArr0;
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
        ifSaveMemCB = view.findViewById(R.id.if_save_mem);

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + subtype);
                    init(ma);
                    out = ma.getContentResolver().openOutputStream(
                            ma.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    );

                    compress(ma);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // TODO
            }
        }));
        ma.shareBtn.setOnClickListener(v -> L.thread(() -> {
            try {
                init(ma);
                File file = new File(ma.getFilesDir(), filename + '.' + subtype);
                out = new FileOutputStream(file);
                compress(ma);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ma, "rl.macr.fileprovider", file));
                intent.setType("image/" + subtype);
                startActivity(Intent.createChooser(intent, null));

                ma.setButtonEnabled(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        L.thread(() -> {
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(ma.getContentResolver(), chosedUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            glide();
        });
    }

    private void init(MainActivity ma) {
        L.toast(ma, "start");
        ma.setButtonEnabled(false);
        subtype = ma.spinner.getSelectedItem().toString().toLowerCase();
        byteOut = new ByteArrayOutputStream();
        if (originalBitmap != null && compressedBitmap != null) {
            terminalTextArr[0] = terminalTextArr[1];
            terminalTextArr[1] = null;

            terminalClickState = -1;

            originalBitmap = compressedBitmap.copy(compressedBitmap.getConfig(), compressedBitmap.isMutable());
            compressedBitmap = null;
        }
    }

    private void compress(MainActivity ma) throws IOException {
        int quality = Integer.parseInt(qualityET.getText().toString());
        boolean ifSaveMem = ifSaveMemCB.isChecked();

        switch (subtype) {
            case "png":
                originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, ifSaveMem ? out : byteOut);
                break;
            case "jpeg":
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, ifSaveMem ? out : byteOut);
                break;
            case "webp":
                originalBitmap.compress(Bitmap.CompressFormat.WEBP, quality, ifSaveMem ? out : byteOut);
                break;
        }

        int outSize = byteOut.size();
        if (ifSaveMem || outSize == Integer.MAX_VALUE) {
            terminalClickState = -1;
            terminalTextArr[1] = subtype;
            L.toast(ma, "finished");
        } else {
            terminalClickState = 0;
            byteOut.writeTo(out);
            compressedBitmap = BitmapFactory.decodeByteArray(byteOut.toByteArray(), 0, outSize);
            terminalTextArr[1] = subtype + '/' + L.byteString(outSize);
            L.toast(ma, "finished. wrote " + L.byteString(outSize));
        }
        setTerminalText(ma);
        ma.setButtonEnabled(true);
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