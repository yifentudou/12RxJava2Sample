package com.potato.rxjavasample;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by li.zhirong on 2020/1/20
 */
public interface WeatherApi {
    @GET("api/data/{city}")
    Observable<WeatherEnity> getWeather(@Path("city") long cityID);
}

