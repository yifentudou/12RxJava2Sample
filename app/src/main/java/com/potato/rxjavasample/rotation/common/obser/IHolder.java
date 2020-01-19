package com.potato.rxjavasample.rotation.common.obser;

import io.reactivex.observables.ConnectableObservable;

/**
 * Created by li.zhirong on 2020/1/19
 * 我们声明一个接口，用于Fragment向Activity一个ConnectableObservable，
 * 使得Activity可以监听到Fragment中后台任务的工作进度。
 */
public interface IHolder {
    public void onWorkerPrepared(ConnectableObservable<String> workerFlow);
}
