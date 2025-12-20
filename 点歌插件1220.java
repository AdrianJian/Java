import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import android.widget.CompoundButton;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.*;
import java.net.URLEncoder;
import java.io.*;
import android.text.TextUtils;
import me.hd.wauxv.data.bean.info.FriendInfo;
import me.hd.wauxv.data.bean.info.GroupInfo;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import java.lang.reflect.Method;

// ==================== 配置键定义 ====================
String SELF_ENABLED_KEY = "music_self_enabled";
String OTHERS_ENABLED_KEY = "music_others_enabled";
String CONTACT_WHITELIST_KEY = "contact_whitelist";
String GROUP_WHITELIST_KEY = "group_whitelist";

String DEFAULT_SOURCE_KEY = "default_source";       // "qq", "douyin", "kuwo"
String COVER_TYPE_KEY = "cover_type";               // "default", "avatar"
String SINGER_TYPE_KEY = "singer_type";             // "default", "nickname", "custom"
String CUSTOM_SINGER_KEY = "custom_singer";

String MUSIC_APP_ID = "wx485a97c844086dc9";

OkHttpClient okHttpClient = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build();

// ==================== 主题颜色 ====================
String C_BG_ROOT, C_TEXT_PRIMARY, C_TEXT_SECONDARY, C_CARD_BG, C_CARD_STROKE;
String C_EDIT_BG, C_EDIT_STROKE, C_DIVIDER, C_ACCENT_START, C_ACCENT_END;
String C_BUTTON_TEXT, C_HINT_TEXT;

boolean isDarkMode() {
    try {
        Activity a = getTopActivity();
        if (a == null) return false;
        int m = a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    } catch (Throwable e) {
        return false;
    }
}

void applyTheme() {
    boolean dark = isDarkMode();
    if (dark) {
        C_BG_ROOT = "#121212";
        C_TEXT_PRIMARY = "#E8E0FF";
        C_TEXT_SECONDARY = "#B0A8D8";
        C_CARD_BG = "#1E1B2B";
        C_CARD_STROKE = "#3A3380";
        C_EDIT_BG = "#252236";
        C_EDIT_STROKE = "#4A4280";
        C_DIVIDER = "#2A2740";
        C_ACCENT_START = "#7B68EE";
        C_ACCENT_END = "#BA68C8";
        C_BUTTON_TEXT = "#FFFFFF";
        C_HINT_TEXT = "#8888AA";
    } else {
        C_BG_ROOT = "#FAF8FF";
        C_TEXT_PRIMARY = "#4B3A80";
        C_TEXT_SECONDARY = "#6E5A9A";
        C_CARD_BG = "#FFFFFF";
        C_CARD_STROKE = "#D8D0FF";
        C_EDIT_BG = "#FFFFFF";
        C_EDIT_STROKE = "#C8B8FF";
        C_DIVIDER = "#E8E0FF";
        C_ACCENT_START = "#B0A0FF";
        C_ACCENT_END = "#E0C8FF";
        C_BUTTON_TEXT = "#4B3A80";
        C_HINT_TEXT = "#9999BB";
    }
}

// ==================== UI 工具方法 ====================
GradientDrawable shape(String color, int radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(Color.parseColor(color));
    g.setCornerRadius(radius);
    return g;
}

GradientDrawable shapeStroke(String fill, int radius, String stroke) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(Color.parseColor(fill));
    g.setCornerRadius(radius);
    g.setStroke(dp(2), Color.parseColor(stroke));
    return g;
}

GradientDrawable gradientBtn() {
    int[] colors = {Color.parseColor(C_ACCENT_START), Color.parseColor(C_ACCENT_END)};
    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    gd.setCornerRadius(dp(60));
    return gd;
}

Button cuteBtn(Activity act, String text) {
    Button b = new Button(act);
    b.setText(text);
    b.setAllCaps(false);
    b.setPadding(dp(36), dp(18), dp(36), dp(18));
    b.setTextColor(Color.parseColor(C_BUTTON_TEXT));
    b.setBackground(gradientBtn());
    b.setTextSize(16);
    return b;
}

void styleTextPrimary(TextView tv) {
    tv.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
}

void styleTextSecondary(TextView tv) {
    tv.setTextColor(Color.parseColor(C_TEXT_SECONDARY));
}

void styleEdit(EditText et) {
    et.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
    et.setHintTextColor(Color.parseColor(C_HINT_TEXT));
    et.setBackground(shapeStroke(C_EDIT_BG, dp(16), C_EDIT_STROKE));
    et.setPadding(dp(28), dp(18), dp(28), dp(18));
}

int dp(int v) {
    try {
        Activity a = getTopActivity();
        float d = a.getResources().getDisplayMetrics().density;
        return (int)(v * d + 0.5f);
    } catch (Throwable e) { return v; }
}

void uiToast(String s) { try { toast(s); } catch (Throwable ignore) {} }

