package net.melove.demo.chat.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMError;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMTextMessageBody;

import net.melove.demo.chat.R;
import net.melove.demo.chat.common.util.MLCrypto;
import net.melove.demo.chat.common.util.MLDate;
import net.melove.demo.chat.common.util.MLLog;
import net.melove.demo.chat.common.util.MLSPUtil;
import net.melove.demo.chat.contacts.MLInvitedEntity;
import net.melove.demo.chat.contacts.MLUserEntity;
import net.melove.demo.chat.conversation.MLConversationExtUtils;
import net.melove.demo.chat.database.MLInvitedDao;
import net.melove.demo.chat.database.MLUserDao;
import net.melove.demo.chat.notification.MLNotifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lzan13 on 2015/7/13.
 */
public class MLEasemobHelper {

    // 上下文对象
    private Context mContext;

    // MLEasemobHelper 单例对象
    private static MLEasemobHelper instance;

    // 记录sdk是否初始化
    private boolean isInit;

    // 保存当前Activity列表
    private List<Activity> mActivityList = new ArrayList<Activity>();

    // 环信的消息监听器
    private EMMessageListener mMessageListener;

    // 申请与邀请类消息的数据库操作类
    private MLInvitedDao mInvitedDao;
    // 用户信息数据库操作类
    private MLUserDao mUserDao;
    // 环信联系人监听
    private EMContactListener mContactListener;
    // 环信连接监听
    private EMConnectionListener mConnectionListener;
    // 环信群组变化监听
    private EMGroupChangeListener mGroupChangeListener;

    // App内广播管理器，为了安全，这里使用本地广播
    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * 单例类，用来初始化环信的sdk
     *
     * @return 返回当前类的实例
     */
    public static MLEasemobHelper getInstance() {
        if (instance == null) {
            instance = new MLEasemobHelper();
        }
        return instance;
    }

    /**
     * 私有的构造方法
     */
    private MLEasemobHelper() {
    }

    /**
     * 初始化环信的SDK
     *
     * @param context
     * @return 返回初始化状态是否成功
     */
    public synchronized boolean initEasemob(Context context) {
        mContext = context;

        mUserDao = new MLUserDao(mContext);
        mInvitedDao = new MLInvitedDao(mContext);

        // 获取App内广播接收器实例
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        // 获取当前进程 id
        int pid = android.os.Process.myPid();
        String processAppName = getAppName(pid);
        // 如果app启用了远程的service，此application:onCreate会被调用2次
        // 为了防止环信SDK被初始化2次，加此判断会保证SDK被初始化1次
        // 默认的app会在以包名为默认的process name下运行，如果查到的process name不是app的process name就立即返回
        if (processAppName == null || !processAppName.equalsIgnoreCase(context.getPackageName())) {
            // 则此application的onCreate 是被service 调用的，直接返回
            return true;
        }
        if (isInit) {
            return isInit;
        }
        mContext = context;

        /**
         * SDK初始化的一些配置
         */
        EMOptions options = new EMOptions();
        // 设置自动登录
        options.setAutoLogin(true);
        // 设置是否需要发送已读回执
        options.setRequireAck(true);
        // 设置是否需要发送回执
        options.setRequireDeliveryAck(true);
        // 设置初始化数据库DB时，每个会话要加载的Message数量
        options.setNumberOfMessagesLoaded(1);
        // 添加好友是否自动同意，如果是自动同意就不会收到好友请求，因为sdk会自动处理
        options.setAcceptInvitationAlways(false);
        // 设置集成小米推送的appid和appkey
        //        String APP_ID = "2882303761517430984";
        //        String APP_KEY = "5191743065984";
        //        options.setMipushConfig(APP_ID, APP_KEY);

        // 调用初始化方法初始化sdk
        EMClient.getInstance().init(mContext, options);

        // 设置开启debug模式
        EMClient.getInstance().setDebugMode(true);

        // 初始化全局监听
        initGlobalListener();

        // 初始化完成
        isInit = true;
        return isInit;
    }


    /**
     * 初始化环信的一些监听
     */
    public void initGlobalListener() {
        // 设置全局的连接监听
        setConnectionListener();
        // 初始化全局消息监听
        setMessageListener();
        // 设置全局的联系人变化监听
        setContactListener();
        // 设置全局的群组变化监听
        setGroupChangeListener();
    }

