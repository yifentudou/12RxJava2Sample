package com.potato.rxjavasample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * retryWhen提供了 重订阅 的功能，对于retryWhen来说，它的重订阅触发有两点要素：
 * 上游通知retryWhen本次订阅流已经完成，询问其是否需要重订阅，该询问是以onError事件触发的。
 * retryWhen根据onError的类型，决定是否需要重订阅，它通过返回一个ObservableSource<?>来通知，
 * 如果该ObservableSource返回onComplete/onError，那么不会触发重订阅；如果发送onNext，那么会触发重订阅。
 * 实现retryWhen的关键在于如何定义它的Function参数：
 *
 * Function的输入是一个Observable<Throwable>，输出是一个泛型ObservableSource<?>。
 * 如果我们接收Observable<Throwable>发送的消息，那么就可以得到上游发送的错误类型，并根据该类型进行响应的处理。
 * 如果输出的Observable发送了onComplete或者onError则表示不需要重订阅，结束整个流程；
 * 否则触发重订阅的操作。也就是说，它 仅仅是作为一个是否要触发重订阅的通知，onNext发送的是什么数据并不重要。
 * 对于每一次订阅的数据流 Function 函数只会回调一次，并且是在onError(Throwable throwable)的时候触发，它不会收到任何的onNext事件。
 * 在Function函数中，必须对输入的 Observable<Object>进行处理，这里我们使用的是flatMap操作符接收上游的数据，
 * 对于flatMap的解释，大家可以参考 RxJava2 实战知识梳理(4) - 结合 Retrofit 请求新闻资讯 。
 *
 * retryWhen和repeatWhen最大的不同就是：retryWhen是收到onError后触发是否要重订阅的询问，而repeatWhen是通过onComplete触发。
 *
 * 根据 Throwable 的类型选择响应的重试策略
 * 由于上游可以通过onError(Throwable throwable)中的异常通知retryWhen，那么我们就可以根据异常的类型来决定重试的策略。
 *
 * 就像我们在上面例子中做的那样，我们通过flatMap操作符获取到异常的类型，然后根据异常的类型选择动态地决定延迟重试的时间，
 * 再用Timer操作符实现延迟重试；当然，对于一些异常，我们可以直接选择不重试，即直接返回Observable.empty或者Observable.error(Throwable throwable)。
 *
 */
public class RetryActivity extends AppCompatActivity {

    private static final String TAG = RetryActivity.class.getSimpleName();
    private static final String MSG_WAIT_SHORT = "wait_short";
    private static final String MSG_WAIT_LONG = "wait_long";
    private static final String[] MSG_ARRAY = new String[]{
            MSG_WAIT_SHORT,
            MSG_WAIT_SHORT,
            MSG_WAIT_LONG,
            MSG_WAIT_LONG
    };

    private TextView mTvRetryWhen;
    private CompositeDisposable mCompositeDisposable;
    private int mMsgIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retry);
        mTvRetryWhen = (TextView) findViewById(R.id.tv_retrywhen);
        mTvRetryWhen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRetryRequest();
            }
        });
        mCompositeDisposable = new CompositeDisposable();
    }

    private void startRetryRequest() {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                int msglen = MSG_ARRAY.length;
                doWork();
                //模拟请求的结果，前四次都返回失败，并将失败信息递交给retryWhen。
                if (mMsgIndex < msglen) { //模拟请求失败的情况。
                    emitter.onError(new Throwable(MSG_ARRAY[mMsgIndex]));
                    mMsgIndex++;
                } else { //模拟请求成功的情况。
                    emitter.onNext("Work Success");
                    emitter.onComplete();
                }
            }
        }).retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {

            private int mRetryCount;

            @Override
            public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        String errorMsg = throwable.getMessage();
                        long waitTime = 0;
                        switch (errorMsg) {
                            case MSG_WAIT_SHORT:
                                waitTime = 2000;
                                break;
                            case MSG_WAIT_LONG:
                                waitTime = 4000;
                                break;
                        }
                        Log.d(TAG, "发生错误，尝试等待时间=" + waitTime + ",当前重试次数=" + mRetryCount);
                        mRetryCount++;
                        return waitTime > 0 && mRetryCount <= 4 ? Observable.timer(waitTime, TimeUnit.MILLISECONDS) : Observable.error(throwable);
                    }
                });
            }
        });
        DisposableObserver<String> disposableObserver = new DisposableObserver<String>() {
            @Override
            public void onNext(String value) {
                Log.d(TAG, "DisposableObserver onNext=" + value);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "DisposableObserver onError=" + e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "DisposableObserver onComplete");
            }
        };
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    private void doWork() {
        long workTime = (long) (Math.random() * 500) + 500;
        try {
            Log.d(TAG, "doWork start, threadId =" + Thread.currentThread().getId());
            Thread.sleep(workTime);
            Log.d(TAG, "doWork finished");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