// ==================== 权限判断（修复版）===================
boolean isAllowed(String talker, Object msgInfo) {
    String selfWxid = getLoginWxid();
    log("[点歌插件] 判断权限: talker=" + talker + ", selfWxid=" + selfWxid);

    boolean isGroup = talker.endsWith("@chatroom");

    boolean selfEnabled = getBoolean(SELF_ENABLED_KEY, true);
    boolean othersEnabled = getBoolean(OTHERS_ENABLED_KEY, true);

    Set<String> whitelist = new HashSet<>(getStringSet(isGroup ? GROUP_WHITELIST_KEY : CONTACT_WHITELIST_KEY, new HashSet<String>()));
    log("[点歌插件] 白名单大小: " + whitelist.size());

    // 关键修复：白名单为空时默认允许所有聊天
    if (whitelist.isEmpty()) {
        log("[点歌插件] 白名单为空，默认允许当前聊天");
        return true;
    }

    boolean inWhitelist = whitelist.contains(talker);
    log("[点歌插件] 白名单检查: inWhitelist=" + inWhitelist);

    if (!inWhitelist) return false;

    boolean isSentBySelf = false;
    try {
        isSentBySelf = msgInfo.isSend();
    } catch (Throwable e) {
        log("[点歌插件] isSend() 异常");
    }

    log("[点歌插件] 消息来源: " + (isSentBySelf ? "自己" : "他人"));

    return isSentBySelf ? selfEnabled : othersEnabled;
}

// ==================== 工具方法 ====================
String getSourceName(String key) {
    if ("qq".equals(key)) return "QQ音乐";
    if ("douyin".equals(key)) return "抖音音乐";
    if ("kuwo".equals(key)) return "酷我音乐";
    return "未知";
}

String getCoverName(String key) {
    if ("default".equals(key)) return "平台默认封面";
    if ("avatar".equals(key)) return "发送者头像封面";
    return "未知";
}

String getSingerName(String key) {
    if ("default".equals(key)) return "平台默认歌手";
    if ("nickname".equals(key)) return "发送者微信昵称";
    if ("custom".equals(key)) return "自定义歌手（" + getString(CUSTOM_SINGER_KEY, "无") + "）";
    return "未知";
}

// ==================== 主配置界面 ====================
void showMainConfigDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) {
        log("[点歌插件] 主界面 getTopActivity null");
        return;
    }

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("点歌插件配置");
                title.setTextSize(26);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                TextView author = new TextView(act);
                author.setText("作者 – JP");
                author.setTextSize(16);
                styleTextSecondary(author);
                author.setGravity(Gravity.CENTER);
                author.setPadding(0, dp(8), 0, dp(20));
                root.addView(author);

                TextView poetry = new TextView(act);
                poetry.setText("花外鸟归残雨暮，竹边人语夕阳闲");
                poetry.setTextSize(18);
                poetry.setTypeface(Typeface.create("serif", Typeface.ITALIC | Typeface.BOLD));
                styleTextSecondary(poetry);
                poetry.setGravity(Gravity.CENTER);
                poetry.setPadding(0, 0, 0, dp(32));
                root.addView(poetry);

                // 权限开关卡片
                LinearLayout switchCard = new LinearLayout(act);
                switchCard.setOrientation(LinearLayout.VERTICAL);
                switchCard.setBackground(shapeStroke(C_CARD_BG, dp(24), C_CARD_STROKE));
                switchCard.setPadding(dp(28), dp(24), dp(28), dp(24));

                TextView switchTitle = new TextView(act);
                switchTitle.setText("功能使用权限");
                switchTitle.setTextSize(17);
                styleTextPrimary(switchTitle);
                switchCard.addView(switchTitle);

                LinearLayout switchRow = new LinearLayout(act);
                switchRow.setOrientation(LinearLayout.HORIZONTAL);
                switchRow.setGravity(Gravity.CENTER_VERTICAL);
                switchRow.setWeightSum(2.0f);

                Switch selfSwitch = new Switch(act);
                selfSwitch.setText(" 自己可用");
                selfSwitch.setChecked(getBoolean(SELF_ENABLED_KEY, true));
                styleTextPrimary(selfSwitch);
                switchRow.addView(selfSwitch, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Switch othersSwitch = new Switch(act);
                othersSwitch.setText(" 他人可用");
                othersSwitch.setChecked(getBoolean(OTHERS_ENABLED_KEY, true));
                styleTextPrimary(othersSwitch);
                switchRow.addView(othersSwitch, new LinearLayout.LayoutParams(0, -2, 1.0f));

                switchCard.addView(switchRow);
                root.addView(switchCard);
                ((LinearLayout.LayoutParams)switchCard.getLayoutParams()).bottomMargin = dp(32);

                selfSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton b, boolean c) {
                        putBoolean(SELF_ENABLED_KEY, c);
                        uiToast(c ? "自己可用已开启" : "自己可用已关闭");
                    }
                });

                othersSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton b, boolean c) {
                        putBoolean(OTHERS_ENABLED_KEY, c);
                        uiToast(c ? "他人可用已开启" : "他人可用已关闭");
                    }
                });

                // 当前平台显示
                TextView currentSourceTv = new TextView(act);
                currentSourceTv.setText("当前点歌平台： " + getSourceName(getString(DEFAULT_SOURCE_KEY, "qq")));
                currentSourceTv.setTextSize(16);
                styleTextSecondary(currentSourceTv);
                currentSourceTv.setGravity(Gravity.CENTER);
                currentSourceTv.setPadding(0, 0, 0, dp(32));
                root.addView(currentSourceTv);

                // 按钮区
                Button btnAdvanced = cuteBtn(act, "高级设置（平台·封面·歌手）");
                root.addView(btnAdvanced);
                ((LinearLayout.LayoutParams)btnAdvanced.getLayoutParams()).bottomMargin = dp(16);

                Button btnContactWl = cuteBtn(act, "个人白名单设置");
                root.addView(btnContactWl);
                ((LinearLayout.LayoutParams)btnContactWl.getLayoutParams()).bottomMargin = dp(16);

                Button btnGroupWl = cuteBtn(act, "群聊白名单设置");
                root.addView(btnGroupWl);
                ((LinearLayout.LayoutParams)btnGroupWl.getLayoutParams()).bottomMargin = dp(32);

                Button btnReadme = cuteBtn(act, "插件说明");
                root.addView(btnReadme);
                ((LinearLayout.LayoutParams)btnReadme.getLayoutParams()).bottomMargin = dp(16);

                Button btnClose = cuteBtn(act, "关闭");
                root.addView(btnClose);

                btnAdvanced.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                btnContactWl.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showWhitelistCustomUI(true);
                    }
                });

                btnGroupWl.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showWhitelistCustomUI(false);
                    }
                });

                btnReadme.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showReadmeDialog();
                    }
                });

                btnClose.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.94f), act.getResources().getDisplayMetrics().widthPixels - dp(32));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 主配置界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 主界面异常: " + e.toString());
                uiToast("打开配置失败");
            }
        }
    });
}

