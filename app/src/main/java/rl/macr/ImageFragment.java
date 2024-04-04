package rl.macr;

import android.Manifest;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ImageFragment extends Fragment {
    private final Uri fUri;
    private final String fFilename;
    private final String[] terminalText = new String[2];

    private String subtype = "png";
    private int quality = 100;
    private int terminalClickState = -1;
    /*
     * -1 : unclickable
     *  0 : should show originalBitmap
     *  1 : should show compressedBitmap
     */
    private Bitmap originalBitmap, compressedBitmap;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private View view;
    private PhotoView iv;

    public EditText qualityET;
    public CheckBox ifSaveMemCB;

    public ImageFragment(Uri fUri, String finalName, String terminalTextArr0) {
        this.fUri = fUri;
        this.fFilename = finalName;
        this.terminalText[0] = terminalTextArr0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity ma = (MainActivity) getActivity();
        this.view = view;
        iv = view.findViewById(R.id.iv);
        qualityET = view.findViewById(R.id.quality_et);
        ifSaveMemCB = view.findViewById(R.id.if_save_mem);

        setTerminalText(ma);
        ma.terminal.setOnClickListener(v -> {
            if (terminalClickState == -1)
                return;

            terminalClickState = (terminalClickState + 1) % 2;  // 0->1, 1->0
            glide(terminalClickState == 0);
            setTerminalText(ma);
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

        ma.saveBtn.setOnClickListener(v -> {
            if (Integer.parseInt(qualityET.getText().toString()) > 100) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rayliu0712/macr")));
            } else {
                L.thread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            boolean r = init(ma);

                            ContentValues values = new ContentValues();
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fFilename + '.' + getFileExt());
                            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + subtype);
                            Uri uri = ma.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            OutputStream out = ma.getContentResolver().openOutputStream(uri);

                            compress(ma, out, r);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(ma, Manifest.permission.WRITE_EXTERNAL_STORAGE) == -1)
                            ActivityCompat.requestPermissions(ma,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    MainActivity.REQUEST_WRITE_PERMISSION);
                        else {
                            ma.onRequestPermissionsResult(MainActivity.REQUEST_WRITE_PERMISSION,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    new int[]{0});
                        }
                    }
                });
            }
        });
        ma.shareBtn.setOnClickListener(v -> {
            if (Integer.parseInt(qualityET.getText().toString()) > 100) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rayliu0712/macr")));
            } else {
                L.thread(() -> {
                    try {
                        boolean r = init(ma);

                        File file = new File(ma.getFilesDir(), fFilename + '.' + getFileExt());
                        OutputStream out = new FileOutputStream(file);

                        compress(ma, out, r);

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ma, "rl.macr.fileprovider", file));
                        intent.setType("image/" + subtype);
                        startActivity(Intent.createChooser(intent, null));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });

        L.thread(() -> {
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(ma.getContentResolver(), fUri);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            glide(true);
        });
    }

    private boolean init(MainActivity ma) {
        L.toast(ma, "start");
        ma.setViewEnabled(false);

        String nSubtype = ma.spinner.getSelectedItem().toString().toLowerCase();
        int nQuality = Integer.parseInt(qualityET.getText().toString());
        if (compressedBitmap != null && subtype.equals(nSubtype) && quality == nQuality) {
            return false;  // duplicated output image
        } else {
            subtype = nSubtype;
            quality = Integer.parseInt(qualityET.getText().toString());

            buffer = new ByteArrayOutputStream();
            if (terminalText[0] != null && terminalText[1] != null) {
                terminalText[0] = terminalText[1];
                if (compressedBitmap != null) {
                    originalBitmap = compressedBitmap.copy(compressedBitmap.getConfig(), compressedBitmap.isMutable());
                    compressedBitmap = null;
                }
            }
            return true;
        }
    }

    private void compress(MainActivity ma, OutputStream out, boolean r) throws Exception {
        boolean ifSaveMem = ifSaveMemCB.isChecked();

        if (r) {
            switch (subtype) {
                case "png":
                    originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, ifSaveMem ? out : buffer);
                    break;
                case "jpeg":
                    L.log(buffer == null);
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, ifSaveMem ? out : buffer);
                    break;
                case "webp":
                    originalBitmap.compress(Bitmap.CompressFormat.WEBP, quality, ifSaveMem ? out : buffer);
                    break;
            }

            int outSize = buffer.size();
            if (ifSaveMem || outSize == Integer.MAX_VALUE) {
                terminalClickState = -1;
                terminalText[1] = subtype;
                L.toast(ma, "finished");
            } else {
                terminalText[1] = subtype + '/' + L.byteString(outSize);
                buffer.writeTo(out);
                compressedBitmap = BitmapFactory.decodeByteArray(buffer.toByteArray(), 0, outSize);

                if (terminalText[0].contains("/")) {
                    terminalClickState = 0;
                    glide(true);
                } else {
                    terminalClickState = -1;
                    glide(false);
                }
                L.toast(ma, "finished. wrote " + L.byteString(outSize));
            }
            setTerminalText(ma);
        } else {
            L.handler(() -> {
                if (ifSaveMemCB.isChecked())
                    Snackbar.make(view,
                            "Save Memory option will be disabled and not working due to duplicated output",
                            Snackbar.LENGTH_SHORT).show();
                ifSaveMemCB.setChecked(false);
            });

            buffer.writeTo(out);
            L.toast(ma, "finished. wrote " + L.byteString(buffer.size()));
        }
        ma.setViewEnabled(true);
    }

    private void setTerminalText(MainActivity ma) {
        L.handler(() -> {
            switch (terminalClickState) {
                case -1:
                    if (terminalText[1] == null)
                        ma.terminal.setText(terminalText[0]);
                    else
                        ma.terminal.setText(String.format("%s >> %s", terminalText[0], terminalText[1]));
                    break;
                case 0:
                    ma.terminal.setText(String.format("%s >> %s\n(click to view)", terminalText[0], terminalText[1]));
                    break;
                case 1:
                    ma.terminal.setText(String.format("%s >> %s\n(click to back)", terminalText[0], terminalText[1]));
                    break;
            }
        });
    }

    private void glide(boolean isOrigin) {
        L.handler(() -> iv.setImageBitmap(isOrigin ? originalBitmap : compressedBitmap));
    }

    public void save(MainActivity ma) {
        boolean r = init(ma);
        try (FileOutputStream out = new FileOutputStream(
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fFilename + '.' + getFileExt())
        )) {
            compress(ma, out, r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileExt() {
        return subtype.equals("jpeg") ? "jpg" : subtype;
    }
}