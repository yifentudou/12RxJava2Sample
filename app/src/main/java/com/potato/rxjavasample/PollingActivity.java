package com.potato.rxjavasample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * 简单及进阶的轮询操作
 * 我们尝试使用RxJava2提供的操作符来实现这一需求，这里演示两种方式的轮询，并将单次访问的次数限制在5次：
 * <p>
 * 固定时延：使用intervalRange操作符，每间隔3s执行一次任务。
 * 变长时延：使用repeatWhen操作符实现，第一次执行完任务后，等待4s再执行第二次任务，在第二次任务执行完成后，等待5s，依次递增。
 * <p>
 * 作者：泽毛
 * 链接：https://www.jianshu.com/p/fa1828d70192
 * 来源：简书
 * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 */
public class PollingActivity extends AppCompatActivity {
    private static final String TAG = PollingActivity.class.getSimpleName();
    private TextView mTvSimple, mTvAdvance;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polling);
        mTvSimple = (TextView) findViewById(R.id.tv_simple);
        mTvSimple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimplePolling();
            }
        });
        mTvAdvance = (TextView) findViewById(R.id.tv_advance);
        mTvAdvance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvancePolling();
            }
        });
        compositeDisposable = new CompositeDisposable();
    }

    /**
     * 之所以可以通过repeatWhen来实现轮询，是因为它为我们提供了重订阅的功能，而重订阅有两点要素：
     * <p>
     * 上游告诉我们一次订阅已经完成，这就需要上游回调onComplete函数。
     * 我们告诉上游是否需要重订阅，通过repeatWhen的Function函数所返回的Observable确定，
     * 如果该Observable发送了onComplete或者onError则表示不需要重订阅，结束整个流程；否则触发重订阅的操作。
     * <p>
     * repeatWhen的难点在于如何定义它的Function参数：
     * Function的输入是一个Observable<Object>，输出是一个泛型ObservableSource<?>。
     * 如果输出的Observable发送了onComplete或者onError则表示不需要重订阅，结束整个流程；
     * 否则触发重订阅的操作。也就是说，它 仅仅是作为一个是否要触发重订阅的通知，onNext发送的是什么数据并不重要。
     * 对于每一次订阅的数据流 Function 函数只会回调一次，并且是在onComplete的时候触发，它不会收到任何的onNext事件。
     * 在Function函数中，必须对输入的 Observable<Object>进行处理，这里我们使用的是flatMap操作符接收上游的数据，
     * <p>
     * 而当我们不需要重订阅时，有两种方式：
     * <p>
     * 返回Observable.empty()，发送onComplete消息，但是DisposableObserver并不会回调onComplete。
     * 返回Observable.error(new Throwable("Polling work finished"))，DisposableObserver的onError会被回调，并接受传过去的错误信息。
     * <p>
     * 使用 Timer 实现两次订阅之间的时延
     * 重订阅触发的时间是在返回的ObservableSource发送了onNext事件之后，
     * 那么我们通过该ObservableSource延迟发送一个事件就可以实现相应的需求，
     * 这里使用的是time操作符，它的原理图如下所示，也就是，在订阅完成后，等待指定的时间它才会发送消息。
     * <p>
     * 使用 doOnComplete 完成轮询的耗时操作
     * 由于在订阅完成时会发送onComplete消息，那么我们就可以在doOnComplete中进行轮询所要进行的具体操作，它所运行的线程通过subscribeOn指定。
     */
    private void startAdvancePolling() {
        Log.d(TAG, "startAdvancePolling");
        final Observable<Long> observable = Observable.just(0L).doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                doWork();
            }
        }).repeatWhen(new Function<Observable<Object>, ObservableSource<Long>>() {

            private long mRepeatCount;

            @Override
            public ObservableSource<Long> apply(Observable<Object> objectObservable) throws Exception {
                //必须作出反应，这里是通过flatMap操作符
                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Object o) throws Exception {
                        if (++mRepeatCount > 4) {
                            //return Observable.empty(); //发送onComplete消息，无法触发下游的onComplete回调。
                            return Observable.error(new Throwable("Pollong work finished"));//发送onError消息，可以触发下游的onError回调。

                        }
                        Log.d(TAG, "startAdvancePolling apply");

                        return Observable.timer(3000 + mRepeatCount * 1000, TimeUnit.MILLISECONDS);
                    }
                });

            }
        });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe();
        compositeDisposable.add(disposableObserver);
    }

    private void startSimplePolling() {
        Log.d(TAG, "startSimplePolling");
/**
 * intervalRange & doOnNext 实现固定时延轮询
 * 是一个创建型操作符，该Observable第一次先发射一个特定的数据，之后间隔一段时间再发送一次，它是interval和range的结合体
 * 与interval相比，它可以指定第一个发送数据项的时延、指定发送数据项的个数。
 * 与range相比，它可以指定两项数据之间发送的时延。
 *
 * start：发送数据的起始值，为Long型。
 * count：总共发送多少项数据。
 * initialDelay：发送第一个数据项时的起始时延。
 * period：两项数据之间的间隔时间。
 * TimeUnit：时间单位。
 *
 * 在轮询操作中一般会进行一些耗时的网络请求，因此我们选择在doOnNext进行处理，
 * 它会在下游的onNext方法被回调之前调用，但是它的运行线程可以通过subscribeOn指定，
 * 下游的运行线程再通过observerOn切换会主线程，通过打印对应的线程ID可以验证结果。
 *
 */

        Observable<Long> observable = Observable.intervalRange(0, 5, 0, 3000, TimeUnit.MILLISECONDS)
                .take(5)
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        doWork(); //这里使用了doOnNext，因此DisposableObserver的onNext要等到该方法执行完才会回调。
                    }
                });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        compositeDisposable.add(disposableObserver);
    }

    private DisposableObserver<Long> getDisposableObserver() {
        return new DisposableObserver<Long>() {
            @Override
            public void onNext(Long aLong) {

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "DisposableObserver onError, threadId=" + Thread.currentThread().getId() + ",reason=" + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "DisposableObserver onComplete, threadId=" + Thread.currentThread().getId());
            }
        };
    }

    private void doWork() {
        long workTime = (long) (Math.random() * 500) + 500;
        try {
            Log.d(TAG, "doWork start, threadId=" + Thread.currentThread().getId());
            Thread.sleep(workTime);
            Log.d(TAG, "doWork finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}