// ==================== 高级设置 ====================
void showAdvancedSettingsDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("高级设置");
                title.setTextSize(22);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);
                ((LinearLayout.LayoutParams)title.getLayoutParams()).bottomMargin = dp(32);

                Button btnSource = cuteBtn(act, "选择点歌平台");
                root.addView(btnSource);
                ((LinearLayout.LayoutParams)btnSource.getLayoutParams()).bottomMargin = dp(20);

                Button btnCover = cuteBtn(act, "选择封面类型");
                root.addView(btnCover);
                ((LinearLayout.LayoutParams)btnCover.getLayoutParams()).bottomMargin = dp(20);

                Button btnSinger = cuteBtn(act, "选择歌手显示");
                root.addView(btnSinger);
                ((LinearLayout.LayoutParams)btnSinger.getLayoutParams()).bottomMargin = dp(40);

                Button btnBack = cuteBtn(act, "返回主菜单");
                root.addView(btnBack);

                btnSource.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showSourceSelectDialog();
                    }
                });

                btnCover.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showCoverSelectDialog();
                    }
                });

                btnSinger.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showSingerSelectDialog();
                    }
                });

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showMainConfigDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 高级设置界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 高级设置异常: " + e.toString());
            }
        }
    });
}

// ==================== 音源选择 ====================
void showSourceSelectDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("选择点歌平台");
                title.setTextSize(22);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                TextView currentTv = new TextView(act);
                currentTv.setText("当前： " + getSourceName(getString(DEFAULT_SOURCE_KEY, "qq")));
                currentTv.setTextSize(16);
                styleTextSecondary(currentTv);
                currentTv.setGravity(Gravity.CENTER);
                currentTv.setPadding(0, dp(20), 0, dp(20));
                root.addView(currentTv);

                final CheckBox cbQq = new CheckBox(act);
                cbQq.setText("QQ音乐");
                cbQq.setTextSize(18);
                styleTextPrimary(cbQq);

                final CheckBox cbDouyin = new CheckBox(act);
                cbDouyin.setText("抖音音乐");
                cbDouyin.setTextSize(18);
                styleTextPrimary(cbDouyin);

                final CheckBox cbKuwo = new CheckBox(act);
                cbKuwo.setText("酷我音乐");
                cbKuwo.setTextSize(18);
                styleTextPrimary(cbKuwo);

                String current = getString(DEFAULT_SOURCE_KEY, "qq");
                cbQq.setChecked("qq".equals(current));
                cbDouyin.setChecked("douyin".equals(current));
                cbKuwo.setChecked("kuwo".equals(current));

                final CheckBox[] cbs = {cbQq, cbDouyin, cbKuwo};

                View.OnClickListener clickListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox clicked = (CheckBox) v;
                        for (CheckBox cb : cbs) {
                            cb.setChecked(cb == clicked);
                        }
                    }
                };

                cbQq.setOnClickListener(clickListener);
                cbDouyin.setOnClickListener(clickListener);
                cbKuwo.setOnClickListener(clickListener);

                root.addView(cbQq);
                root.addView(cbDouyin);
                root.addView(cbKuwo);

                LinearLayout btns = new LinearLayout(act);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                btns.setWeightSum(2.0f);
                btns.setPadding(0, dp(40), 0, 0);

                Button btnSave = cuteBtn(act, "保存");
                btns.addView(btnSave, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnBack = cuteBtn(act, "返回");
                btns.addView(btnBack, new LinearLayout.LayoutParams(0, -2, 1.0f));

                root.addView(btns);

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String selected = cbDouyin.isChecked() ? "douyin" : cbKuwo.isChecked() ? "kuwo" : "qq";
                        putString(DEFAULT_SOURCE_KEY, selected);
                        uiToast("已切换为 " + getSourceName(selected));
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 音源选择界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 音源选择异常: " + e.toString());
            }
        }
    });
}

