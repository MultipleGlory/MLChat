package net.melove.app.chat.module.listener;

/**
 * Created by lzan13 on 2016/11/10.
 *
 * 自定义回调接口，因为 RecyclerView 控件没有实现 Item 的点击以及长按监听，所以需要自己定义回调实现
 */
public interface MLItemCallBack {
    /**
     * Item 点击及长按事件的处理
     *
     * @param action 长按菜单需要处理的动作，
     * @param tag 需要操作的 Item 的 tag，这里定义为一个 Object，可以根据需要进行类型转换
     */
    public void onAction(int action, Object tag);
}
