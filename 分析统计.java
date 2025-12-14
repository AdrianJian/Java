import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import me.hd.wauxv.data.bean.*;
import okhttp3.*;

String API_KEY = "sk-mosmbagzujowknozyikllwfyshycpzzsezsvkrenqtvugcrp";
String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
String PROMPT_PREFIX = "以下是微信聊天记录，格式为'[时间] 用户名: 消息内容'，请分析这些消息，总结主要话题、情感倾向、活跃用户，总结关键词并提供简洁的分析报告。特别注意强调: 分析报告中不能使用###符号,注意分段换行,不能太紧凑,需要精美排版（700字。）";
String CACHE_DIR = cacheDir + "/MessageStats/";
String IMAGE_CACHE_DIR = cacheDir + "/分析报告图/";
String NICKNAME_CACHE = "nicknames.json";
String TEXT_MSG_PREFIX = "text_messages_";
String STATS_PREFIX = "stats_";
HashMap userStatsCache = new HashMap();
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(240, TimeUnit.SECONDS)
    .readTimeout(240, TimeUnit.SECONDS)
    .writeTimeout(240, TimeUnit.SECONDS)
    .build();

void onLoad() {
    try {
        if (cacheDir == null || cacheDir.length() == 0 || pluginId == null || pluginId.length() == 0) {
            log("无效 cacheDir 或 pluginId");
            toast("插件加载失败");
            return;
        }
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        if (!cacheDir.canWrite()) {
            log("无写入权限: " + CACHE_DIR);
            toast("存储权限不足");
            return;
        }
        File[] talkerDirs = cacheDir.listFiles();
        if (talkerDirs != null) {
            for (File talkerDir : talkerDirs) {
                if (!talkerDir.isDirectory()) continue;
                String talker = talkerDir.getName();
                File[] dateFiles = talkerDir.listFiles();
                if (dateFiles != null) {
                    for (File file : dateFiles) {
                        String fileName = file.getName();
                        if (!fileName.endsWith(".json") || fileName.equals(NICKNAME_CACHE)) continue;
                        if (fileName.startsWith(STATS_PREFIX)) {
                            String date = fileName.substring(STATS_PREFIX.length(), fileName.length() - 5);
                            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) continue;
                            JSONObject stats = loadJsonData(talker, fileName);
                            if (stats.getJSONObject("users") == null) {
                                stats.put("users", new JSONObject());
                            }
                            userStatsCache.put(talker + "_" + date, stats);
                            log("加载统计缓存: " + file.getPath() + ", 内容: " + JSON.toJSONString(stats));
                        }
                    }
                }
            }
        }
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                saveStatsToFiles();
                cleanOldData();
            }
        }, 180, 180, TimeUnit.SECONDS);
        toast("消息统计插件加载成功");
        log("插件加载成功，缓存目录: " + CACHE_DIR);
    } catch (Exception e) {
        log("onLoad 错误: " + e.toString());
        toast("插件加载失败: " + e.toString());
    }
}

void onDisable() {
    try {
        saveStatsToFiles();
        scheduler.shutdown();
        toast("消息统计插件已禁用");
        log("插件已禁用");
    } catch (Exception e) {
        log("onDisable 错误: " + e.toString());
    }
}

void onHandleMsg(Object msgInfoBean) {
    try {
        if (!(msgInfoBean instanceof MsgInfoBean)) return;
        MsgInfoBean msg = (MsgInfoBean) msgInfoBean;
        String talker = msg.getTalker();
        String senderWxid = msg.getSendTalker();
        String content = msg.getContent();
        long timestamp = msg.getCreateTime();
        boolean isSystem = msg.isSystem();
        if (talker == null || senderWxid == null || isSystem) {
            log("无效消息: talker=" + talker + ", senderWxid=" + senderWxid + ", isSystem=" + isSystem);
            return;
        }
        String type = getMessageType(msg);
        if (type != null) {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp));
            cacheMessage(talker, senderWxid, content, date, type, timestamp);
            updateNickname(talker, senderWxid);
        }
    } catch (Exception e) {
        log("onHandleMsg 错误: " + e.toString());
    }
}