// ==================== 封面选择 ====================
void showCoverSelectDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("选择封面类型");
                title.setTextSize(22);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                TextView tip = new TextView(act);
                tip.setText("注：头像封面需调用异步接口，实际使用时以平台封面为主");
                styleTextSecondary(tip);
                tip.setGravity(Gravity.CENTER);
                tip.setPadding(0, dp(12), 0, dp(20));
                root.addView(tip);

                TextView currentTv = new TextView(act);
                currentTv.setText("当前： " + getCoverName(getString(COVER_TYPE_KEY, "default")));
                currentTv.setTextSize(16);
                styleTextSecondary(currentTv);
                currentTv.setGravity(Gravity.CENTER);
                currentTv.setPadding(0, 0, 0, dp(20));
                root.addView(currentTv);

                final CheckBox cbDefault = new CheckBox(act);
                cbDefault.setText("平台默认封面");
                cbDefault.setTextSize(18);
                styleTextPrimary(cbDefault);

                final CheckBox cbAvatar = new CheckBox(act);
                cbAvatar.setText("发送者头像封面");
                cbAvatar.setTextSize(18);
                styleTextPrimary(cbAvatar);

                String current = getString(COVER_TYPE_KEY, "default");
                cbDefault.setChecked("default".equals(current));
                cbAvatar.setChecked("avatar".equals(current));

                final CheckBox[] cbs = {cbDefault, cbAvatar};

                View.OnClickListener clickListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox clicked = (CheckBox) v;
                        for (CheckBox cb : cbs) {
                            cb.setChecked(cb == clicked);
                        }
                    }
                };

                cbDefault.setOnClickListener(clickListener);
                cbAvatar.setOnClickListener(clickListener);

                root.addView(cbDefault);
                root.addView(cbAvatar);

                LinearLayout btns = new LinearLayout(act);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                btns.setWeightSum(2.0f);
                btns.setPadding(0, dp(40), 0, 0);

                Button btnSave = cuteBtn(act, "保存");
                btns.addView(btnSave, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnBack = cuteBtn(act, "返回");
                btns.addView(btnBack, new LinearLayout.LayoutParams(0, -2, 1.0f));

                root.addView(btns);

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String selected = cbAvatar.isChecked() ? "avatar" : "default";
                        putString(COVER_TYPE_KEY, selected);
                        uiToast("封面类型已保存");
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 封面选择界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 封面选择异常: " + e.toString());
            }
        }
    });
}

// ==================== 歌手选择 ====================
void showSingerSelectDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("选择歌手显示");
                title.setTextSize(22);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                TextView currentTv = new TextView(act);
                currentTv.setText("当前： " + getSingerName(getString(SINGER_TYPE_KEY, "default")));
                currentTv.setTextSize(16);
                styleTextSecondary(currentTv);
                currentTv.setGravity(Gravity.CENTER);
                currentTv.setPadding(0, dp(20), 0, dp(20));
                root.addView(currentTv);

                final CheckBox cbDefault = new CheckBox(act);
                cbDefault.setText("平台默认歌手");
                cbDefault.setTextSize(18);
                styleTextPrimary(cbDefault);

                final CheckBox cbNickname = new CheckBox(act);
                cbNickname.setText("发送者微信昵称");
                cbNickname.setTextSize(18);
                styleTextPrimary(cbNickname);

                final CheckBox cbCustom = new CheckBox(act);
                cbCustom.setText("自定义歌手");
                cbCustom.setTextSize(18);
                styleTextPrimary(cbCustom);

                String current = getString(SINGER_TYPE_KEY, "default");
                cbDefault.setChecked("default".equals(current));
                cbNickname.setChecked("nickname".equals(current));
                cbCustom.setChecked("custom".equals(current));

                final CheckBox[] cbs = {cbDefault, cbNickname, cbCustom};

                View.OnClickListener clickListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox clicked = (CheckBox) v;
                        for (CheckBox cb : cbs) {
                            cb.setChecked(cb == clicked);
                        }
                    }
                };

                cbDefault.setOnClickListener(clickListener);
                cbNickname.setOnClickListener(clickListener);
                cbCustom.setOnClickListener(clickListener);

                root.addView(cbDefault);
                root.addView(cbNickname);
                root.addView(cbCustom);

                LinearLayout btns = new LinearLayout(act);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                btns.setWeightSum(2.0f);
                btns.setPadding(0, dp(40), 0, 0);

                Button btnSave = cuteBtn(act, "保存");
                btns.addView(btnSave, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnBack = cuteBtn(act, "返回");
                btns.addView(btnBack, new LinearLayout.LayoutParams(0, -2, 1.0f));

                root.addView(btns);

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String selected = cbNickname.isChecked() ? "nickname" : cbCustom.isChecked() ? "custom" : "default";
                        putString(SINGER_TYPE_KEY, selected);

                        if ("custom".equals(selected)) {
                            d.dismiss();
                            showCustomSingerInputDialog();
                            return;
                        }

                        uiToast("歌手显示已保存");
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 歌手选择界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 歌手选择异常: " + e.toString());
            }
        }
    });
}

void showCustomSingerInputDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText("自定义歌手名");
                title.setTextSize(20);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                final EditText et = new EditText(act);
                et.setHint("输入自定义歌手名");
                et.setText(getString(CUSTOM_SINGER_KEY, ""));
                styleEdit(et);
                root.addView(et);
                ((LinearLayout.LayoutParams)et.getLayoutParams()).topMargin = dp(20);

                LinearLayout btns = new LinearLayout(act);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                btns.setWeightSum(2.0f);

                Button btnSave = cuteBtn(act, "保存");
                btns.addView(btnSave, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnBack = cuteBtn(act, "返回");
                btns.addView(btnBack, new LinearLayout.LayoutParams(0, -2, 1.0f));

                root.addView(btns);
                ((LinearLayout.LayoutParams)btns.getLayoutParams()).topMargin = dp(32);

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String text = et.getText().toString().trim();
                        putString(CUSTOM_SINGER_KEY, text);
                        uiToast("自定义歌手已保存");
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showAdvancedSettingsDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 自定义歌手输入界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 自定义歌手异常: " + e.toString());
            }
        }
    });
}

// ==================== 白名单界面（遮罩改为白色透明）===================
void showWhitelistCustomUI(final boolean isContact) {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) {
        log("[点歌插件] 白名单 getTopActivity null");
        return;
    }

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(24), dp(32), dp(24), dp(24));
                root.setBackground(shape(C_BG_ROOT, dp(32)));

                TextView title = new TextView(act);
                title.setText(isContact ? "个人白名单" : "群聊白名单");
                title.setTextSize(22);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                final EditText searchEdit = new EditText(act);
                searchEdit.setHint("搜索名称或wxid...");
                styleEdit(searchEdit);
                searchEdit.setSingleLine(true);
                LinearLayout.LayoutParams lpSearch = new LinearLayout.LayoutParams(-1, -2);
                lpSearch.topMargin = dp(20);
                root.addView(searchEdit, lpSearch);

                Set<String> loaded = getStringSet(isContact ? CONTACT_WHITELIST_KEY : GROUP_WHITELIST_KEY, new HashSet<String>());
                final Set<String> selected = new HashSet<String>(loaded);

                final List<String> allIds = new ArrayList<String>();
                final List<String> allNames = new ArrayList<String>();

                final ScrollView scroll = new ScrollView(act);
                final LinearLayout container = new LinearLayout(act);
                container.setOrientation(LinearLayout.VERTICAL);
                scroll.addView(container);

                // 遮罩层改为白色半透明
                final FrameLayout overlay = new FrameLayout(act);
                overlay.setBackgroundColor(Color.parseColor("#AAFFFFFF"));
                overlay.setVisibility(View.VISIBLE);

                ProgressBar pb = new ProgressBar(act, null, android.R.attr.progressBarStyleLarge);
                pb.setIndeterminate(true);

                TextView loadingTv = new TextView(act);
                loadingTv.setText("正在加载...");
                loadingTv.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
                loadingTv.setTextSize(18);

                FrameLayout.LayoutParams pbLp = new FrameLayout.LayoutParams(-2, -2);
                pbLp.gravity = Gravity.CENTER;
                overlay.addView(pb, pbLp);

                FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(-2, -2);
                tvLp.gravity = Gravity.CENTER;
                tvLp.topMargin = dp(100);
                overlay.addView(loadingTv, tvLp);

                FrameLayout contentWrapper = new FrameLayout(act);
                contentWrapper.addView(scroll);
                contentWrapper.addView(overlay);

                root.addView(contentWrapper, new LinearLayout.LayoutParams(-1, 0, 1.0f));

                final Runnable refreshList = new Runnable() {
                    public void run() {
                        String query = searchEdit.getText().toString().toLowerCase(Locale.getDefault());
                        container.removeAllViews();

                        for (int i = 0; i < allIds.size(); i++) {
                            String name = allNames.get(i);
                            if (!query.isEmpty() && !name.toLowerCase(Locale.getDefault()).contains(query)) continue;

                            String wxid = allIds.get(i);
                            boolean checked = selected.contains(wxid);

                            LinearLayout item = new LinearLayout(act);
                            item.setOrientation(LinearLayout.HORIZONTAL);
                            item.setPadding(dp(20), dp(16), dp(20), dp(16));
                            item.setGravity(Gravity.CENTER_VERTICAL);

                            CheckBox cb = new CheckBox(act);
                            cb.setChecked(checked);
                            cb.setTag(wxid);
                            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                                    String id = (String) button.getTag();
                                    if (isChecked) selected.add(id);
                                    else selected.remove(id);
                                }
                            });

                            item.addView(cb);

                            TextView tv = new TextView(act);
                            tv.setText(name);
                            styleTextSecondary(tv);
                            tv.setPadding(dp(16), 0, 0, 0);
                            item.addView(tv, new LinearLayout.LayoutParams(0, -2, 1.0f));

                            container.addView(item);
                        }
                    }
                };

                searchEdit.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable s) { refreshList.run(); }
                });

                // 加载数据线程
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            log("[点歌插件] 开始加载" + (isContact ? "联系人" : "群聊") + "白名单数据");
                            if (isContact) {
                                List<FriendInfo> list = getFriendList();
                                if (list != null) {
                                    for (FriendInfo f : list) {
                                        String wxid = f.getWxid();
                                        if (wxid == null || wxid.isEmpty() || wxid.endsWith("@chatroom")) continue;
                                        String nick = f.getNickname();
                                        String name = (nick != null && !nick.isEmpty()) ? nick : "未知";
                                        allIds.add(wxid);
                                        allNames.add(name + " (" + wxid + ")");
                                    }
                                }
                            } else {
                                List<GroupInfo> list = getGroupList();
                                if (list != null) {
                                    for (GroupInfo g : list) {
                                        String roomId = g.getRoomId();
                                        if (roomId == null || !roomId.endsWith("@chatroom")) continue;
                                        String name = g.getName();
                                        if (name == null || name.isEmpty()) name = "未知群";
                                        int count = 0;
                                        try { count = getGroupMemberCount(roomId); } catch (Throwable ignored) {}
                                        allIds.add(roomId);
                                        allNames.add(name + " (" + count + "人) (" + roomId + ")");
                                    }
                                }
                            }

                            // 排序
                            List<Integer> indices = new ArrayList<Integer>();
                            for (int i = 0; i < allIds.size(); i++) indices.add(i);
                            Collections.sort(indices, new Comparator<Integer>() {
                                public int compare(Integer a, Integer b) {
                                    return allNames.get(a).compareTo(allNames.get(b));
                                }
                            });

                            List<String> sortedIds = new ArrayList<String>();
                            List<String> sortedNames = new ArrayList<String>();
                            for (int idx : indices) {
                                sortedIds.add(allIds.get(idx));
                                sortedNames.add(allNames.get(idx));
                            }

                            allIds.clear(); allIds.addAll(sortedIds);
                            allNames.clear(); allNames.addAll(sortedNames);

                            log("[点歌插件] 白名单数据加载并排序完成，条目数: " + allIds.size());

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                public void run() {
                                    overlay.setVisibility(View.GONE);
                                    refreshList.run();
                                }
                            });
                        } catch (Throwable e) {
                            log("[点歌插件] 白名单加载异常: " + e.toString());
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                public void run() {
                                    overlay.setVisibility(View.GONE);
                                    uiToast("加载失败");
                                }
                            });
                        }
                    }
                }).start();

                LinearLayout btns = new LinearLayout(act);
                btns.setOrientation(LinearLayout.HORIZONTAL);
                btns.setWeightSum(3.0f);
                btns.setPadding(0, dp(20), 0, 0);

                final Button btnAll = cuteBtn(act, "全选");
                btns.addView(btnAll, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnNone = cuteBtn(act, "全不选");
                btns.addView(btnNone, new LinearLayout.LayoutParams(0, -2, 1.0f));

                Button btnSave = cuteBtn(act, "保存");
                btns.addView(btnSave, new LinearLayout.LayoutParams(0, -2, 1.0f));

                root.addView(btns);

                btnAll.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        overlay.setVisibility(View.VISIBLE);
                        loadingTv.setText("正在处理全选...");
                        new Thread(new Runnable() {
                            public void run() {
                                if ("全选".equals(btnAll.getText().toString())) {
                                    selected.addAll(allIds);
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        public void run() {
                                            btnAll.setText("取消全选");
                                            overlay.setVisibility(View.GONE);
                                            refreshList.run();
                                        }
                                    });
                                } else {
                                    selected.clear();
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        public void run() {
                                            btnAll.setText("全选");
                                            overlay.setVisibility(View.GONE);
                                            refreshList.run();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });

                btnNone.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        overlay.setVisibility(View.VISIBLE);
                        loadingTv.setText("正在处理全不选...");
                        new Thread(new Runnable() {
                            public void run() {
                                selected.clear();
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    public void run() {
                                        btnAll.setText("全选");
                                        overlay.setVisibility(View.GONE);
                                        refreshList.run();
                                    }
                                });
                            }
                        }).start();
                    }
                });

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        overlay.setVisibility(View.VISIBLE);
                        loadingTv.setText("正在保存...");
                        new Thread(new Runnable() {
                            public void run() {
                                putStringSet(isContact ? CONTACT_WHITELIST_KEY : GROUP_WHITELIST_KEY, new HashSet<String>(selected));
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    public void run() {
                                        overlay.setVisibility(View.GONE);
                                        uiToast("白名单已保存");
                                        d.dismiss();
                                        showMainConfigDialog();
                                    }
                                });
                            }
                        }).start();
                    }
                });

                Button btnBack = cuteBtn(act, "返回");
                root.addView(btnBack);
                ((LinearLayout.LayoutParams)btnBack.getLayoutParams()).topMargin = dp(16);

                btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showMainConfigDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.94f), act.getResources().getDisplayMetrics().widthPixels - dp(32));
                    lp.height = act.getResources().getDisplayMetrics().heightPixels * 4 / 5;
                    w.setAttributes(lp);
                }
                d.show();
                log("[点歌插件] 白名单界面已打开");
            } catch (Throwable e) {
                log("[点歌插件] 白名单界面异常: " + e.toString());
            }
        }
    });
}

