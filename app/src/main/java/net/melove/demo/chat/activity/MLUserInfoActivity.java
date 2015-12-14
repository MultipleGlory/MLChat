package net.melove.demo.chat.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.easemob.chat.EMContactManager;
import com.easemob.exceptions.EaseMobException;

import net.melove.demo.chat.R;
import net.melove.demo.chat.db.MLUserDao;
import net.melove.demo.chat.info.MLUserInfo;
import net.melove.demo.chat.widget.MLImageView;
import net.melove.demo.chat.widget.MLToast;

/**
 * Created by lzan13 on 2015/8/29.
 */
public class MLUserInfoActivity extends MLBaseActivity {

    private Activity mActivity;


    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_info);

        init();
        initToolbar();
        initView();
    }

    private void init() {
        mActivity = this;
//        mUserDao = new MLUserDao(mActivity);
    }

    /**
     * 初始化Toolbar组件
     */
    private void initToolbar() {
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.ml_widget_collapsing);
        mCollapsingToolbarLayout.setTitle(mActivity.getResources().getString(R.string.ml_username));

        mToolbar = (Toolbar) findViewById(R.id.ml_widget_toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.ml_white));
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_arrow);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.finishAfterTransition();
            }
        });

    }

    private void initView() {

    }

    private View.OnClickListener viewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case -1:
                    mActivity.finishAfterTransition();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 添加好友
     */
    private void addContact() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String reason = "加个好友呗";
                try {
                    EMContactManager.getInstance().addContact("", reason);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MLToast.makeToast("发送好友请求成功，等待对方同意^_^").show();
                        }
                    });
                } catch (EaseMobException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MLToast.makeToast("发送好友请求失败-_-||").show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 发起聊天
     */
    private void startChat() {
        Intent intent = new Intent();
        intent.setClass(mActivity, MLChatActivity.class);
//        intent.putExtra("username", mUsername);
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