boolean onLongClickSendBtn(String text) {
    try {
        String talker = getTargetTalker();
        if (talker == null || talker.length() == 0) {
            toast("请先进入聊天");
            return true;
        }
        String content = text.trim();
        if (content.equals("统计")) {
            generateStatsReport(talker, "day", null);
            return true;
        } else if (content.equals("统计本周")) {
            generateStatsReport(talker, "week", null);
            return true;
        } else if (content.equals("统计本月")) {
            generateStatsReport(talker, "month", null);
            return true;
        } else if (content.startsWith("统计") && content.length() == 6 && content.substring(2).matches("\\d{4}")) {
            generateStatsReport(talker, "day", content.substring(2));
            return true;
        } else if (content.equals("分析")) {
            generateAnalysisReport(talker, "day", null, false);
            return true;
        } else if (content.equals("分析本周")) {
            generateAnalysisReport(talker, "week", null, false);
            return true;
        } else if (content.equals("分析本月")) {
            generateAnalysisReport(talker, "month", null, false);
            return true;
        } else if (content.equals("图分析")) {
            generateAnalysisReport(talker, "day", null, true);
            return true;
        } else if (content.equals("图分析本周")) {
            generateAnalysisReport(talker, "week", null, true);
            return true;
        } else if (content.equals("图分析本月")) {
            generateAnalysisReport(talker, "month", null, true);
            return true;
        }
        return false;
    } catch (Exception e) {
        log("onLongClickSendBtn 错误: " + e.toString());
        toast("操作失败");
        return true;
    }
}

void onMemberChange(String type, String groupWxid, String userWxid, String userName) {
    try {
        if (type.equals("add") || type.equals("invite")) {
            if (userName != null && userName.length() > 0) {
                updateNickname(groupWxid, userWxid, userName);
            }
        } else if (type.equals("del") || type.equals("kick")) {
            removeNickname(groupWxid, userWxid);
        }
    } catch (Exception e) {
        log("onMemberChange 错误: " + e.toString());
    }
}

String getMessageType(MsgInfoBean msg) {
    if (msg.isText()) return "text";
    if (msg.isImage()) return "image";
    if (msg.isVoice()) return "voice";
    if (msg.isVideo()) return "video";
    if (msg.isEmoji()) return "emoji";
    if (msg.isLink()) return "link";
    return null;
}

void cacheMessage(String talker, String senderWxid, String content, String date, String type, long timestamp) {
    try {
        String cacheKey = talker + "_" + date;
        String statsFilePath = CACHE_DIR + talker + "/" + STATS_PREFIX + date + ".json";
        JSONObject stats = (JSONObject) userStatsCache.get(cacheKey);
        if (stats == null) {
            stats = loadJsonData(talker, STATS_PREFIX + date + ".json");
            if (stats.getJSONObject("users") == null) {
                stats.put("users", new JSONObject());
            }
            userStatsCache.put(cacheKey, stats);
            log("加载统计文件: " + statsFilePath + ", 内容: " + JSON.toJSONString(stats));
        }
        stats.put("total", stats.getIntValue("total", 0) + 1);
        stats.put("type_" + type, stats.getIntValue("type_" + type, 0) + 1);
        JSONObject userStats = stats.getJSONObject("users");
        userStats.put(senderWxid, userStats.getIntValue(senderWxid, 0) + 1);
        if (type.equals("text")) {
            saveTextMessage(talker, date, senderWxid, content, timestamp);
        }
        writeFile(statsFilePath, JSON.toJSONString(stats));
        log("统计数据已写入: " + statsFilePath + ", 内容: " + JSON.toJSONString(stats));
    } catch (Exception e) {
        log("cacheMessage 错误: " + e.toString());
    }
}

