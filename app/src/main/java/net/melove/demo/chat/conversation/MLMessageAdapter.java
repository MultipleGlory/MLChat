package net.melove.demo.chat.conversation;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;

import net.melove.demo.chat.R;
import net.melove.demo.chat.application.MLConstants;
import net.melove.demo.chat.common.util.MLDate;
import net.melove.demo.chat.common.widget.MLImageView;

import java.util.List;


/**
 * Class ${FILE_NAME}
 * <p/>
 * Created by lzan13 on 2016/1/6 18:51.
 */
public class MLMessageAdapter extends BaseAdapter {

    // 消息类型数
    private final int MSG_TYPE_COUNT = 5;
    // 正常的消息类型
    private final int MSG_TYPE_TXT_SEND = 0;
    private final int MSG_TYPE_TXT_RECEIVED = 1;
    private final int MSG_TYPE_IMAGE_SEND = 2;
    private final int MSG_TYPE_IMAGE_RECEIVED = 3;

    // 系统级消息类型
    private final int MSG_TYPE_SYS_RECALL = 4;

    // 刷新类型
    private final int HANDLER_MSG_REFRESH = 0;
    private final int HANDLER_MSG_REFRESH_MORE = 1;

    private Context mContext;

    private LayoutInflater mInflater;

    private ListView mListView;
    private EMConversation mConversation;
    private List<EMMessage> messages;

    private MLHandler mHandler;

    /**
     * 构造方法
     *
     * @param context
     * @param chatId
     * @param listView
     */
    public MLMessageAdapter(Context context, String chatId, ListView listView) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mListView = listView;

        mHandler = new MLHandler();

