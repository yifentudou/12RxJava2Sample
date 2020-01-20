package com.potato.rxjavasample.rotation.common.obser;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.potato.rxjavasample.R;

/**
 * 屏幕旋转观察生命周期
 *
 *  * 如果我们在AndroidManifest.xml中声明Activity时，没有对android:configChanges进行特殊的声明，
 *  * 那么在屏幕旋转时，会导致Activity的重建
 *
 *  *旋转屏幕前的Activity中的变量都会被销毁，但是有时候我们某些任务的执行不和Activity的生命周期绑定，
 *  这时候我们就可以利用Fragment提供的setRetainInstance方法，该方法的说明如:WorkerFragment
 *
 * 如果给Fragment设置了该标志位，那么在屏幕旋转之后，虽然它依附的Activity被销毁了，
 * 但是该Fragment的实例会被保留，并且在Activity的销毁过程中，
 * 只会调用该Fragment的onDetach方法，而不会调用onDestroy方法。
 *
 * 而在Activity重建时，会调用该Fragment实例的onAttach、onActivityCreated方法，但不会调用onCreate方法。
 *
 * 根据Fragment提供的这一特性，那么我们就可以将一些在屏幕旋转过程中，
 * 仍然需要运行的任务放在具有该属性的Fragment中执行。
 * 在 Handling Configuration Changes with Fragments 这篇文章中，作者介绍了通过这个技巧来实现了一个不被中断的AsyncTask，
 * 大家有需要了解详细说明的可以查看这篇文章。
 *
 * 作者：泽毛
 * 链接：https://www.jianshu.com/p/1d09bc2c463a
 * 来源：简书
 * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
 *  *
 *  */

public class RotationTestActivity extends AppCompatActivity {
    private static final String TAG = RotationTestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_test);
        Log.i(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "OnPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }


}