// ==================== 说明 ====================
void showReadmeDialog() {
    applyTheme();
    Activity act = getTopActivity();
    if (act == null) return;

    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(32), dp(32), dp(32), dp(32));
                root.setBackground(shape(C_BG_ROOT, dp(28)));

                TextView title = new TextView(act);
                title.setText("插件说明");
                title.setTextSize(20);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleTextPrimary(title);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                ScrollView scroll = new ScrollView(act);
                TextView content = new TextView(act);
                content.setText(readFile("readme.md"));
                content.setLineSpacing(0, 1.5f);
                styleTextSecondary(content);
                content.setPadding(dp(12), dp(20), dp(12), dp(20));
                scroll.addView(content);

                root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

                Button btnClose = cuteBtn(act, "关闭");
                root.addView(btnClose);
                ((LinearLayout.LayoutParams)btnClose.getLayoutParams()).topMargin = dp(24);

                btnClose.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                        showMainConfigDialog();
                    }
                });

                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(root);
                Window w = d.getWindow();
                if (w != null) {
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.width = Math.min((int)(act.getResources().getDisplayMetrics().widthPixels * 0.92f), act.getResources().getDisplayMetrics().widthPixels - dp(48));
                    w.setAttributes(lp);
                }
                d.show();
            } catch (Throwable e) {
                uiToast("说明打开失败");
                log("[点歌插件] 说明界面异常: " + e.toString());
            }
        }
    });
}

