import java.io.File;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson2.JSONObject;
import me.hd.wauxv.plugin.api.callback.PluginCallBack;

String BASE_URL = "https://www.yx520.ltd/API/stzz/api.php";
String CACHE_DIR = cacheDir + "/Gif/";
int DEFAULT_FONT = 7;
int DEFAULT_EFFECT = 39;
int currentFont = DEFAULT_FONT;
int currentEffect = DEFAULT_EFFECT;

File cacheDirFile = new File(CACHE_DIR);
if (!cacheDirFile.exists()) {
    cacheDirFile.mkdirs();
}

boolean onLongClickSendBtn(String text) {
    if (text.startsWith("fg")) {
        String content = text.substring(2).trim();
        if (content.isEmpty()) {
            toast("请输入发光字体内容，例如：fg发光字体");
            return true;
        }
        sendGlowText(content);
        return true;
    }
    else if (text.startsWith("字体")) {
        String fontStr = text.substring(2).trim();
        try {
            int font = Integer.parseInt(fontStr);
            if (font >= 1 && font <= 14) {
                currentFont = font;
                putInt("currentFont", currentFont);
                toast("字体已设置为 " + font);
            } else {
                toast("字体范围应为 1~14");
            }
        } catch (Exception e) {
            toast("请输入有效字体编号，例如：字体7");
        }
        return true;
    }
    else if (text.startsWith("效果")) {
        String effectStr = text.substring(2).trim();
        try {
            int effect = Integer.parseInt(effectStr);
            if (effect >= 1 && effect <= 37) {
                currentEffect = effect;
                putInt("currentEffect", currentEffect);
                toast("效果已设置为 " + effect);
            } else {
                toast("效果范围应为 1~37");
            }
        } catch (Exception e) {
            toast("请输入有效效果编号，例如：效果39");
        }
        return true;
    }
    return false;
}

void sendGlowText(String content) {
    String talker = getTargetTalker();
    String fileName = System.currentTimeMillis() + ".gif";
    String filePath = CACHE_DIR + fileName;
    cleanCache();
    String encodedContent = content.replace("\n", "\\n");
    String url = BASE_URL + "?text=" + encodedContent + "&zt=" + currentFont + "&xg=" + currentEffect + "&msg=img";
    log("请求 URL: " + url);
    log("目标文件路径: " + filePath);
    download(url, filePath, null, new PluginCallBack.DownloadCallback() {
        public void onSuccess(File file) {
            String absolutePath = file.getAbsolutePath();
            log("下载成功，文件路径: " + absolutePath);
            try {
                sendEmoji(talker, absolutePath);
                toast("发光字体 GIF 发送成功");
            } catch (Exception e) {
                log("sendEmoji 失败: " + e.getMessage());
                toast("发送失败: " + e.getMessage());
            }
        }
        public void onError(Exception e) {
            log("下载失败: " + e.getMessage());
            toast("下载失败: " + e.getMessage());
        }
    });
}

void cleanCache() {
    File dir = new File(CACHE_DIR);
    File[] files = dir.listFiles();
    if (files != null && files.length > 5) {
        File oldestFile = files[0];
        long oldestTime = oldestFile.lastModified();
        for (File file : files) {
            if (file.lastModified() < oldestTime) {
                oldestFile = file;
                oldestTime = file.lastModified();
            }
        }
        oldestFile.delete();
        log("删除最旧缓存文件: " + oldestFile.getAbsolutePath());
    }
}

void init() {
    currentFont = getInt("currentFont", DEFAULT_FONT);
    currentEffect = getInt("currentEffect", DEFAULT_EFFECT);
}

init();