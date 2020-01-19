package com.potato.rxjavasample.rotation.common;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by li.zhirong on 2020/1/19
 */
public class TaskFragment extends Fragment {
    private static String TAG = TaskFragment.class.getSimpleName();
    private TaskCallback mCallbacks;
    private DummyTask mTask;

    interface TaskCallback {
        void onPreExecute();

        void onProgressUpdate(int percent);

        void onCancelled();

        void onPostExecute();
    }

    private class DummyTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; !isCancelled() && i < 100; i++) {
                SystemClock.sleep(100);
                publishProgress(i);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mCallbacks != null) {
                mCallbacks.onProgressUpdate(values[0]);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute();
            }
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "TaskFragment onAttach");

    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallback) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "TaskFragment onCreate");
        setRetainInstance(true);
        mTask = new DummyTask();
        mTask.execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "TaskFragment onDetach");
        mCallbacks = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.i(TAG, "TaskFragment onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "TaskFragment onViewCreated");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "TaskFragment onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "TaskFragment onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "TaskFragment onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "TaskFragment onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "TaskFragment onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "TaskFragment onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "TaskFragment onDestroy");
    }
}
