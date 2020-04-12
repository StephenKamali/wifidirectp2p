package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainSearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_search);
    }

    public void search(View view) {
        EditText fileName = (EditText) findViewById(R.id.fileName);
        Log.i(null, "Searching for file: " + fileName.getText().toString());
    }

    public void sendMessage(View view) {
        Log.i(null, "Sending Message");
    }

    public void uploadFile(View view) {
        Log.i(null, "Uploading File");
    }
}
