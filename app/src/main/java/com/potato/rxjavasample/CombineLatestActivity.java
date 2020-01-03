package com.potato.rxjavasample;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.subjects.PublishSubject;


/**
 * 在平时的应用中，我们经常会让用户输入一些信息，最常见的莫过于注册或者登录界面中，让用户输入用户名或者密码，
 * 但是我们经常会对用户名或者密码有一定的要求，只有当它们同时满足要求时，才允许用户进行下一步的操作。
 * <p>
 * 这个需求就涉及到一种模型，即在多个地方监听变化，但是在一个地方进行统一验证，
 * 如果验证成功，那么允许用户进行下一步的操作，否则提示用户输入不正确。
 * <p>
 * 在下面这个示例中，包含了两个输入框，分别对应用户名和密码，
 * 它们的长度要求分别为2~8和4~16，如果两者都正确，那么登录按钮的文案变为“登录”，否则显示“用户名或密码无效”。
 * <p>
 * 在上面的例子中，我们首先创建了两个PublishSubject，分别用于用户名和密码的订阅，然后通过combineLatest对这两个PublishSubject进行组合。
 * 这样，当任意一个PublishSubject发送事件之后，就会回调combineLatest最后一个函数的apply方法，
 * 该方法会取到每个被观察的PublishSubject最后一次发射的数据，我们通过该数据进行验证。
 * <p>
 * 该操作符接受多个Observable以及一个函数作为参数，并且函数的签名为这些Observable发射的数据类型。
 * 当以上的任意一个Observable发射数据之后，会去取其它Observable 最近一次发射的数据，
 * 回调到函数当中，但是该函数回调的前提是所有的Observable都至少发射过一个数据项。
 * <p>
 * zip和combineLatest的区别在于：
 * zip是在其中一个Observable发射数据项后，组合所有Observable最早一个未被组合的数据项，
 * 也就是说，组合后的Observable发射的第n个数据项，必然是每个源由Observable各自发射的第n个数据项构成的。
 * combineLatest则是在其中一个Observable发射数据项后，组合所有Observable所发射的最后一个数据项（前提是所有的Observable都至少发射过一个数据项）。
 */
public class CombineLatestActivity extends AppCompatActivity {

    private EditText etPwd;
    private EditText etName;
    private Button btnLogin;
    private PublishSubject<String> nameSubject;
    private PublishSubject<String> pwdSubject;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combine_latest);
        etName = (EditText) findViewById(R.id.et_name);
        etPwd = (EditText) findViewById(R.id.et_pwd);
        btnLogin = (Button) findViewById(R.id.btn_login);

        nameSubject = PublishSubject.create();
        pwdSubject = PublishSubject.create();
        etName.addTextChangedListener(new EditTextMonitor(nameSubject));
        etPwd.addTextChangedListener(new EditTextMonitor(pwdSubject));
        Observable<Boolean> observable = Observable.combineLatest(nameSubject, pwdSubject, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String name, String pwd) throws Exception {
                int nameLen = name.length();
                int pwdLen = pwd.length();
                return nameLen >= 2 && nameLen <= 8 && pwdLen >= 4 && pwdLen <= 16;
            }
        });
        DisposableObserver<Boolean> disposableObserver = new DisposableObserver<Boolean>() {
            @Override
            public void onNext(Boolean value) {
                btnLogin.setText(value ? "登录" : "用户名或密码无效");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.subscribe(disposableObserver);
        compositeDisposable = new CompositeDisposable();

    }

    private class EditTextMonitor implements TextWatcher {
        PublishSubject<String> mPublishSubject;

        public EditTextMonitor(PublishSubject<String> mSubject) {
            mPublishSubject = mSubject;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mPublishSubject.onNext(s.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}
