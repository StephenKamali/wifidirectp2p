package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.ArrayList;

public class FileUploadsActivity extends AppCompatActivity {

    ArrayList<String> names = new ArrayList<>();

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
    }

    public void onUploadClick(View view) {
        Log.i("FileUploadsActivity","Uploading File");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivity(intent);
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainSearchActivity.class);
        startActivity(intent);
    }
}
