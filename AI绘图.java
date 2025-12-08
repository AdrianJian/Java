import java.util.HashMap;
import java.io.File;
import java.net.URLEncoder;
import me.hd.wauxv.plugin.api.callback.PluginCallBack;
import me.hd.wauxv.data.bean.MsgInfoBean;
import org.json.JSONObject;

String PLUGIN_NAME = "Wa_AIDraw";
String PLUGIN_DIR = pluginDir + "/Wa_AIDraw/";
String LAST_SENT_TIME_KEY = "last_sent_time";
String AI_DRAW_SWITCH_KEY_PREFIX = "ai_draw_switch_";
String AI_DRAW_API = "https://api.siliconflow.cn/v1/images/generations";
String API_KEY = "sk-mosmbagzujowknozyikllwfyshycpzzsezsvkrenqtvugcrp";

void initCacheDir() {
    File dir = new File(PLUGIN_DIR);
    if (!dir.exists() && !dir.mkdirs()) {
        toast("无法创建缓存目录");
        return;
    }
    File[] files = dir.listFiles();
    if (files != null && files.length > 1) {
        for (int i = 1; i < files.length; i++) {
            files[i].delete();
        }
    }
}

boolean onLongClickSendBtn(String text) {
    try {
        String talker = getTargetTalker();
        if (text.equals("绘画开")) {
            putBoolean(AI_DRAW_SWITCH_KEY_PREFIX + talker, true);
            insertSystemMsg(talker, "AI绘画功能已开启", System.currentTimeMillis());
            return true;
        } else if (text.equals("绘画关")) {
            putBoolean(AI_DRAW_SWITCH_KEY_PREFIX + talker, false);
            insertSystemMsg(talker, "AI绘画功能已关闭", System.currentTimeMillis());
            return true;
        } else if (text.startsWith("画")) {
            boolean isEnabled = getBoolean(AI_DRAW_SWITCH_KEY_PREFIX + talker, false); // 初次使用默认关闭
            if (!isEnabled) {
                insertSystemMsg(talker, "AI绘画功能已关闭，请先开启", System.currentTimeMillis());
                return true;
            }
            long currentTime = System.currentTimeMillis();
            long lastSentTime = getLong(LAST_SENT_TIME_KEY, 0);
            if (currentTime - lastSentTime < 1000) {
                toast("操作太频繁，请稍后重试");
                return true;
            }
            handleDrawCommand(text, talker);
            putLong(LAST_SENT_TIME_KEY, currentTime);
            return true;
        }
        return false;
    } catch (Throwable t) {
        toast("操作失败：" + t.toString());
        return false;
    }
}

void onHandleMsg(Object msgInfoBean) {
    try {
        if (msgInfoBean == null) {
            return;
        }
        MsgInfoBean msg = (MsgInfoBean) msgInfoBean;
        String content = msg.getContent();
        boolean isText = msg.isText();
        String talker = msg.getTalker();
        if (isText && content != null && content.startsWith("画")) {
            boolean isEnabled = getBoolean(AI_DRAW_SWITCH_KEY_PREFIX + talker, false); // 初次使用默认关闭
            if (!isEnabled) {
                insertSystemMsg(talker, "AI绘画功能已关闭，请先开启", System.currentTimeMillis());
                return;
            }
            long currentTime = System.currentTimeMillis();
            long lastSentTime = getLong(LAST_SENT_TIME_KEY, 0);
            if (currentTime - lastSentTime < 1000) {
                toast("消息处理太频繁，请稍后重试");
                return;
            }
            handleDrawCommand(content, talker);
            putLong(LAST_SENT_TIME_KEY, currentTime);
        }
    } catch (Throwable t) {
        toast("消息处理失败：" + t.toString());
    }
}

void handleDrawCommand(String text, String talker) {
    try {
        String prompt = text.substring(1).trim();
        if (prompt.isEmpty()) {
            toast("提示词为空，请输入有效描述");
            return;
        }
        initCacheDir();
        sendDrawRequest(talker, prompt);
    } catch (Throwable t) {
        toast("AI绘画失败：" + t.toString());
    }
}

void sendDrawRequest(String talker, String prompt) {
    try {
        HashMap paramMap = new HashMap();
        paramMap.put("model", "Kwai-Kolors/Kolors");
        paramMap.put("prompt", prompt);
        paramMap.put("image_size", "1536x1024");
        paramMap.put("batch_size", 1);        
        paramMap.put("num_inference_steps", 20);
        paramMap.put("guidance_scale", 7.5);

        HashMap headerMap = new HashMap();
        headerMap.put("Authorization", "Bearer " + API_KEY);
        headerMap.put("Content-Type", "application/json");

        post(AI_DRAW_API, paramMap, headerMap, new PluginCallBack.HttpCallback() {
            public void onSuccess(int statusCode, String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String imageUrl = jsonResponse.getJSONArray("data").getJSONObject(0).getString("url");
                    downloadAndSend(talker, imageUrl, "aidraw");
                } catch (Throwable t) {
                    toast("解析响应失败：" + t.toString());
                }
            }
            public void onError(Exception e) {
                toast("请求失败：网络错误，请稍后重试");
            }
        });
    } catch (Throwable t) {
        toast("请求构造失败：" + t.toString());
    }
}

boolean downloadAndSend(String talker, String url, String filePrefix) {
    String savePath = PLUGIN_DIR + filePrefix + "_" + System.currentTimeMillis() + ".png";
    try {
        download(url, savePath, null, new PluginCallBack.DownloadCallback() {
            public void onSuccess(File file) {
                try {
                    if (!file.exists() || !file.canRead()) {
                        toast("图片文件不可用");
                        return;
                    }
                    sendImage(talker, file.getAbsolutePath());
                    toast("AI绘画图片发送成功");
                } catch (Throwable t) {
                    toast("图片发送失败：" + t.toString());
                }
            }
            public void onError(Exception e) {
                toast("图片获取失败：网络错误，请稍后重试");
            }
            public void onProgress(int progress) {}
        });
        return true;
    } catch (Throwable t) {
        toast("图片获取失败：网络不可用");
        return false;
    }
}

String encodeUrl(String text) {
    try {
        return URLEncoder.encode(text, "UTF-8");
    } catch (Throwable t) {
        return text;
    }
}

initCacheDir();