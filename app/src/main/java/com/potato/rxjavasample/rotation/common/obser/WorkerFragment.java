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
 *
 * 下面，我们来实现WorkerFragment，我们在onCreate中创建了数据源，
 * 它每隔1s向下游发送数据，在onResume中，
 * 通过前面定义的接口向Activity传递一个ConnectableObservable用于监听。
 * 这里最关键的是需要调用我们前面说到的setRetainInstance方法，
 * 最后别忘了，在onDetach中将mHolder置为空，否则它就会持有需要被重建的Activity示例，从而导致内存泄漏。
 *
 *
 * 为什么调用 publish 方法，使用 Hot Observable 作为 WorkerFragment 的数据源
 * 推荐大家先看一下这篇文章 RxJava 教程第三部分：驯服数据流之 Hot & Cold Observable，
 * 这里面对于Cold & Hot Observable进行了解释，它们之间关键的区别就是：
 *
 * 只有当订阅者订阅时，Cold Observale才开始发送数据，并且每个订阅者都独立执行一遍数据流代码。
 * 而Hot Observable不管有没有订阅者，它都会发送数据流。
 * 而在我们的应用场景中，由于WorkerFragment是在后台执行任务：
 *
 * 从Activity的角度来看：每次Activity重建时，在Activity中都需要用一个新的Observer实例去订阅WorkerFragment中的数据源，
 * 因此我们只能选择通过Hot Observable，而不是Cold Observable来实现WorkerFragment中的数据源，
 * 否则每次都会重新执行一遍数据流的代码，而不是继续接收它发送的事件。
 *
 * 从WorkerFragment的角度来看，它只是一个任务的执行者，不管有没有人在监听它的进度，它都应该执行任务。
 * 通过Observable.create方法创建的是一个Cold Observable，
 * 该Cold Observable每隔1s发送一个事件。
 * 我们调用publish方法来将它转换为Hot Observable，
 * 之后再调用该Hot Observable的connect方法让其对源Cold Observable进行订阅，
 * 这样源Cold Observable就可以开始执行任务了。
 * 并且，通过connect方法返回的Disposable对象，我们就可以管理转换后的Hot Observable和源Cold Observable之间的订阅关系。
 *
 * Disposable的用途在于：在某些时候，我们希望能够停止源Observable任务的执行，
 * 例如当该WorkerFragment真正被销毁时，也就是执行了它的onDestroy方法，
 * 那么我们就可以通过上面的Disposable取消Hot Observable对源Cold Observable的订阅，
 * 而在Cold Observable的循环中，我们判断如果下游（也就是Hot Observable）取消了订阅，那么就不再执行任务。
 *
 */
public class WorkerFragment extends Fragment {
    public static final String TAG = WorkerFragment.class.getSimpleName();
    private IHolder mHolder;
    private ConnectableObservable<String> mWorker;
    private Disposable mDispoable;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG,"onAttach");

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
                for (int i = 0; i <= 10; i++) {
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
        Log.i(TAG,"onResume");
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
        Log.i(TAG,"onDetach");
        mHolder = null;
    }
}