    /**
     * ------------------------------- Connection Listener ---------------------
     * 链接监听，监听与服务器连接状况
     */
    private void setConnectionListener() {
        mConnectionListener = new EMConnectionListener() {

            /**
             * 链接聊天服务器成功
             */
            @Override
            public void onConnected() {
                MLLog.d("onConnected");
            }

            /**
             * 链接聊天服务器失败
             *
             * @param errorCode
             */
            @Override
            public void onDisconnected(final int errorCode) {
                if (errorCode == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                    MLLog.d("被踢，多次初始化也可能出现-" + errorCode);
                } else if (errorCode == EMError.USER_REMOVED) {
                    MLLog.d("账户移除-" + errorCode);
                } else {
                    MLLog.d("连接不到服务器-" + errorCode);
                }
            }
        };
    }

    /**
     * ---------------------------------- Message Listener ----------------------------
     * 初始化全局的消息监听
     */
    protected void setMessageListener() {
        mMessageListener = new EMMessageListener() {
            /**
             * 收到新消息，离线消息也都是在这里获取
             * 这里在处理消息监听时根据收到的消息修改了会话对象的最后时间，是为了在会话列表中当清空了会话内容时，
             * 不用过滤掉空会话，并且能显示会话时间
             * {@link MLConversationExtUtils#setConversationLastTime(EMConversation)}
             *
             * @param list 收到的新消息集合
             */
            @Override
            public void onMessageReceived(List<EMMessage> list) {
                EMConversation conversation = null;
                for (EMMessage message : list) {
                    // 根据消息类型来获取回话对象
                    if (message.getChatType() == EMMessage.ChatType.Chat) {
                        conversation = EMClient.getInstance().chatManager().getConversation(message.getFrom());
                    } else {
                        conversation = EMClient.getInstance().chatManager().getConversation(message.getTo());
                    }
                    // 设置会话的最后时间
                    MLConversationExtUtils.setConversationLastTime(conversation);
                }
                // 发送广播，通知需要刷新UI等操作的地方
                mLocalBroadcastManager.sendBroadcast(new Intent(MLConstants.ML_ACTION_MESSAGE));
            }

            /**
             * 收到新的 CMD 消息
             *
             * @param list
             */
            @Override
            public void onCmdMessageReceived(List<EMMessage> list) {
                for (EMMessage cmdMessage : list) {
                    EMCmdMessageBody body = (EMCmdMessageBody) cmdMessage.getBody();
                    // 判断是不是撤回消息的透传
                    if (body.action().equals(MLConstants.ML_ATTR_RECALL)) {
                        MLEasemobHelper.getInstance().receiveRecallMessage(cmdMessage);
                    }
                }
                // 发送广播，通知需要刷新UI等操作的地方
                mLocalBroadcastManager.sendBroadcast(new Intent(MLConstants.ML_ACTION_MESSAGE));
            }

            /**
             * 收到新的已读回执
             *
             * @param list 收到消息已读回执
             */
            @Override
            public void onMessageReadAckReceived(List<EMMessage> list) {
            }

            /**
             * 收到新的发送回执
             *
             * @param list 收到发送回执的消息集合
             */
            @Override
            public void onMessageDeliveryAckReceived(List<EMMessage> list) {
            }

            /**
             * 消息的状态改变
             *
             * @param message 发生改变的消息
             * @param object  包含改变的消息
             */
            @Override
            public void onMessageChanged(EMMessage message, Object object) {
            }
        };
        // 注册消息监听
        EMClient.getInstance().chatManager().addMessageListener(mMessageListener);
    }

