package com.potato.rxjavasample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class TimerActivity extends AppCompatActivity {
    private static final String TAG = TimerActivity.class.getSimpleName();
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        mCompositeDisposable = new CompositeDisposable();
    }

    //先执行一个任务，等待 1s，再执行另一个任务，然后结束
    public void clickDelay(View view) {
        Log.i(TAG, "startTimerActivityDelay");
        DisposableObserver<Long> disposableObserver = getTimeDemoObserver();
        Observable.just(0L).doOnNext(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.i(TAG, "执行第一个任务");

            }
        }).delay(1000, TimeUnit.MILLISECONDS).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    ///每隔一秒，执行一次任务，第一次任务执行前有一秒的间隔，执行无限次
    public void clickInterval(View view) {
        Log.i(TAG, "startTimerActivityInterval");
        DisposableObserver<Long> disposableObserver = getTimeDemoObserver();
        Observable.interval(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    ///每隔 1s 执行一次任务，立即执行第一次任务，执行无限次
    public void clickIntervalFirstNo(View view) {
        Log.i(TAG, "startTimerActivityIntervalFirstNo");
        DisposableObserver<Long> disposableObserver = getTimeDemoObserver();
        Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    ///每隔1s 执行一次任务，立即执行第一次任务，只执行五次
    public void clickIntervalLimitFive(View view) {
        Log.i(TAG, "startTimerActivityInterval5");
        DisposableObserver<Long> disposableObserver = getTimeDemoObserver();
        Observable.interval(0,1000, TimeUnit.MILLISECONDS)
                .take(5)
                .subscribeOn(Schedulers.io())
                .subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    //延迟一秒后执行一个任务，然后结束
    public void clickTimer(View view) {
        Log.i(TAG, "startTimerActivityTimer");
        DisposableObserver<Long> disposableObserver = getTimeDemoObserver();
        Observable.timer(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(disposableObserver);

        mCompositeDisposable.add(disposableObserver);
    }

    private DisposableObserver<Long> getTimeDemoObserver() {
        return new DisposableObserver<Long>() {
            @Override
            public void onNext(Long aLong) {
                Log.i(TAG, "onNext:" + aLong + "\t" + "throwId:" + Thread.currentThread());
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "onError:" + e);
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete!");
            }
        };
    }
}
