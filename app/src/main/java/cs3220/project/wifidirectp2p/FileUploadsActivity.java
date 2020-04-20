package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class FileUploadsActivity extends AppCompatActivity {

    ArrayList<String> names = new ArrayList<>();
    private static final int FILE_UPLOAD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_uploads);
        File[] uploads = getApplicationContext().getFilesDir().listFiles();
        if(uploads != null) {
            for (File f : uploads) {
                names.add(f.getName());
            }
        }

        TextView emptyMessage = findViewById(R.id.noUploads);
        ListView uploadsList = findViewById(R.id.uploadsList);
        if (names.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            uploadsList.setVisibility(View.GONE);
        } else {
            Log.i(null, names.get(0));
            emptyMessage.setVisibility(View.GONE);
            uploadsList.setVisibility(View.VISIBLE);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    names);

            uploadsList.setAdapter(arrayAdapter);
        }
    }

    public void onUploadClick(View view) {
        Log.i("FileUploadsActivity","Uploading File");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_UPLOAD);
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainSearchActivity.class);
        startActivity(intent);
    }

    // from https://stackoverflow.com/questions/5568874/how-to-extract-the-file-name-from-uri-returned-from-intent-action-get-content
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Log.i(null, "Here");
        if (requestCode == FILE_UPLOAD && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(null, uri.toString());
                File upload = new File(uri.getPath());
                try {
                    FileOutputStream fOut = openFileOutput(getFileName(uri), Context.MODE_PRIVATE);
                    InputStream is = getContentResolver().openInputStream(uri);
                    byte[] buff = new byte[1024];
                    int len;
                    while ((len = is.read(buff)) > 0) {
                        fOut.write(buff, 0, len);
                    }
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File[] uploads = getApplicationContext().getFilesDir().listFiles();
            names = new ArrayList<>();
            if(uploads != null) {
                for (File f : uploads) {
                    names.add(f.getName());
                }
            }
        }
    }
}