    /**
     * ---------------------------------- Contact Listener -------------------------------
     * 联系人监听，用来监听联系人的请求与变化等
     */
    private void setContactListener() {

        mContactListener = new EMContactListener() {

            /**
             * 监听到添加联系人
             *
             * @param username 被添加的联系人
             */
            @Override
            public void onContactAdded(String username) {
                MLUserEntity user = new MLUserEntity();
                user.setUserName(username);
                mUserDao.saveContacts(user);
            }

            /**
             * 监听删除联系人
             * @param username 被删除的联系人
             */
            @Override
            public void onContactDeleted(String username) {

            }

            /**
             * 收到对方联系人申请
             *
             * @param username
             * @param reason
             */
            @Override
            public void onContactInvited(String username, String reason) {
                MLLog.d("onContactInvited");

                // 创建一条好友申请数据
                MLInvitedEntity invitedEntity = new MLInvitedEntity();
                invitedEntity.setUserName(username);
                //                invitedEntity.setNickName(mUserEntity.getNickName());
                invitedEntity.setReason(reason);
                invitedEntity.setStatus(MLInvitedEntity.InvitedStatus.BEAPPLYFOR);
                invitedEntity.setType(0);
                invitedEntity.setCreateTime(MLDate.getCurrentMillisecond());
                invitedEntity.setObjId(MLCrypto.cryptoStr2MD5(invitedEntity.getUserName() + invitedEntity.getType()));

                /**
                 * 这里先读取本地的申请与通知信息
                 * 如果相同则直接 return，不进行操作
                 * 只有当新的好友请求发过来时才进行保存，并发送通知
                 */
                // 这里进行一下筛选，如果已存在则去更新本地内容
                MLInvitedEntity temp = mInvitedDao.getInvitedEntiry(invitedEntity.getObjId());
                if (temp != null) {
                    if (temp.getReason().equals(invitedEntity.getReason())) {
                        // 这里判断当前保存的信息如果和新的一模一样不进行操作
                        return;
                    }
                    mInvitedDao.updateInvited(invitedEntity);
                } else {
                    mInvitedDao.saveInvited(invitedEntity);
                }
                // 调用发送通知栏提醒方法，提醒用户查看申请通知
                MLNotifier.getInstance(MLApplication.getContext()).sendInvitedNotification(invitedEntity);
                mLocalBroadcastManager.sendBroadcast(new Intent(MLConstants.ML_ACTION_INVITED));
            }

            /**
             * 对方同意了自己的申请
             *
             * @param username 对方的username
             */
            @Override
            public void onContactAgreed(String username) {
                MLLog.d("onContactAgreed %", username);

                // 这里进行一下筛选，如果已存在则去更新本地内容
                MLInvitedEntity temp = mInvitedDao.getInvitedEntiry(MLCrypto.cryptoStr2MD5(username + 0));
                if (temp != null) {
                    temp.setStatus(MLInvitedEntity.InvitedStatus.BEAGREED);
                    mInvitedDao.updateInvited(temp);
                } else {
                    // 创建一条好友申请数据
                    MLInvitedEntity invitedEntity = new MLInvitedEntity();
                    invitedEntity.setUserName(username);
                    //                invitedEntity.setNickName(mUserEntity.getNickName());
                    invitedEntity.setReason(MLApplication.getContext().getString(R.string.ml_add_contact_reason));
                    invitedEntity.setStatus(MLInvitedEntity.InvitedStatus.BEAGREED);
                    invitedEntity.setType(0);
                    invitedEntity.setCreateTime(MLDate.getCurrentMillisecond());
                    invitedEntity.setObjId(MLCrypto.cryptoStr2MD5(invitedEntity.getUserName() + invitedEntity.getType()));
                    mInvitedDao.saveInvited(invitedEntity);
                }
                // 调用发送通知栏提醒方法，提醒用户查看申请通知
                MLNotifier.getInstance(MLApplication.getContext()).sendInvitedNotification(temp);
                mLocalBroadcastManager.sendBroadcast(new Intent(MLConstants.ML_ACTION_INVITED));
            }

            /**
             * 对方拒绝了联系人申请
             *
             * @param username 对方的username
             */
            @Override
            public void onContactRefused(String username) {
                MLLog.d("onContactRefused");
                // 这里进行一下筛选，如果已存在则去更新本地内容
                MLInvitedEntity temp = mInvitedDao.getInvitedEntiry(MLCrypto.cryptoStr2MD5(username + 0));
                if (temp != null) {
                    temp.setStatus(MLInvitedEntity.InvitedStatus.BEREFUSED);
                    mInvitedDao.updateInvited(temp);
                } else {
                    // 创建一条好友申请数据
                    MLInvitedEntity invitedEntity = new MLInvitedEntity();
                    invitedEntity.setUserName(username);
                    //                invitedEntity.setNickName(mUserEntity.getNickName());
                    invitedEntity.setReason(MLApplication.getContext().getString(R.string.ml_add_contact_reason));
                    invitedEntity.setStatus(MLInvitedEntity.InvitedStatus.BEREFUSED);
                    invitedEntity.setType(0);
                    invitedEntity.setCreateTime(MLDate.getCurrentMillisecond());
                    invitedEntity.setObjId(MLCrypto.cryptoStr2MD5(invitedEntity.getUserName() + invitedEntity.getType()));
                    mInvitedDao.saveInvited(invitedEntity);
                }
                // 调用发送通知栏提醒方法，提醒用户查看申请通知
                MLNotifier.getInstance(MLApplication.getContext()).sendInvitedNotification(temp);
                mLocalBroadcastManager.sendBroadcast(new Intent(MLConstants.ML_ACTION_INVITED));
            }
        };
    }

