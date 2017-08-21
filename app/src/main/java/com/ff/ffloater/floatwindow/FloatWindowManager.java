package com.ff.ffloater.floatwindow;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ff.ffloater.MainApplication;
import com.ff.ffloater.R;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;

/**
 * Created by feifan on 2017/8/21.
 * Contacts me:404619986@qq.com
 */

public class FloatWindowManager {
    @SuppressLint("StaticFieldLeak")
    private static FloatWindowManager sInstance = new FloatWindowManager();
    private WindowManager mWindowManager;
    private Context mContext;

    //小窗布局相关
    private View mSmallWindow;
    private WindowManager.LayoutParams mSmallWindowParams;
    private DisplayMetrics mDisplayMetrics;
    private int mVideoViewWidth;
    private int mVideoViewHeight;
    private int mStatusBarHeight;
    private int mNavigationBarHeight;
    private int dp12;
    private boolean isAddToWindow = false;

    private FloatWindowManager() {
        mContext = MainApplication.Companion.getInstance();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mStatusBarHeight = SystemBarUtils.getStatusBarHeight(mContext);
        mNavigationBarHeight = SystemBarUtils.getNavigationBarHeight(mContext);
        createSmallWindow();
    }

    public static FloatWindowManager getInstance() {
        return sInstance;
    }

    public void showFloatWindow(Context activity) {
        if (!FloatWindowPermissionChecker.checkFloatWindowPermission()) {
            FloatWindowPermissionChecker.askForFloatWindowPermission(activity);
            return;
        }
        if (isAddToWindow) return;
        try {
            mWindowManager.addView(mSmallWindow, mSmallWindowParams);
        } catch (Exception e) {
            mWindowManager.updateViewLayout(mSmallWindow, mSmallWindowParams);
        }
        isAddToWindow = true;
    }

    public void removeFromWindow() {
        if (!isAddToWindow) return;
        mWindowManager.removeView(mSmallWindow);
        isAddToWindow = false;
    }

    private void createSmallWindow() {
        mSmallWindow = LayoutInflater.from(mContext).inflate(R.layout.layout_float_window, null);
        FakeVideoView fakeVideoView = (FakeVideoView) mSmallWindow.findViewById(R.id.video);
        fakeVideoView.setOnTouchHandler(new FakeVideoView.onTouchHandler() {
            @Override
            public void onMove(float moveX, float moveY) {
                mSmallWindowParams.x += moveX;
                mSmallWindowParams.y += moveY;
                if (isAddToWindow) {
                    mWindowManager.updateViewLayout(mSmallWindow, mSmallWindowParams);
                }
            }

            @Override
            public void onMoveEnd() {
                final int currentX = mSmallWindowParams.x;
                final int currentY = mSmallWindowParams.y;
                int dx = 0;
                int dy = 0;
                if (mSmallWindowParams.x > mDisplayMetrics.widthPixels - mVideoViewWidth) {
                    dx = (mDisplayMetrics.widthPixels - mVideoViewWidth) - mSmallWindowParams.x;
                } else if (mSmallWindowParams.x < 0) {
                    dx = -mSmallWindowParams.x;
                }
                if (mSmallWindowParams.y > mDisplayMetrics.heightPixels - mVideoViewHeight - mStatusBarHeight) {
                    dy = (mDisplayMetrics.heightPixels - mVideoViewHeight - mStatusBarHeight) - mSmallWindowParams.y;
                } else if (mSmallWindowParams.y < 0) {
                    dy = -mSmallWindowParams.y;
                }
                if (dx == 0 && dy == 0) {
                    return;
                }
                ValueAnimator x = ValueAnimator.ofInt(0, dx).setDuration(300);
                x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mSmallWindowParams.x = currentX + (int) animation.getAnimatedValue();
                    }
                });
                ValueAnimator y = ValueAnimator.ofInt(0, dy).setDuration(300);
                y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mSmallWindowParams.y = currentY + (int) animation.getAnimatedValue();
                        if (isAddToWindow) {
                            mWindowManager.updateViewLayout(mSmallWindow, mSmallWindowParams);
                        }
                    }
                });
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(x).with(y);
                animatorSet.start();
            }
        });
        mSmallWindow.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFromWindow();
            }
        });

        ViewGroup.LayoutParams videoLayoutParams = fakeVideoView.getLayoutParams();
        mDisplayMetrics = Resources.getSystem().getDisplayMetrics();
        mVideoViewWidth = (int) (mDisplayMetrics.widthPixels * 0.65);
        mVideoViewHeight = (int) (mVideoViewWidth / 16.0 * 9.0) + 1;
        videoLayoutParams.width = mVideoViewWidth;
        videoLayoutParams.height = mVideoViewHeight;
        fakeVideoView.setLayoutParams(videoLayoutParams);

        dp12 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mDisplayMetrics);
        mSmallWindowParams = new WindowManager.LayoutParams();
        mSmallWindowParams.width = WRAP_CONTENT;
        mSmallWindowParams.height = WRAP_CONTENT;
        mSmallWindowParams.gravity = Gravity.TOP | Gravity.START;
        mSmallWindowParams.x = Resources.getSystem().getDisplayMetrics().widthPixels - mVideoViewWidth - dp12;
        mSmallWindowParams.y = Resources.getSystem().getDisplayMetrics().heightPixels - mVideoViewHeight - dp12 - mStatusBarHeight - mNavigationBarHeight;

        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 23) {
            mSmallWindowParams.type = TYPE_TOAST;
        } else {
            mSmallWindowParams.type = TYPE_SYSTEM_ALERT;
        }
        mSmallWindowParams.flags = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_HARDWARE_ACCELERATED | FLAG_LAYOUT_NO_LIMITS;
        mSmallWindowParams.format = PixelFormat.RGBA_8888;
        mSmallWindowParams.windowAnimations = android.R.style.Animation_Translucent;
    }
}
