package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainSearchActivity extends AppCompatActivity {
    public static final String SEARCH_STRING = "cs3220.project.wifidirectp2p.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_search);
    }

    public void search(View view) {
        EditText fileName = findViewById(R.id.fileName);
        Log.i("MainSearchActivity", "Search Clicked");
        Intent intent = new Intent(this, FileDownloadActivity.class);
        intent.putExtra(SEARCH_STRING, fileName.getText().toString());
        startActivity(intent);
    }

    public void goToNetworkView(View view) {
        Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);
    }

    public void uploadFile(View view) {
        Log.i("MainSearchActivity", "Upload File Clicked");
        Intent intent = new Intent(this, FileUploadsActivity.class);
        startActivity(intent);
    }
}