void saveTextMessage(String talker, String date, String senderWxid, String content, long timestamp) {
    try {
        if (talker == null || talker.length() == 0 || content == null || content.length() == 0) {
            log("无效文本消息: talker=" + talker + ", content=" + content);
            return;
        }
        String filePath = CACHE_DIR + talker + "/" + TEXT_MSG_PREFIX + date + ".json";
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.getParentFile().canWrite()) {
            log("无法写入目录: " + file.getParentFile().getPath());
            return;
        }
        String senderName = getNickname(talker, senderWxid);
        JSONArray messages = new JSONArray();
        if (file.exists()) {
            String data = readFile(filePath);
            if (data != null && data.length() > 0) {
                try {
                    messages = JSON.parseArray(data);
                    log("读取文本消息文件: " + filePath + ", 消息数: " + messages.size());
                } catch (Exception e) {
                    log("解析 JSON 文件失败: " + filePath + ", 错误: " + e.toString());
                    messages = new JSONArray();
                }
            }
        }
        JSONObject msgObj = new JSONObject();
        msgObj.put("msgId", System.currentTimeMillis());
        msgObj.put("senderName", senderName);
        msgObj.put("content", content);
        msgObj.put("timestamp", new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp)));
        messages.add(msgObj);
        writeFile(filePath, JSON.toJSONString(messages));
        log("文本消息已写入: " + filePath + ", 消息数: " + messages.size());
    } catch (Exception e) {
        log("saveTextMessage 错误: " + e.toString());
    }
}

void updateNickname(String talker, String wxid, String name) {
    try {
        String filePath = CACHE_DIR + talker + "/" + NICKNAME_CACHE;
        JSONObject nicknames = loadJsonData(talker, NICKNAME_CACHE);
        if (name != null && name.length() > 0) {
            nicknames.put(wxid, name);
            writeFile(filePath, JSON.toJSONString(nicknames));
            log("更新昵称: talker=" + talker + ", wxid=" + wxid + ", name=" + name);
        } else {
            String fetchedName = getFriendName(wxid, talker);
            if (fetchedName != null && fetchedName.length() > 0) {
                nicknames.put(wxid, fetchedName);
                writeFile(filePath, JSON.toJSONString(nicknames));
                log("获取并更新昵称: talker=" + talker + ", wxid=" + wxid + ", name=" + fetchedName);
            }
        }
    } catch (Exception e) {
        log("updateNickname 错误: " + e.toString());
    }
}

void updateNickname(String talker, String wxid) {
    updateNickname(talker, wxid, null);
}

void removeNickname(String talker, String wxid) {
    try {
        String filePath = CACHE_DIR + talker + "/" + NICKNAME_CACHE;
        JSONObject nicknames = loadJsonData(talker, NICKNAME_CACHE);
        nicknames.remove(wxid);
        writeFile(filePath, JSON.toJSONString(nicknames));
        log("移除昵称: talker=" + talker + ", wxid=" + wxid);
    } catch (Exception e) {
        log("removeNickname 错误: " + e.toString());
    }
}

String getNickname(String talker, String wxid) {
    try {
        JSONObject nicknames = loadJsonData(talker, NICKNAME_CACHE);
        String name = nicknames.getString(wxid);
        if (name == null || name.length() == 0) {
            name = getFriendName(wxid, talker);
            if (name != null && name.length() > 0) {
                updateNickname(talker, wxid, name);
            } else {
                name = "未知用户_" + wxid.substring(0, Math.min(6, wxid.length()));
            }
        }
        return name;
    } catch (Exception e) {
        log("getNickname 错误: " + e.toString());
        return "未知用户_" + wxid.substring(0, Math.min(6, wxid.length()));
    }
}

