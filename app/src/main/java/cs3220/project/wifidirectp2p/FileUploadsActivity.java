package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class FileUploadsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_uploads);
    }

    public void onUploadClick(View view) {
        Log.i("FileUploadsActivity","Uploading File");
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainSearchActivity.class);
        startActivity(intent);
    }
}