        /**
         * 初始化会话对象，这里有三个参数么，
         * 第一个表示会话的当前聊天的 useranme 或者 groupid
         * 第二个是绘画类型可以为空
         * 第三个表示如果会话不存在是否创建
         */
        mConversation = EMClient.getInstance().chatManager().getConversation(chatId, null, true);
        messages = mConversation.getAllMessages();

    }

    @Override
    public int getCount() {
        return messages == null ? 0 : messages.size();
    }

    @Override
    public Object getItem(int position) {
        if (messages != null && position < messages.size()) {
            return messages.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 重写 Adapter 的 view 类型数目方法（此方法必须重写，否则在不同类型的 Item 重用时会出现错误）
     *
     * @return 返回当前 adapter 当前 ListView 最多显示的 Item 类型数
     */
    @Override
    public int getViewTypeCount() {
        return MSG_TYPE_COUNT;
    }

    /**
     * 重写 Adapter 的获取当前 Item 类型的方法（必须重写，同上）
     *
     * @param position 当前 Item 位置
     * @return 当前 Item 的类型
     */
    @Override
    public int getItemViewType(int position) {
        EMMessage message = (EMMessage) getItem(position);
        int itemType = -1;
        switch (message.getType()) {
            case TXT:
                // 判断是否为撤回类型的消息
                if (message.getBooleanAttribute(MLConstants.ML_ATTR_TYPE, false)) {
                    itemType = MSG_TYPE_SYS_RECALL;
                } else {
                    itemType = message.direct() == EMMessage.Direct.SEND ? MSG_TYPE_TXT_SEND : MSG_TYPE_TXT_RECEIVED;
                }
                break;
            default:
                // 默认返回txt类型
                itemType = message.direct() == EMMessage.Direct.SEND ? MSG_TYPE_TXT_SEND : MSG_TYPE_TXT_RECEIVED;
                break;
        }
        return itemType;
    }

    /**
     * 获取 item 的布局，根据传入的消息类型不同，返回不同的布局
     *
     * @param message 要展示的消息
     * @return 要显示的布局
     */
    private View createItemView(EMMessage message) {
        View itemView = null;
        switch (message.getType()) {
            case TXT:
                if (message.getBooleanAttribute(MLConstants.ML_ATTR_TYPE, false)) {
                    itemView = mInflater.inflate(R.layout.item_msg_recall, null);
                } else {
                    itemView = message.direct() == EMMessage.Direct.SEND
                            ? mInflater.inflate(R.layout.item_msg_text_send, null)
                            : mInflater.inflate(R.layout.item_msg_text_received, null);
                }
                break;
            default:
                itemView = message.direct() == EMMessage.Direct.SEND
                        ? mInflater.inflate(R.layout.item_msg_text_send, null)
                        : mInflater.inflate(R.layout.item_msg_text_received, null);
                break;
        }
        return itemView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前要显示的 message 对象
        EMMessage message = (EMMessage) getItem(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = createItemView(message);
            viewHolder = new ViewHolder();
            if (message.getType() == EMMessage.Type.TXT) {
                if (message.getBooleanAttribute(MLConstants.ML_ATTR_TYPE, false)) {
                    viewHolder.timeView = (TextView) convertView.findViewById(R.id.ml_text_msg_time);
                    viewHolder.contentView = (TextView) convertView.findViewById(R.id.ml_text_msg_content);
                } else {
                    viewHolder.avatarView = (MLImageView) convertView.findViewById(R.id.ml_img_msg_avatar);
                    viewHolder.contentView = (TextView) convertView.findViewById(R.id.ml_text_msg_content);
                    viewHolder.usernameView = (TextView) convertView.findViewById(R.id.ml_text_msg_username);
                    viewHolder.timeView = (TextView) convertView.findViewById(R.id.ml_text_msg_time);
                    viewHolder.msgState = (ImageView) convertView.findViewById(R.id.ml_img_msg_state);
                }
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        if (message.getBooleanAttribute(MLConstants.ML_ATTR_TYPE, false)) {
            String messageStr = ((EMTextMessageBody) message.getBody()).getMessage().toString();
            viewHolder.contentView.setText(messageStr);
            viewHolder.timeView.setText(MLDate.long2Time(message.getMsgTime()));
        } else {
            //            viewHolder.avatarView.setImageBitmap();
            viewHolder.usernameView.setText(message.getFrom());
            viewHolder.timeView.setText(MLDate.long2Time(message.getMsgTime()));
            switch (message.getType()) {
                case TXT:
                    handleTextMessage(message, viewHolder);
                    break;
                case IMAGE:
                    break;
                case FILE:
                    break;
                case LOCATION:
                    break;
                case VIDEO:
                    break;
                case VOICE:
                    break;
                default:
                    break;
            }

            // 判断消息的状态，如果发送失败就显示重发按钮，并设置重发按钮的监听
            if (message.status() == EMMessage.Status.FAIL) {
                viewHolder.msgState.setVisibility(View.VISIBLE);
                viewHolder.msgState.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 重新发送  在这里实现重发逻辑
                    }
                });
            } else {
                viewHolder.msgState.setVisibility(View.GONE);
            }
        }
        return convertView;
    }


    /**
     * 处理文字类消息
     *
     * @param message    要展示的消息对象
     * @param viewHolder 展示消息内容的 ViewHolder
     */
    private void handleTextMessage(EMMessage message, ViewHolder viewHolder) {
        EMTextMessageBody body = (EMTextMessageBody) message.getBody();
        String messageStr = body.getMessage().toString();
        // 判断是不是阅后即焚的消息
        if (message.getBooleanAttribute(MLConstants.ML_ATTR_BURN, false)) {
            viewHolder.contentView.setText(String.format("【内容长度%d】点击阅读", messageStr.length()));
        } else {
            viewHolder.contentView.setText(messageStr);
        }
    }


    /**
     * 供界面调用的刷新 Adapter 的方法
     */
    public void refreshList() {
        Message msg = mHandler.obtainMessage();
        msg.what = HANDLER_MSG_REFRESH;
        mHandler.sendMessage(msg);
    }

    public void refreshList(int position) {
        Message msg = mHandler.obtainMessage();
        msg.what = HANDLER_MSG_REFRESH_MORE;
        msg.arg1 = position;
        mHandler.sendMessage(msg);
    }

    /**
     * 自定义Handler，用来处理消息的刷新等
     */
    private class MLHandler extends Handler {

        /**
         * 刷新聊天信息列表，并滚动到底部
         */
        private void refresh() {
            messages.clear();
            messages = mConversation.getAllMessages();
            notifyDataSetChanged();
            // 平滑滚动到最后一条
            mListView.smoothScrollToPosition(messages.size() - 1);
        }

        /**
         * 刷新界面并平滑滚动到新加载的记录位置
         *
         * @param position 新加载的内容的最后一个位置
         */
        private void refresh(int position) {
            messages.clear();
            messages = mConversation.getAllMessages();
            notifyDataSetChanged();
            /**
             * 平滑滚动到最后一条，这里使用了ListView的两个方法:setSelection()/smoothScrollToPosition();
             * 如果单独使用setSelection()就会直接跳转到指定位置，没有平滑的效果
             * 如果单独使用smoothScrollToPosition() 就会因为我们的item高度不同导致滚动有偏差
             * 所以我们要先使用setSelection()跳转到指定位置，然后再用smoothScrollToPosition()平滑滚动到上一个
             */
            mListView.setSelection(position);
            mListView.smoothScrollToPosition(position - 1);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_MSG_REFRESH:
                    refresh();
                    break;
                case HANDLER_MSG_REFRESH_MORE:
                    refresh(msg.arg1);
                    break;
            }
        }
    }

    /**
     * 非静态内部类会隐式持有外部类的引用，就像大家经常将自定义的adapter在Activity类里，
     * 然后在adapter类里面是可以随意调用外部activity的方法的。当你将内部类定义为static时，
     * 你就调用不了外部类的实例方法了，因为这时候静态内部类是不持有外部类的引用的。
     * 声明ViewHolder静态内部类，可以将ViewHolder和外部类解引用。
     * 大家会说一般ViewHolder都很简单，不定义为static也没事吧。
     * 确实如此，但是如果你将它定义为static的，说明你懂这些含义。
     * 万一有一天你在这个ViewHolder加入一些复杂逻辑，做了一些耗时工作，
     * 那么如果ViewHolder是非静态内部类的话，就很容易出现内存泄露。如果是静态的话，
     * 你就不能直接引用外部类，迫使你关注如何避免相互引用。 所以将 ViewHolder 定义为静态的
     */
    static class ViewHolder {
        MLImageView avatarView;
        TextView usernameView;
        TextView contentView;
        TextView timeView;
        ImageView msgState;

    }
}