void generateStatsReport(String talker, String period, String mmdd) {
    try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat displaySdf = new SimpleDateFormat("M.dd");
        Date now = new Date();
        String endDate = sdf.format(now);
        String startDate = endDate;
        String periodDesc = "(今天)";
        List dates = new ArrayList();
        if (period.equals("week")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -6);
            startDate = sdf.format(cal.getTime());
            periodDesc = "(本周)";
            for (int i = 0; i < 7; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                dates.add(sdf.format(cal.getTime()));
            }
        } else if (period.equals("month")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            startDate = sdf.format(cal.getTime());
            periodDesc = "(本月)";
            while (!cal.getTime().after(now)) {
                dates.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else if (mmdd != null) {
            String date = "2025-" + mmdd.substring(0, 2) + "-" + mmdd.substring(2, 4);
            Date inputDate = sdf.parse(date);
            if (inputDate.after(now) || inputDate.before(new Date(now.getTime() - 30L * 24 * 60 * 60 * 1000))) {
                sendText(talker, "日期无效，仅支持最近30天！");
                return;
            }
            startDate = date;
            endDate = date;
            dates.add(date);
            periodDesc = "(指定日期)";
        } else {
            dates.add(endDate);
        }
        int textCount = 0, imageCount = 0, voiceCount = 0, videoCount = 0, emojiCount = 0, linkCount = 0;
        Map userCounts = new HashMap();
        for (String date : (List) dates) {
            String cacheKey = talker + "_" + date;
            JSONObject stats = (JSONObject) userStatsCache.get(cacheKey);
            if (stats == null) {
                stats = loadJsonData(talker, STATS_PREFIX + date + ".json");
                if (stats.getJSONObject("users") == null) {
                    stats.put("users", new JSONObject());
                }
                userStatsCache.put(cacheKey, stats);
                log("加载统计数据: talker=" + talker + ", date=" + date + ", 内容: " + JSON.toJSONString(stats));
            }
            textCount += stats.getIntValue("type_text", 0);
            imageCount += stats.getIntValue("type_image", 0);
            voiceCount += stats.getIntValue("type_voice", 0);
            videoCount += stats.getIntValue("type_video", 0);
            emojiCount += stats.getIntValue("type_emoji", 0);
            linkCount += stats.getIntValue("type_link", 0);
            JSONObject userStats = stats.getJSONObject("users");
            if (userStats != null && !userStats.isEmpty()) {
                for (Map.Entry entry : userStats.entrySet()) {
                    String wxid = (String) entry.getKey();
                    int count = ((Integer) entry.getValue()).intValue();
                    String key = talker + "_" + wxid;
                    userCounts.put(key, ((Integer) userCounts.getOrDefault(key, 0)).intValue() + count);
                }
            }
        }
        List topUsers = new ArrayList();
        for (Map.Entry entry : userCounts.entrySet()) {
            String key = (String) entry.getKey();
            int count = ((Integer) entry.getValue()).intValue();
            String wxid = key.substring(talker.length() + 1);
            String name = getNickname(talker, wxid);
            topUsers.add(new Object[] { wxid, name, count });
        }
        topUsers.sort(new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Integer) ((Object[]) o2)[2]).compareTo((Integer) ((Object[]) o1)[2]);
            }
        });
        StringBuilder report = new StringBuilder();
        report.append("🌟 消息统计 · 活跃榜单 🌟\n");
        report.append("📅 统计周期：").append(displaySdf.format(sdf.parse(startDate)))
              .append("-").append(displaySdf.format(sdf.parse(endDate))).append(periodDesc).append("\n");
        report.append("----------------------------\n");
        report.append("📊 消息概览\n");
        report.append("文本消息：").append(textCount).append(" 条\n");
        report.append("图片消息：").append(imageCount).append(" 张\n");
        report.append("语音消息：").append(voiceCount).append(" 条\n");
        report.append("视频消息：").append(videoCount).append(" 条\n");
        report.append("表情包    ：").append(emojiCount).append(" 个\n");
        report.append("链接消息：").append(linkCount).append(" 条\n");
        report.append("活跃人数：").append(topUsers.size()).append(" 人\n");
        report.append("总消息    ：").append(textCount + imageCount + voiceCount + videoCount + emojiCount + linkCount).append(" 条\n");
        report.append("----------------------------\n");
        report.append("🏆 发言 Top 10\n");
        for (int i = 0; i < Math.min(10, topUsers.size()); i++) {
            Object[] stat = (Object[]) topUsers.get(i);
            report.append(i + 1).append(". ").append(stat[1]).append("：").append(stat[2]).append(" 条\n");
        }
        report.append("----------------------------");
        sendText(talker, report.toString());
        toast("统计报告已发送");
        log("生成统计报告: talker=" + talker + ", 周期=" + period);
    } catch (Exception e) {
        log("generateStatsReport 错误: " + e.toString());
        toast("生成报告失败");
    }
}

