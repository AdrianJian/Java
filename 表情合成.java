import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import java.io.File;
import me.hd.wauxv.plugin.api.callback.PluginCallBack;

boolean onLongClickSendBtn(String content) {
    String talker = getTargetTalker();
    return processEmoji(content, talker);
}

private boolean processEmoji(String content, String talker) {
    if (content == null || content.codePointCount(0, content.length()) < 2) {
        return false;
    }

    if (!isValidEmojiPair(content)) {
        return false;
    }

    int firstEmojiEnd = content.offsetByCodePoints(0, 1);
    int secondEmojiEnd = content.offsetByCodePoints(firstEmojiEnd, 1);
    String emoji1 = content.substring(0, firstEmojiEnd);
    String emoji2 = content.substring(firstEmojiEnd, secondEmojiEnd);

    if (emoji1.isEmpty() || emoji2.isEmpty()) {
        return false;
    }

    get("https://api.317ak.com/API/yljk/emo/emo.php?emoji1=" + emoji1 + "&emoji2=" + emoji2, null, new PluginCallBack.HttpCallback() {
        public void onSuccess(int respCode, String respContent) {
            JSONObject jsonObject = JSON.parseObject(respContent);
            int code = JSONPath.eval(jsonObject, "$.code");
            if (code == 1) {
                String url = JSONPath.eval(jsonObject, "$.data.url");
                if (url == null || url.isEmpty()) {
                    return;
                }
                download(url, pluginDir + "/emoji.png", null, new PluginCallBack.DownloadCallback() {
                    public void onSuccess(File file) {
                        sendEmoji(talker, file.getAbsolutePath());
                    }

                    public void onError(Exception e) {
                    }
                });
            }
        }

        public void onError(Exception e) {
        }
    });
    return true;
}

private boolean isValidEmojiPair(String content) {
    int codePointCount = content.codePointCount(0, content.length());
    if (codePointCount < 2) {
        return false;
    }

    for (int i = 0; i < content.length(); ) {
        int codePoint = content.codePointAt(i);
        if (codePoint < 0x1F300 || codePoint > 0x1F9FF) {
            return false;
        }
        i += Character.charCount(codePoint);
    }
    return true;
}