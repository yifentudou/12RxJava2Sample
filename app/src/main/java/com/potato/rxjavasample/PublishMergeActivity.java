package com.potato.rxjavasample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.potato.rxjavasample.Retrofig_RxJava2.adapter.NewsAdapter;
import com.potato.rxjavasample.Retrofig_RxJava2.entity.NewsResultEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class PublishMergeActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_1;
    private TextView tv_2;
    private TextView tv_3;
    private TextView tv_4;
    private NewsAdapter mNewsAdapter;
    private List<NewsResultEntity> mNewsResultEntities = new ArrayList<>();
    private static final String TAG = PublishMergeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_merge);
        tv_1 = findViewById(R.id.tv_1);
        tv_2 = findViewById(R.id.tv_2);
        tv_3 = findViewById(R.id.tv_3);
        tv_4 = findViewById(R.id.tv_4);
        tv_1.setOnClickListener(this);
        tv_2.setOnClickListener(this);
        tv_3.setOnClickListener(this);
        tv_4.setOnClickListener(this);
        RecyclerView re = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        re.setLayoutManager(layoutManager);
        mNewsAdapter = new NewsAdapter(mNewsResultEntities, this);
        re.setAdapter(mNewsAdapter);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_1:
                refreshArtcleUseConcat();
                break;
            case R.id.tv_2:
                refreshArtcleUseConcatEager();
                break;
            case R.id.tv_3:
                refreshArtcleUseMerge();
                break;
            case R.id.tv_4:
                refreshArtcleUsePublish();
                break;

        }
    }

    /**
     * 使用concat,会按照先后顺序一次发送数据；
     * 它会连接多个Observable，并且必须要等到前一个Observable的所有数据项都发送完之后，才会开始下一个Observable数据的发送
     * 缺点：加载时间过长，浪费时间
     */
    private void refreshArtcleUseConcat() {
        Observable<List<NewsResultEntity>> contactObservable = Observable.concat(getCacheArticle(500).subscribeOn(Schedulers.io()), getNetworkArticle(2000).subscribeOn(Schedulers.io()));

        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArtcleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    /**
     * 使用concatEager,会同时发送几个被观察者请求，但是结果还是得按照加入顺序返回，
     * 如果后加入的先返回数据，则需要等待上游数据返回，再呈现
     * 它和concat最大的不同就是多个Observable可以同时开始发射数据，如果后一个Observable发射完成后，前一个Observable还没发射完数据，那么它会将后一个Observable的数据先缓存起来，等到前一个Observable发射完毕后，才将缓存的数据发射出去。
     * 缺点：浪费时间（如果先返回网络数据，则需要等待缓存数据返回并显示后，才返回网络数据）
     */
    private void refreshArtcleUseConcatEager() {
        List<Observable<List<NewsResultEntity>>> observables = new ArrayList<>();
        observables.add(getCacheArticle(500).subscribeOn(Schedulers.io()));
        observables.add(getNetworkArticle(2000).subscribeOn(Schedulers.io()));

        Observable<List<NewsResultEntity>> contactObservable = Observable.concatEager(observables);
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArtcleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    /**
     * 它和concatEager一样，会让多个Observable同时开始发射数据，但是它不需要Observable之间的互相等待，而是直接发送给下游。
     * 缺点：如果先返回网络，后返回缓存数据，缓存数据会覆盖网络数据，与实际不符
     */
    private void refreshArtcleUseMerge() {

        Observable<List<NewsResultEntity>> mergeObservable = Observable.merge(getCacheArticle(2000).subscribeOn(Schedulers.io()), getNetworkArticle(500).subscribeOn(Schedulers.io()));

        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArtcleObserver();
        mergeObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    /**
     * 满足需求
     * 同时请求网络和缓存，网络先返回的话，直接舍弃缓存数据，如果缓存先返回，显示缓存再显示网络数据
     * takeUntil：
     * 我们给sourceObservable通过takeUntil传入了另一个otherObservable，它表示sourceObservable在otherObservable发射数据之后，就不允许再发射数据了，这就刚好满足了我们前面说的“只要网络源发送了数据，那么缓存源就不应再发射数据”。
     * 之后，我们再用前面介绍过的merge操作符，让两个缓存源和网络源同时开始工作，去取数据。
     *
     * 但是上面有一点缺陷，就是调用merge和takeUntil会发生两次订阅，这时候就需要使用publish操作符。
     * 它接收一个Function函数，该函数返回一个Observable，该Observable是对原Observable，
     * 也就是上面网络源的Observable转换之后的结果，该Observable可以被takeUntil和merge操作符所共享，从而实现只订阅一次的效果。
     *
     */
    private void refreshArtcleUsePublish() {

        Observable<List<NewsResultEntity>> publishObservable = getNetworkArticle(2000).subscribeOn(Schedulers.io()).publish(new Function<Observable<List<NewsResultEntity>>, ObservableSource<List<NewsResultEntity>>>() {
            @Override
            public ObservableSource<List<NewsResultEntity>> apply(Observable<List<NewsResultEntity>> network) throws Exception {

                return Observable.merge(network, getCacheArticle(500).subscribeOn(Schedulers.io()).takeUntil(network));
            }
        });
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArtcleObserver();
        publishObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    private Observable<List<NewsResultEntity>> getCacheArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> emitter) throws Exception {
                try {
                    Log.e(TAG, "开始加载缓存数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> results = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("缓存");
                        entity.setDesc("序号=" + i);
                        entity.setCache(true);
                        results.add(entity);
                    }
                    emitter.onNext(results);
                    emitter.onComplete();
                    Log.i(TAG, "结束加载缓存数据");
                } catch (InterruptedException e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
            }
        });
    }

    //模拟网络数据源
    private Observable<List<NewsResultEntity>> getNetworkArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> emitter) throws Exception {
                try {

                    Log.i(TAG, "开始加载网络数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> list = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("网络");
                        entity.setDesc("序号=" + i);
                        entity.setCache(false);
                        list.add(entity);

                    }
                    //正常情况
                    emitter.onNext(list);
                    emitter.onComplete();
                    //异常情况
//                    emitter.onError(new Throwable("network error"));
                    Log.i(TAG, "结束加载网络数据");

                } catch (InterruptedException e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
            }
        }).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends List<NewsResultEntity>>>() {
            @Override
            public ObservableSource<? extends List<NewsResultEntity>> apply(Throwable throwable) throws Exception {
                Log.i(TAG, "网络请求发生错误：" + throwable);
                return Observable.never();
            }
        });
    }

    private DisposableObserver<List<NewsResultEntity>> getArtcleObserver() {
        return new DisposableObserver<List<NewsResultEntity>>() {
            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                mNewsResultEntities.clear();
                mNewsResultEntities.addAll(newsResultEntities);
                if (newsResultEntities.get(0).isCache()) {
                    Log.i(TAG, "展示数据，数据类型=缓存");
                } else {
                    Log.i(TAG, "展示数据，数据类型=网络");
                }


                mNewsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "加载错误，e=" + e);
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "加载完成");
            }
        };
    }
}