    /**
     * ------------------------------------- Group Listener -------------------------------------
     * 群组变化监听，用来监听群组请求，以及其他群组情况
     */
    private void setGroupChangeListener() {
        mGroupChangeListener = new EMGroupChangeListener() {

            /**
             * 收到其他用户邀请加入群组
             *
             * @param groupId   要加入的群的id
             * @param groupName 要加入的群的名称
             * @param inviter   邀请者
             * @param reason    邀请理由
             */
            @Override
            public void onInvitationReceived(String groupId, String groupName, String inviter, String reason) {

            }

            /**
             * 用户申请加入群组
             *
             * @param groupId   要加入的群的id
             * @param groupName 要加入的群的名称
             * @param applyer   申请人的username
             * @param reason    申请加入的reason
             */
            @Override
            public void onApplicationReceived(String groupId, String groupName, String applyer, String reason) {

            }

            @Override
            public void onApplicationAccept(String s, String s1, String s2) {

            }

            @Override
            public void onApplicationDeclined(String s, String s1, String s2, String s3) {

            }

            @Override
            public void onInvitationAccpted(String s, String s1, String s2) {

            }

            @Override
            public void onInvitationDeclined(String s, String s1, String s2) {

            }

            @Override
            public void onUserRemoved(String s, String s1) {

            }

            @Override
            public void onGroupDestroy(String s, String s1) {

            }


            @Override
            public void onAutoAcceptInvitationFromGroup(String s, String s1, String s2) {

            }
        };
    }

    /**
     * 发送一条撤回消息的透传，这里需要和接收方协商定义，通过一个透传，并加上扩展去实现消息的撤回
     *
     * @param message  需要撤回的消息
     * @param callBack 发送消息的回调，通知调用方发送撤回消息的结果
     */
    public void sendRecallMessage(final EMMessage message, final EMCallBack callBack) {
        boolean result = false;
        // 获取当前时间，用来判断后边撤回消息的时间点是否合法，这个判断不需要在接收方做，
        // 因为如果接收方之前不在线，很久之后才收到消息，将导致撤回失败
        long currTime = MLDate.getCurrentMillisecond();
        long msgTime = message.getMsgTime();
        if (currTime < msgTime || (currTime - msgTime > 120000)) {
            callBack.onError(0, "time");
            return;
        }
        // 获取消息 id，作为撤回消息的参数
        String msgId = message.getMsgId();
        // 创建一个CMD 类型的消息，将需要撤回的消息通过这条CMD消息发送给对方
        EMMessage cmdMessage = EMMessage.createSendMessage(EMMessage.Type.CMD);
        // 判断下消息类型，如果是群聊就设置为群聊
        if (message.getChatType() == EMMessage.ChatType.GroupChat) {
            cmdMessage.setChatType(EMMessage.ChatType.GroupChat);
        }
        // 设置消息接收者
        cmdMessage.setReceipt(message.getTo());
        // 创建CMD 消息的消息体 并设置 action 为 recall
        String action = MLConstants.ML_ATTR_RECALL;
        EMCmdMessageBody body = new EMCmdMessageBody(action);
        cmdMessage.addBody(body);
        // 设置消息的扩展为要撤回的 msgId
        cmdMessage.setAttribute(MLConstants.ML_ATTR_MSG_ID, msgId);
        // 确认无误，开始发送撤回消息的透传
        cmdMessage.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                // 更改要撤销的消息的内容，替换为消息已经撤销的提示内容
                EMTextMessageBody body = new EMTextMessageBody("此条消息已撤回");
                EMMessage recallMessage = EMMessage.createSendMessage(EMMessage.Type.TXT);
                recallMessage.addBody(body);
                recallMessage.setReceipt(message.getTo());
                // 设置扩展为撤回消息类型，是为了区分消息的显示
                recallMessage.setAttribute(MLConstants.ML_ATTR_RECALL, true);
                // 返回修改消息结果
                EMClient.getInstance().chatManager().updateMessage(recallMessage);
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
            }

