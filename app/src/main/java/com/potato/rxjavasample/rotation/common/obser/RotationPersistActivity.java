package com.potato.rxjavasample.rotation.common.obser;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.potato.rxjavasample.R;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.DisposableObserver;

/**
 * 最后来看Activity，当点击“开始工作任务”后，我们尝试添加WorkerFragment，
 * 这时候就会走到WorkerFragment的onCreate方法中启动任务，之后当WorkerFragment走到onResume方法后，
 * 就调用onWorkerPrepared，让Activity进行订阅，Activity就可以收到当前任务进度的通知，来更新UI。
 * 而在任务执行完毕之后，我们就可以将该Fragment移除了。
 *
 *
 * Activity 和 Fragment 之间的数据传递
 * 数据传递分为两个方向，它们各自可以通过以下方法来实现：
 *
 * Activity向Fragment传递数据（示例中我们传递了任务的名称）
 * 一般用于向WorkerFragment传递一些任务参数，此时可以通过Fragment的setArguments传入相关的字段，
 * Fragment在onCreate方法中通过getArguments获取参数。
 *
 * Fragment向Activity传递数据（示例中我们传递了Observable供Activity订阅以获取进度）
 * 可以让Activity实现一个接口，我们在Fragment的onAttach方法中获取Activity实例转换成对应的接口类型，
 * 之后通过它来调用Activity的方法，需要注意的是，在Fragment#onDetach时，要将该Activity的引用置空，否则会出现内存泄漏
 *
 *
 * 在任务执行之后 removeFragment
 * 在了能让WorkerFragment能正常进行下一次任务的执行，我们需要在发生错误或者任务完成的时候，通过remove的方式销毁WorkerFragment。
 *
 */

public class RotationPersistActivity extends AppCompatActivity implements IHolder {
    private static final String TAG = RotationPersistActivity.class.getSimpleName();
    private Button mBtnWorker;
    private TextView mTvResult;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_persist);
        Log.i(TAG, "onCreate");
        mBtnWorker = findViewById(R.id.btn_start_worker);
        mBtnWorker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWorker();
            }
        });

        mTvResult = findViewById(R.id.tv_result);
        mCompositeDisposable = new CompositeDisposable();
    }

    private void startWorker() {
        WorkerFragment workerFragment = getWorkerFragment();
        if (workerFragment == null) {
            addWorkerFragment();
        } else {
            Log.i(TAG, "WorkerFragment has attach");
        }
    }

    private void addWorkerFragment() {
        WorkerFragment workerFragment = new WorkerFragment();
        Bundle bundle = new Bundle();
        bundle.putString("task_name", "学习RxJava2");
        workerFragment.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(workerFragment, workerFragment.TAG).commit();
    }

    private WorkerFragment getWorkerFragment() {
        FragmentManager fm = getSupportFragmentManager();
        return (WorkerFragment) fm.findFragmentByTag(WorkerFragment.TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onResume");
        mCompositeDisposable.clear();
    }


    @Override
    public void onWorkerPrepared(ConnectableObservable<String> workerFlow) {
        DisposableObserver<String> disposableObserver = new DisposableObserver<String>() {
            @Override
            public void onNext(String s) {
                mTvResult.setText(s);
            }

            @Override
            public void onError(Throwable e) {
                onWorkerFinished();
                mTvResult.setText("任务错误：" + e);
            }

            @Override
            public void onComplete() {
                onWorkerFinished();
                mTvResult.setText("任务完成");
            }
        };
        workerFlow.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }


    private void removeWorkerFragment() {
        WorkerFragment workerFragment = getWorkerFragment();
        if (workerFragment != null) {
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager.beginTransaction().remove(workerFragment).commit();
        }
    }

    private void onWorkerFinished() {
        Log.i(TAG, "onWorkerComplete");
        removeWorkerFragment();
    }
}
