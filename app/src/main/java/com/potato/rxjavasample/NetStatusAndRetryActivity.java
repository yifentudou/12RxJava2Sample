package com.potato.rxjavasample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetStatusAndRetryActivity extends AppCompatActivity {
    private static final String TAG = NetStatusAndRetryActivity.class.getSimpleName();
    private static final List<Long> CITY_ARRAY = new ArrayList<Long>() {//这个大括号 就相当于我们  new 接口
        {//这个大括号 就是 构造代码块 会在构造函数前 调用
            this.add(100010L);//this 可以省略  这里加上 只是为了让读者 更容易理解
            this.add(100011L);
            this.add(100012L);
            this.add(100013L);
            this.add(100014L);
        }

    };
    private Thread mLocationThread;
    private PublishSubject<Long> mCityPublish;
    private PublishSubject<Boolean> mNetStatusPublish;
    private BroadcastReceiver mReceiver;
    private boolean isNetworkConnected;
    private CompositeDisposable mCompositeDisposable;
    private TextView tv_show_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_status_and_retry);
        mCompositeDisposable = new CompositeDisposable();

        tv_show_result = (TextView) findViewById(R.id.tv_show_result);
    }

    // 定位模块
    //我们通过一个后台线程来模拟定位的过程，它每隔一段时间获取一次定位的结果，
    // 并将该结果通过mCityPublish发送数据给它的订阅者
    private void startUpdateLocation() {
        mLocationThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        for (Long cityId : CITY_ARRAY) {
                            if (isInterrupted()) {
                                break;
                            }
                            Log.i(TAG, "重新定位");
                            Thread.sleep(1000);
                            Log.i(TAG, "定位到城市信息：" + cityId);
                            mCityPublish.onNext(cityId);
                        }
                    } catch (InterruptedException e) {

                    }
                }
            }
        };
        mLocationThread.start();
    }

    /**
     * 在mCityPublish发送消息到订阅者收到消息之间，我们还需要做一些特殊的处理：
     * 使用distinctUntilChanged对定位结果进行过滤，如果此次定位的结果和上次定位的结果相同，那么不通知订阅者
     * <p>
     * 使用doOnNext，在返回结果给订阅者之前，先把最新一次的定位结果存储起来，用于在之后网络重连之后进行请求。
     */
    private Observable<Long> getCityPublish() {
        return mCityPublish.distinctUntilChanged().doOnNext(new Consumer<Long>() {
            @Override
            public void accept(Long l) throws Exception {
                saveCacheCity(l);
            }
        });
    }

    private void saveCacheCity(Long s) {

    }

    private long getCacheCity() {
        return 100;
    }

    /**
     * 网络状态模块
     * 与定位模块类似，我们也需要一个mNetStatusPublish，其类型为PublishSubject，
     * 它在网络状态发生变化时通知订阅者。这里需要注册一个广播，在收到广播之后，我们通过mNetStatusPublish通知订阅者
     */
    private void registerBroadcast() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mNetStatusPublish != null) {
                    mNetStatusPublish.onNext(isNetworkConnected);
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, filter);
    }

    /**
     * 使用filter对消息进行过滤，只有在 联网情况并且之前已经定位到了城市 之后才通知订阅者，
     * filter操作符用于过滤掉一些不需要的数据：
     * 使用map，读取当前缓存的城市名，返回给订阅者，map操作符可以用于执行变换操作。
     *
     * @return
     */
    private Observable<Long> getNetStatusPublish() {
        return mNetStatusPublish.filter(new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) throws Exception {
                return aBoolean && getCacheCity() > 0;
            }

        }).map(new Function<Boolean, Long>() {
            @Override
            public Long apply(Boolean b) throws Exception {
                return getCacheCity();
            }
        }).subscribeOn(Schedulers.io());
    }

    private void startUpdateWeather() {
        Observable.merge(getCityPublish(), getNetStatusPublish())
                .flatMap(new Function<Long, ObservableSource<WeatherEnity>>() {
                    @Override
                    public ObservableSource<WeatherEnity> apply(Long aLong) throws Exception {
                        Log.i(TAG, "尝试请求天气信息=" + aLong);
                        return getWeather(aLong).subscribeOn(Schedulers.io());
                    }
                })
                .retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                        return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(Throwable throwable) throws Exception {
                                Log.i(TAG, "请求天气信息过程中发生错误，进行重订阅");
                                return Observable.just(0);
                            }
                        });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<WeatherEnity>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(WeatherEnity weatherEntity) {
                        WeatherEnity.WeatherInfo info = weatherEntity.getWeatherinfo();
                        if (info != null) {
                            Log.d(TAG, "尝试请求天气信息成功");
                            StringBuilder builder = new StringBuilder();
                            builder.append("城市名：").append(info.getCity()).append("\n").append("温度：").append(info.getTemp()).append("\n").append("风向：").append(info.getWD()).append("\n").append("风速：").append(info.getWS()).append("\n");
                            tv_show_result.setText(builder.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "尝试请求天气信息失败");

                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "尝试请求天气信息结束");
                    }
                });
    }

    private Observable<WeatherEnity> getWeather(long cityId) {
        WeatherApi api = new Retrofit.Builder()
                .baseUrl("http://www.weacher.com.cn")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(WeatherApi.class);
        return api.getWeather(cityId);

    }
}
