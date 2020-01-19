package com.potato.rxjavasample.rotation.common;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.potato.rxjavasample.R;

public class ProgressActivity extends AppCompatActivity implements TaskFragment.TaskCallback {
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private static final String TAG = ProgressActivity.class.getSimpleName();
    private ProgressBar pb;
    private Button btn_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        btn_start = findViewById(R.id.btn_start);
        pb = findViewById(R.id.pb);

        FragmentManager fragmentManager = getSupportFragmentManager();
        TaskFragment mTaskFragment = (TaskFragment) fragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT);

        if (mTaskFragment == null) {
            mTaskFragment = new TaskFragment();
            fragmentManager.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }


    }

    @Override
    public void onPreExecute() {
        Log.i(TAG, "onPreExecute");
      ;
    }

    @Override
    public void onProgressUpdate(int percent) {

        Log.i(TAG, "onProgressUpdate" +percent);
    }

    @Override
    public void onCancelled() {
        Log.i(TAG, "onCancelled");
        btn_start.setText("Cancel");

    }

    @Override
    public void onPostExecute() {
        Log.i(TAG, "onPostExecute");
    }
}
