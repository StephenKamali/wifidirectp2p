package cs3220.project.wifidirectp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class FileDownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_download);
        Intent intent = getIntent();
        String searchString = intent.getStringExtra(MainSearchActivity.SEARCH_STRING);
        EditText searchInput = findViewById(R.id.searchText);
        searchInput.setText(searchString);
    }

    public void onSearchClick(View view) {
        EditText searchInput = findViewById(R.id.searchText);
        Log.i("FileDownloadActivity", "Search Clicked");
        search(searchInput.getText().toString());
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainSearchActivity.class);
        startActivity(intent);
    }

    private void search(String searchString) {
        //TODO
        Log.i("FileDownloadActivity", "Searching for " + searchString);
    }
}