void generateAnalysisReport(String talker, String period, String mmdd, boolean isImageMode) {
    try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat displaySdf = new SimpleDateFormat("M.dd");
        Date now = new Date();
        String endDate = sdf.format(now);
        String startDate = endDate;
        List dates = new ArrayList();
        String periodDesc = "(今天)";
        if (period.equals("week")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -6);
            startDate = sdf.format(cal.getTime());
            periodDesc = "(本周)";
            for (int i = 0; i < 7; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                dates.add(sdf.format(cal.getTime()));
            }
        } else if (period.equals("month")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            startDate = sdf.format(cal.getTime());
            periodDesc = "(本月)";
            while (!cal.getTime().after(now)) {
                dates.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else if (mmdd != null) {
            String date = "2025-" + mmdd.substring(0, 2) + "-" + mmdd.substring(2, 4);
            Date inputDate = sdf.parse(date);
            if (inputDate.after(now) || inputDate.before(new Date(now.getTime() - 30L * 24 * 60 * 60 * 1000))) {
                sendText(talker, "日期无效，仅支持最近30天！");
                return;
            }
            startDate = date;
            endDate = date;
            dates.add(date);
            periodDesc = "(指定日期)";
        } else {
            dates.add(endDate);
        }
        StringBuilder chatLog = new StringBuilder(PROMPT_PREFIX);
        for (String date : (List) dates) {
            String filePath = CACHE_DIR + talker + "/" + TEXT_MSG_PREFIX + date + ".json";
            File file = new File(filePath);
            if (file.exists()) {
                String data = readFile(filePath);
                if (data != null && data.length() > 0) {
                    JSONArray messages = JSON.parseArray(data);
                    for (int i = 0; i < messages.size(); i++) {
                        JSONObject msg = messages.getJSONObject(i);
                        String senderName = msg.getString("senderName");
                        String content = msg.getString("content");
                        String timestamp = msg.getString("timestamp");
                        chatLog.append("[").append(timestamp).append("] ").append(senderName).append(": ").append(content).append("\n");
                    }
                    log("读取分析消息: " + filePath + ", 消息数: " + messages.size());
                }
            }
        }
        if (chatLog.length() == PROMPT_PREFIX.length()) {
            sendText(talker, "无文本消息可分析" + periodDesc);
            return;
        }
        Map paramMap = new HashMap();
        paramMap.put("model", "deepseek-ai/DeepSeek-V3");
        List messagesArray = new ArrayList();
        Map userMessage = new HashMap();
        userMessage.put("role", "user");
        userMessage.put("content", chatLog.toString());
        messagesArray.add(userMessage);
        paramMap.put("messages", messagesArray);
        paramMap.put("stream", false);
        paramMap.put("max_tokens", 1024);
        paramMap.put("temperature", 0.7);
        paramMap.put("top_p", 0.7);
        paramMap.put("frequency_penalty", 0.5);
        Map responseFormat = new HashMap();
        responseFormat.put("type", "text");
        paramMap.put("response_format", responseFormat);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), JSON.toJSONString(paramMap));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        String error = "API请求失败: HTTP " + response.code() + ", " + response.message();
                        log(error);
                        toast("分析失败：网络请求错误");
                        sendText(talker, "分析失败：网络请求错误");
                        return;
                    }
                    String responseBody = response.body().string();
                    JSONObject json = JSONObject.parseObject(responseBody);
                    String content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    log("API返回的原始content: " + content);
                    content = content.replaceAll("^\n|\r\n|\r", "").trim(); 
                    StringBuilder report = new StringBuilder();
                    report.append("聊天分析报告\n");
                    report.append("分析周期：").append(displaySdf.format(sdf.parse(startDate)))
                          .append("-").append(displaySdf.format(sdf.parse(endDate))).append(periodDesc).append("\n");
                    report.append("----------------------------\n");
                    report.append("分析报告：\n");
                    report.append("\n");
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        if (line.trim().length() > 0) { 
                            report.append(line.trim()).append("\n");
                        }
                    }
                    report.append("----------------------------");
                    log("构造的report内容: " + report.toString());
                    if (isImageMode) {
                        generateImageReport(talker, report.toString());
                    } else {
                        sendText(talker, report.toString());
                        toast("分析报告已发送");
                        log("分析报告已发送: talker=" + talker + ", 周期=" + period);
                    }
                } catch (Exception e) {
                    log("解析分析响应错误: " + e.toString());
                    toast("分析失败：响应解析错误");
                    sendText(talker, "分析失败：响应解析错误");
                } finally {
                    response.close();
                }
            }
            public void onFailure(Call call, IOException e) {
                log("API请求失败: " + e.toString());
                toast("分析失败：网络请求错误");
                sendText(talker, "分析失败：网络请求错误");
            }
        });
    } catch (Exception e) {
        log("generateAnalysisReport 错误: " + e.toString());
        toast("消息分析失败");
    }
}

