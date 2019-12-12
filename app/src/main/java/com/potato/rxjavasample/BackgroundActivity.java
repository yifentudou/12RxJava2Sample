package com.potato.rxjavasample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * 当我们需要进行一些耗时操作，例如下载、访问数据库等，
 * 为了不阻塞主线程，往往会将其放在后台进行处理，
 * 同时在处理的过程中、处理完成后通知主线程更新UI，
 * 这里就涉及到了后台线程和主线程之间的切换。
 *
 * 作者：泽毛
 * 链接：https://www.jianshu.com/p/c935d0860186
 * 来源：简书
 * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 */
public class BackgroundActivity extends AppCompatActivity {
    private TextView tv_download_result;
    private TextView tv_download;
    private  String TAG = BackgroundActivity.class.getSimpleName();
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background);
        tv_download = (TextView) findViewById(R.id.tv_download);
        tv_download_result = (TextView) findViewById(R.id.tv_download_result);
        tv_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload();
            }
        });
    }

    private void startDownload() {
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; i < 100; i++) {
                    if (i % 20 == 0) {
                        try {
                            Thread.sleep(500);//模拟下载操作
                        } catch (InterruptedException exception) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(exception);
                            }
                        }
                    }
                    emitter.onNext(i);
                }
                emitter.onComplete();
            }
        });

        DisposableObserver<Integer> disposableObserver = new DisposableObserver<Integer>() {
            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext=" + integer);
                tv_download_result.setText("Current Progress=" + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError=" + e);
                tv_download_result.setText("Download Error");

            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete");
                tv_download_result.setText("Download onComplete");

            }
        };
        //subscribeOn:指定Observable自身在哪个调度器上执行
        //observeOn:指定一个观察者在哪个调度器上观察这个Observable
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }
}
