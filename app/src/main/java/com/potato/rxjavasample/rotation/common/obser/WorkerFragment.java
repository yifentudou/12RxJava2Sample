package com.potato.rxjavasample.rotation.common.obser;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by li.zhirong on 2020/1/19
 */
public class WorkerFragment extends Fragment {
    public static final String TAG = WorkerFragment.class.getSimpleName();
    private IHolder mHolder;
    private ConnectableObservable<String> mWorker;
    private Disposable mDispoable;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof IHolder) {
            mHolder = (IHolder) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle bundle = getArguments();
        final String taskName = (bundle != null ? bundle.getString("task_name") : null);
        mWorker = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                for (int i = 0; i < 10; i++) {
                    String message = "任务名称=" + taskName + ", 任务进度=" + i * 10 + "%";
                    try {
                        Log.i(TAG, message);
                        Thread.sleep(1000);
                        //如果已经抛弃，那么不在继续任务
                        if (emitter.isDisposed()) {
                            break;
                        }
                    } catch (InterruptedException error) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(error);
                        }
                    }
                    emitter.onNext(message);
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).publish();
        mDispoable = mWorker.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHolder != null) {
            mHolder.onWorkerPrepared(mWorker);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispoable.dispose();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHolder = null;
    }
}