void generateImageReport(String talker, String report) {
    try {
        File cacheDir = new File(IMAGE_CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            log("创建图片缓存目录: " + IMAGE_CACHE_DIR);
        }
        if (!cacheDir.canWrite()) {
            log("无写入权限: " + IMAGE_CACHE_DIR);
            toast("存储权限不足");
            return;
        }
        File[] files = cacheDir.listFiles();
        if (files != null && files.length >= 5) {
            Arrays.sort(files, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return Long.compare(((File) o1).lastModified(), ((File) o2).lastModified());
                }
            });
            for (int i = 0; i < files.length - 4; i++) {
                files[i].delete();
                log("删除过期图片缓存: " + files[i].getPath());
            }
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String imagePath = IMAGE_CACHE_DIR + "report_" + timestamp + ".png";
        log("生成图片路径: " + imagePath);

        // 动态添加换行符
        StringBuilder formattedText = new StringBuilder();
        String[] lines = report.split("(?<=[:：])\\s*"); // 以冒号后空格分割，保留分段标题
        boolean isAfterReport = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("聊天分析报告")) {
                formattedText.append(line).append("\n\n"); // 标题后两个换行符
            } else if (line.startsWith("分析周期：")) {
                formattedText.append(line).append("\n");
            } else if (line.equals("----------------------------")) {
                formattedText.append(line).append("\n");
            } else if (line.startsWith("分析报告：")) {
                formattedText.append(line).append("\n\n"); // 分析报告后两个换行
                isAfterReport = true;
            } else if (isAfterReport && (line.startsWith("主要话题：") || line.startsWith("情感倾向：") ||
                    line.startsWith("活跃用户：") || line.startsWith("关键词：") || line.startsWith("特殊发现："))) {
                formattedText.append("\n").append(line).append("\n"); // 分段标题前一个换行，后一个换行
            } else {
                formattedText.append(line).append("\n"); // 内容行后一个换行
            }
        }
        String preEncodedText = formattedText.toString().trim().replaceAll("\n", "%5Cn");
        log("原始report内容: " + report);
        log("格式化后的text参数: " + preEncodedText);

        // 构造URL，使用字体9、随机颜色、字体大小20，type=0
        String url = "https://api.zxz.ee/api/wbzt/?text=" + preEncodedText + 
                     "&font=9&size=20&randcolor=1&hh=%5Cn&type=0";
        log("图片生成请求URL: " + url);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        String error = "文本转图API请求失败: HTTP " + response.code() + ", " + response.message();
                        log(error);
                        toast("图片生成失败：网络请求错误");
                        sendText(talker, "图片生成失败：网络请求错误");
                        return;
                    }
                    InputStream inputStream = response.body().byteStream();
                    FileOutputStream fos = new FileOutputStream(imagePath);
                    byte[] buffer = new byte[1024];
                    int len;
                    int totalBytes = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        totalBytes += len;
                    }
                    fos.flush();
                    fos.close();
                    inputStream.close();
                    log("图片已保存: " + imagePath + ", 文件大小: " + totalBytes + " 字节");

                    sendImage(talker, imagePath);
                    toast("分析报告图片已发送");
                    log("分析报告图片已发送: talker=" + talker + ", 路径=" + imagePath);
                } catch (Exception e) {
                    log("图片生成或发送错误: " + e.toString() + ", 堆栈: " + Arrays.toString(e.getStackTrace()));
                    toast("图片生成失败");
                    sendText(talker, "图片生成失败");
                } finally {
                    response.close();
                    log("响应已关闭");
                }
            }
            public void onFailure(Call call, IOException e) {
                log("文本转图API请求失败: " + e.toString() + ", 堆栈: " + Arrays.toString(e.getStackTrace()));
                toast("图片生成失败：网络请求错误");
                sendText(talker, "图片生成失败：网络请求错误");
            }
        });
    } catch (Exception e) {
        log("generateImageReport 错误: " + e.toString() + ", 堆栈: " + Arrays.toString(e.getStackTrace()));
        toast("图片生成失败");
    }
}

