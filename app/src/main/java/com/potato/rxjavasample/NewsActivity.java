package com.potato.rxjavasample;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.potato.rxjavasample.Retrofig_RxJava2.adapter.NewsAdapter;
import com.potato.rxjavasample.Retrofig_RxJava2.api.NewApi;
import com.potato.rxjavasample.Retrofig_RxJava2.entity.NewsEntity;
import com.potato.rxjavasample.Retrofig_RxJava2.entity.NewsResultEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewsActivity extends AppCompatActivity {

    private int mCurrentPage = 1;
    private NewsAdapter mNewsAdapter;
    private List<NewsResultEntity> mNewsResultEntities = new ArrayList<>();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        initView();
    }

    private void initView() {
        Button bt_refresh = (Button) findViewById(R.id.bt_refresh);
        bt_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticle(++mCurrentPage);
            }
        });
        RecyclerView re = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        re.setLayoutManager(layoutManager);
        mNewsAdapter = new NewsAdapter(mNewsResultEntities, this);
        re.setAdapter(mNewsAdapter);
        refreshArticle(mCurrentPage);
    }

    private void refreshArticle(final int page) {
        Observable<List<NewsResultEntity>> observable = Observable.just(page)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Integer, ObservableSource<List<NewsResultEntity>>>() {
                    @Override
                    public ObservableSource<List<NewsResultEntity>> apply(Integer integer) throws Exception {
                        Observable<NewsEntity> androidNews = getObservable("Android", page);
                        Observable<NewsEntity> iosNews = getObservable("iOS", page);
                        return Observable.zip(androidNews, iosNews, new BiFunction<NewsEntity, NewsEntity, List<NewsResultEntity>>() {
                            @Override
                            public List<NewsResultEntity> apply(NewsEntity androidEntity, NewsEntity iosEntity) throws Exception {
                                List<NewsResultEntity> result = new ArrayList<>();
                                result.addAll(androidEntity.getResults());
                                result.addAll(iosEntity.getResults());
                                return result;
                            }
                        });
                    }
                });

        DisposableObserver<List<NewsResultEntity>> disposableObserver = new DisposableObserver<List<NewsResultEntity>>() {
            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                mNewsResultEntities.clear();
                mNewsResultEntities.addAll(newsResultEntities);
                mNewsAdapter.notifyDataSetChanged();

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        compositeDisposable.add(disposableObserver);

    }

    private Observable<NewsEntity> getObservable(String category, int page) {
        NewApi api = new Retrofit.Builder()
                .baseUrl("http://gank.io")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(NewApi.class);
        return api.getNews(category, 20, page);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}