String readFile(String filename) {
    try {
        File file = new File(pluginDir + "/" + filename);
        if (!file.exists()) return "文件不存在：" + filename;
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();
        return sb.toString().trim();
    } catch (Exception e) {
        return "读取失败：" + e.toString();
    }
}

// ==================== 反射工具 ====================
Object invoke(Object obj, String methodName) {
    try {
        Method m = obj.getClass().getMethod(methodName);
        return m.invoke(obj);
    } catch (Exception e) {
        return null;
    }
}

boolean getBool(Object obj, String methodName) {
    Object ret = invoke(obj, methodName);
    return ret != null && (Boolean) ret;
}

String getStr(Object obj, String methodName) {
    Object ret = invoke(obj, methodName);
    return ret != null ? ret.toString().trim() : "";
}

// ==================== 主消息处理 ====================
void onHandleMsg(Object msgInfoBean) {
    try {
        boolean isText = getBool(msgInfoBean, "isText");
        String talker = getStr(msgInfoBean, "getTalker");
        String content = getStr(msgInfoBean, "getContent").trim();

        if (!isText || content.isEmpty()) return;

        if (content.startsWith("点歌") && content.length() > 2) {
            String songName = content.substring(2).trim();

            if (songName.isEmpty()) {
                insertSystemMsg(talker, "歌名不能为空哦~ 示例：点歌 青花瓷");
                return;
            }

            if (!isAllowed(talker, msgInfoBean)) {
                insertSystemMsg(talker, "本聊天未开启点歌功能，可在“点歌配置”中添加白名单");
                return;
            }

            String source = getString(DEFAULT_SOURCE_KEY, "qq");
            searchAndSendMusic(talker, songName, source, msgInfoBean);

        }

    } catch (Exception e) {
        log("[点歌插件] onHandleMsg 异常: " + e.toString());
    }
}

