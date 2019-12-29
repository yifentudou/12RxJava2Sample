package com.potato.rxjavasample.Retrofig_RxJava2.api;

import com.potato.rxjavasample.Retrofig_RxJava2.entity.NewsEntity;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NewApi {
    @GET("api/data/{category}/{count}/{page}")
    Observable<NewsEntity> getNews(@Path("category") String category, @Path("count") int count, @Path("page") int page);
}
