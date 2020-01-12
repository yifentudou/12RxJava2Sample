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
import io.reactivex.android.schedulers.AndroidSchedulers;
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
                break;
            case R.id.tv_3:
                break;
            case R.id.tv_4:
                break;

        }
    }

    private void refreshArtcleUseConcat() {
        Observable<List<NewsResultEntity>> contactObservable = Observable.concat(getCacheArticle(500).subscribeOn(Schedulers.io()), getNetworkArticle(2000).subscribeOn(Schedulers.io()));

        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArtcleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
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
                        list.add(entity);

                    }
                    emitter.onNext(list);
                    emitter.onComplete();
                    Log.i(TAG, "结束加载网络数据");

                } catch (InterruptedException e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
            }
        });
    }

    private DisposableObserver<List<NewsResultEntity>> getArtcleObserver() {
        return new DisposableObserver<List<NewsResultEntity>>() {
            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                mNewsResultEntities.clear();
                mNewsResultEntities.addAll(newsResultEntities);
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