// ==================== 搜索并发送音乐 ====================
void searchAndSendMusic(String talker, String songName, String apiType, Object msgInfo) {
    String url = "";
    if (apiType.equals("qq")) {
        url = "https://www.yx520.ltd/API/qqyy/api.php?a=1&msg=" + URLEncoder.encode(songName, "UTF-8");
    } else if (apiType.equals("douyin")) {
        url = "https://www.yx520.ltd/API/dyyy/api.php?msg=" + URLEncoder.encode(songName, "UTF-8");
    } else if (apiType.equals("kuwo")) {
        url = "https://www.yx520.ltd/API/kwyy/api.php?msg=" + URLEncoder.encode(songName, "UTF-8") + "&a=10&n=1";
    }

    log("[点歌插件] 请求歌曲: " + songName + " 平台: " + apiType + " URL: " + url);

    Request request = new Request.Builder().url(url).get().build();

    okHttpClient.newCall(request).enqueue(new Callback() {
        public void onFailure(Call call, IOException e) {
            insertSystemMsg(talker, "点歌失败：网络错误");
            log("[点歌插件] 网络请求失败: " + e.toString());
        }

        public void onResponse(Call call, Response response) throws IOException {
            if (!response.isSuccessful()) {
                insertSystemMsg(talker, "点歌服务暂时不可用");
                response.close();
                return;
            }

            String body = response.body().string();
            response.close();

            try {
                org.json.JSONObject json = new org.json.JSONObject(body);
                String title = null;
                String singer = null;
                String cover = "";
                String musicUrl = null;
                String lyric = "[99:99.99]暂无歌词";

                if (apiType.equals("qq") && json.getInt("code") == 200 && json.getJSONArray("data").length() > 0) {
                    org.json.JSONObject song = json.getJSONArray("data").getJSONObject(0);
                    title = song.getString("song");
                    singer = song.getString("singer");
                    cover = song.getString("cover");
                    musicUrl = song.getString("url");
                    lyric = song.optString("lyric", lyric);
                } else if (apiType.equals("douyin") && json.getString("code").equals("0") && json.getJSONArray("data").length() > 0) {
                    org.json.JSONObject song = json.getJSONArray("data").getJSONObject(0);
                    String full = song.getString("title");
                    int idx = full.lastIndexOf("---");
                    title = idx > 0 ? full.substring(0, idx).trim() : full;
                    singer = idx > 0 ? full.substring(idx + 3).trim() : "未知歌手";
                    cover = song.getString("imgurl");
                    musicUrl = song.getString("url");
                } else if (apiType.equals("kuwo") && json.getString("code").equals("200")) {
                    musicUrl = json.getString("url");
                    String nameList = json.getString("name");
                    String[] lines = nameList.split("\n");
                    if (lines.length > 0) {
                        String part = lines[0].substring(lines[0].indexOf(".") + 1).trim();
                        int dash = part.indexOf("---");
                        title = dash > 0 ? part.substring(0, dash).trim() : part;
                        singer = dash > 0 ? part.substring(dash + 3).trim() : "未知歌手";
                    }
                }

                if (title == null || musicUrl == null) {
                    insertSystemMsg(talker, "抱歉，没找到《" + songName + "》");
                    return;
                }

                // 歌手处理
                String singerType = getString(SINGER_TYPE_KEY, "default");
                if ("nickname".equals(singerType)) {
                    boolean isSelf = getBool(msgInfo, "isSend");
                    String senderWxid = isSelf ? getLoginWxid() : getStr(msgInfo, "getSendTalker");
                    String nick = getFriendName(senderWxid);
                    if (nick != null && !nick.isEmpty()) singer = nick;
                } else if ("custom".equals(singerType)) {
                    String custom = getString(CUSTOM_SINGER_KEY, "");
                    if (!custom.isEmpty()) singer = custom;
                }

                // 封面处理（头像仅请求，不替换）
                String coverType = getString(COVER_TYPE_KEY, "default");
                if ("avatar".equals(coverType)) {
                    boolean isSelf = getBool(msgInfo, "isSend");
                    String senderWxid = isSelf ? getLoginWxid() : getStr(msgInfo, "getSendTalker");
                    getAvatarUrl(senderWxid, true); // 异步请求大头像（日志可见URL）
                    log("[点歌插件] 已请求发送者大头像用于封面（实际卡片仍使用平台封面）");
                }

                WXMusicObject musicObj = new WXMusicObject();
                musicObj.musicUrl = musicUrl;
                musicObj.musicDataUrl = musicUrl;
                musicObj.songLyric = lyric;
                musicObj.songAlbumUrl = cover;

                WXMediaMessage msg = new WXMediaMessage(musicObj);
                msg.title = title;
                msg.description = singer;

                try {
                    sendMediaMsg(talker, msg, MUSIC_APP_ID);
                    log("[点歌插件] 成功发送音乐卡片: " + title + " - " + singer);
                } catch (Exception e) {
                    insertSystemMsg(talker, "歌曲找到但卡片发送失败，链接：" + musicUrl);
                    log("[点歌插件] sendMediaMsg 失败: " + e.toString());
                }

            } catch (Exception e) {
                insertSystemMsg(talker, "点歌出错，请重试");
                log("[点歌插件] 解析响应异常: " + e.toString());
            }
        }
    });
}

// ==================== 发送按钮拦截（打开配置） ====================
boolean onClickSendBtn(String text) {
    String t = text == null ? "" : text.trim();
    if ("点歌配置".equals(t)) {
        showMainConfigDialog();
        return true; // 拦截，不发送
    }
    return false;
}

void onLoad() {
    log("[点歌插件] 高颜值配置版加载成功 - 作者 JP（修复：默认允许 + 白色遮罩 + 详细日志）");
}

void onUnLoad() {
}