void cleanOldData() {
    try {
        File cacheDir = new File(CACHE_DIR);
        File[] talkerDirs = cacheDir.listFiles();
        if (talkerDirs == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        long thirtyDaysAgo = now.getTime() - 30L * 24 * 60 * 60 * 1000;
        for (File talkerDir : talkerDirs) {
            if (!talkerDir.isDirectory()) continue;
            File[] dateFiles = talkerDir.listFiles();
            if (dateFiles == null) continue;
            for (File file : dateFiles) {
                String fileName = file.getName();
                if (!fileName.endsWith(".json") || fileName.equals(NICKNAME_CACHE)) continue;
                String dateStr = fileName.startsWith(STATS_PREFIX) ? fileName.substring(STATS_PREFIX.length(), fileName.length() - 5)
                                                                  : fileName.substring(TEXT_MSG_PREFIX.length(), fileName.length() - 5);
                try {
                    Date dataDate = sdf.parse(dateStr);
                    if (dataDate.getTime() < thirtyDaysAgo) {
                        file.delete();
                        log("删除过期缓存: " + file.getPath());
                    }
                } catch (Exception e) {}
            }
        }
    } catch (Exception e) {
        log("cleanOldData 错误: " + e.toString());
    }
}

JSONObject loadJsonData(String talker, String fileName) {
    try {
        String filePath = CACHE_DIR + talker + "/" + fileName;
        String data = readFile(filePath);
        JSONObject json = data != null && data.length() > 0 ? JSONObject.parseObject(data) : new JSONObject();
        if (json.getJSONObject("users") == null) {
            json.put("users", new JSONObject());
        }
        return json;
    } catch (Exception e) {
        log("loadJsonData 错误: " + e.toString());
        return new JSONObject();
    }
}

String readFile(String path) {
    try {
        File file = new File(path);
        if (!file.exists()) return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line).append("\n");
        }
        br.close();
        return text.length() > 0 ? text.toString().trim() : null;
    } catch (Exception e) {
        log("readFile 错误: " + e.toString());
        return null;
    }
}

void writeFile(String path, String data) {
    try {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.getParentFile().canWrite()) {
            log("无法写入目录: " + file.getParentFile().getPath());
            return;
        }
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        osw.write(data);
        osw.flush();
        osw.close();
        fos.close();
    } catch (Exception e) {
        log("writeFile 错误: " + e.toString());
    }
}