            @Override
            public void onProgress(int i, String s) {
            }
        });
        // 准备工作完毕，发送消息
        EMClient.getInstance().chatManager().sendMessage(cmdMessage);
    }

    /**
     * 收到撤回消息，这里需要和发送方协商定义，通过一个透传，并加上扩展去实现消息的撤回
     *
     * @param cmdMessage 收到的透传消息，包含需要撤回的消息的 msgId
     * @return 返回撤回结果是否成功
     */
    public boolean receiveRecallMessage(EMMessage cmdMessage) {
        boolean result = false;
        // 从cmd扩展中获取要撤回消息的id
        String msgId = cmdMessage.getStringAttribute(MLConstants.ML_ATTR_MSG_ID, null);
        if (msgId == null) {
            MLLog.d("recall - 3 %s", msgId);
            return result;
        }
        // 根据得到的msgId 去本地查找这条消息，如果本地已经没有这条消息了，就不用撤回
        EMMessage message = EMClient.getInstance().chatManager().getMessage(msgId);
        if (message == null) {
            MLLog.d("recall - 3 message is null %s", msgId);
            return result;
        }

        /**
         * 创建一条接收方的消息，因为最新版SDK不支持setType，所以其他类型消息无法更新为TXT类型，
         * 这里只能新建消息，并且设置消息类型为TXT，
         */
        // 更改要撤销的消息的内容，替换为消息已经撤销的提示内容
        EMTextMessageBody body = new EMTextMessageBody("此条消息已被 " + message.getUserName() + " 撤回");
        EMMessage recallMessage = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
        recallMessage.addBody(body);
        recallMessage.setFrom(message.getFrom());
        // 设置扩展为撤回消息类型，是为了区分消息的显示
        message.setAttribute(MLConstants.ML_ATTR_RECALL, true);
        // 返回修改消息结果
        result = EMClient.getInstance().chatManager().updateMessage(recallMessage);
        return result;
    }

    /**
     * 退出登录环信
     *
     * @param callback 退出登录的回调函数，用来给上次回调退出状态
     */
    public void signOut(final EMCallBack callback) {
        MLSPUtil.remove(mContext, MLConstants.ML_SHARED_USERNAME);
        MLSPUtil.remove(mContext, MLConstants.ML_SHARED_PASSWORD);
        EMClient.getInstance().logout(true, new EMCallBack() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(int i, String s) {
                if (callback != null) {
                    callback.onError(i, s);
                }
            }

            @Override
            public void onProgress(int i, String s) {
                if (callback != null) {
                    callback.onProgress(i, s);
                }
            }
        });
    }

    /**
     * 判断是否登录成功过，并且没有调用logout和被踢
     *
     * @return 返回一个boolean值 表示是否登录成功过
     */
    public boolean isLogined() {
        return EMClient.getInstance().isLoggedInBefore();
    }

    /**
     * 添加 Activity 到集合，为了给全局监听用来判断当前是否在 Activity 界面
     *
     * @param activity
     */
    public void addActivity(Activity activity) {
        if (!mActivityList.contains(activity)) {
            mActivityList.add(0, activity);
        }
    }

    /**
     * 将 Activity 从集合中移除
     *
     * @param activity
     */
    public void removeActivity(Activity activity) {
        if (mActivityList.contains(activity)) {
            mActivityList.remove(activity);
        }
    }

    /**
     * 根据Pid获取当前进程的名字，一般就是当前app的包名
     *
     * @param pid 进程的id
     * @return 返回进程的名字
     */
    private String getAppName(int pid) {
        String processName = null;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List list = activityManager.getRunningAppProcesses();
        Iterator i = list.iterator();
        PackageManager pm = mContext.getPackageManager();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pid) {
                    CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
                    // Log.d("Process", "Id: "+ info.pid +" ProcessName: "+
                    // info.processName +"  Label: "+c.toString());
                    // processName = c.toString();
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                MLLog.e(e.toString());
            }
        }
        return processName;
    }

}
