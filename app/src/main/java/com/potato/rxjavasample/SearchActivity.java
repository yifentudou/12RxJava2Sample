package com.potato.rxjavasample;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * 优化搜索联想功能
 *
 * 几乎每个应用程序都提供了搜索功能，某些应用还提供了搜索联想。
 * 对于一个搜索联想功能，最基本的实现流程为：
 * 客户端通过EditText的addTextChangedListener方法监听输入框的变化，
 * 当输入框发生变化之后就会回调afterTextChanged方法，
 * 客户端利用当前输入框内的文字向服务器发起请求，
 * 服务器返回与该搜索文字关联的结果给客户端进行展示。
 *
 * 在该场景下，有几个可以优化的方面：
 * 在用户连续输入的情况下，可能会发起某些不必要的请求。例如用户输入了abc，那么按照上面的实现，客户端就会发起a、ab、abc三个请求。
 * 当搜索词为空时，不应该发起请求。
 * 如果用户依次输入了ab和abc，那么首先会发起关键词为ab请求，之后再发起abc的请求，但是abc的请求如果先于ab的请求返回，那么就会造成用户期望搜索的结果为abc，最终展现的结果却是和ab关联的。
 *
 * 使用debounce操作符，当输入框发生变化时，不会立刻将事件发送给下游，而是等待200ms，如果在这段事件内，输入框没有发生变化，那么才发送该事件；反之，则在收到新的关键词后，继续等待200ms。
 * 使用filter操作符，只有关键词的长度大于0时才发送事件给下游。
 * 使用switchMap操作符，这样当发起了abc的请求之后，即使ab的结果返回了，也不会发送给下游，从而避免了出现前面介绍的搜索词和联想结果不匹配的问题
 *
 * 1.debounce原理类似于我们在收到请求之后，发送一个延时消息给下游，如果在这段延时时间内没有收到新的请求，那么下游就会收到该消息；而如果在这段延时时间内收到来新的请求，那么就会取消之前的消息，并重新发送一个新的延时消息，以此类推。
 *           而如果在这段时间内，上游发送了onComplete消息，那么即使没有到达需要等待的时间，下游也会立刻收到该消息。
 * 2.filter的原理很简单，就是传入一个Predicate函数，其参数为上游发送的事件，只有该函数返回true时，才会将事件发送给下游，否则就丢弃该事件
 * 3.switchMap的原理是将上游的事件转换成一个或多个新的Observable，但是有一点很重要，就是如果在该节点收到一个新的事件之后，那么如果之前收到的时间所产生的Observable还没有发送事件给下游，那么下游就再也不会收到它发送的事件了。
 *
 * 作者：泽毛
 * 链接：https://www.jianshu.com/p/7995497baff5
 * 来源：简书
 * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 */

public class SearchActivity extends AppCompatActivity {

    private EditText et_search;
    private TextView tv_search_result;
    private DisposableObserver<String> disposableObserver;
    private CompositeDisposable compositeDisposable;
    private String TAG = SearchActivity.class.getSimpleName();
    private PublishSubject mPublishSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        tv_search_result = (TextView) findViewById(R.id.tv_search_result);
        et_search = (EditText) findViewById(R.id.et_search);
        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                startSearch(s.toString());
            }
        });

        mPublishSubject = PublishSubject.create();
        disposableObserver = new DisposableObserver<String>() {
            @Override
            public void onNext(String s) {
                tv_search_result.setText(s);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        mPublishSubject.debounce(200, TimeUnit.MILLISECONDS)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        return s.length() > 0;
                    }

                })
                .switchMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        return getSearchObservable(s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(disposableObserver);
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(disposableObserver);
    }

    private ObservableSource<String> getSearchObservable(final String query) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "开始请求，关键词为：" + query);
                try {
                    Thread.sleep(100 + (long) (Math.random() * 500));
                } catch (InterruptedException e) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
                Log.d(TAG, "请求结束，关键词为：" + query);
                emitter.onNext("完成搜索，关键词为：" + query);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());

    }

    private void startSearch(String query) {
      mPublishSubject.onNext(query);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}
