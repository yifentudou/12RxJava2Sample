package com.potato.rxjavasample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.subjects.PublishSubject;

/**
 * 计算一段时间内数据的平均值
 *
 * 我们有时候会需要计算一段时间内的平均数据，
 * 例如统计一段时间内的平均温度，
 * 或者统计一段时间内的平均位置。
 * 在接触RxJava之前，
 * 我们一般会将这段时间内统计到的数据都暂时存起来，
 * 等到需要更新的时间点到了之后，
 * 再把这些数据结合起来，计算这些数据的平均值。
 *
 *这里，我们通过一个Handler循环地发送消息，
 * 实现间隔一定时间进行温度的测量，
 * 但是在测量之后，我们并不实时地更新界面的温度显示，
 * 而是每隔3s统计一次过去这段时间内的平均温度。
 *
 * 作者：泽毛
 * 链接：https://www.jianshu.com/p/5dd01b14c02a
 * 来源：简书
 * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 */
public class BufferActivity extends AppCompatActivity {

    private PublishSubject<Double> mPublishSubject;
    private TextView tv_buffer;
    private String TAG = BufferActivity.class.getSimpleName();
    private CompositeDisposable compositeDisposable;
    private SourceHandler mSourceHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buffer);
        tv_buffer = (TextView) findViewById(R.id.tv_buffer);
        mPublishSubject = PublishSubject.create();
        DisposableObserver<List<Double>> disposableObserver = new DisposableObserver<List<Double>>() {
            @Override
            public void onNext(List<Double> doubles) {
                double result = 0;
                if (doubles.size() > 0) {
                    for (double d : doubles) {
                        result += d;
                    }
                    result = result / doubles.size();
                }
                Log.d(TAG, "更新平均温度：" + result);
                tv_buffer.setText("过去3秒收到了" + doubles.size() + "个数据，平均温度为：" + result);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        //两个形参分别对应是时间的值和单位
        mPublishSubject.buffer(3000, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(disposableObserver);
        //开始测量温度
        mSourceHandler = new SourceHandler();
        mSourceHandler.sendEmptyMessage(0);
    }

    private class SourceHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            double temperature = Math.random() * 25 + 5;
            updateTemperature(temperature);
            //循环发送
            sendEmptyMessageDelayed(0, 250 + (long) (250 + Math.random()));
        }
    }

    private void updateTemperature(double temperature) {
        Log.d(TAG, "温度测量结果：" + temperature);
        //事件并不会直接传递到Observer的onNext方法中，
        // 而是放在缓冲区中，直到事件到之后，
        // 再将所有在这段缓冲事件内放入缓冲区中的值，
        // 放在一个List中一起发送到下游。
        mPublishSubject.onNext(temperature);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSourceHandler.removeCallbacksAndMessages(null);
        compositeDisposable.clear();
    }
}
