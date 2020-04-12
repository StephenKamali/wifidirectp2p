package cs3220.project.wifidirectp2p;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileDownloadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileDownloadFragment extends Fragment {

    public FileDownloadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FileDownloadFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FileDownloadFragment newInstance() {
        FileDownloadFragment fragment = new FileDownloadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_file_download, container, false);
        Button download = v.findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.i("FileDownloadFragment", "Find a way to call WiFiDirectActivity.broadcast");
            }
        });
        return v;
    }
}
