import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import me.hd.wauxv.plugin.api.callback.PluginCallBack.HttpCallback;
import java.text.SimpleDateFormat;
import java.util.Date;

// 星座运程插件
long[] recentMsgIds = new long[10]; // 存储最近 10 个 msgId
int msgIdIndex = 0;

void onHandleMsg(Object msgInfo) {
    long msgId = msgInfo.getMsgId();
    for (int i = 0; i < recentMsgIds.length; i++) {
        if (recentMsgIds[i] == msgId) {
            log("忽略重复消息: msgId=" + msgId);
            return;
        }
    }
    recentMsgIds[msgIdIndex] = msgId;
    msgIdIndex = (msgIdIndex + 1) % recentMsgIds.length;

    log("收到消息: " + msgInfo.toString());
    long createTime = msgInfo.getCreateTime();
    long currentTime = System.currentTimeMillis();
    if (currentTime - createTime >= 60 * 1000) {
        log("消息已过期，createTime=" + createTime + ", currentTime=" + currentTime);
        return;
    }

    if (msgInfo.isText()) {
        String content = msgInfo.getContent().trim();
        String talker = msgInfo.getTalker();
        boolean isRoomMsg = talker.endsWith("@chatroom");

        log("处理消息: content=" + content + ", talker=" + talker + ", isRoomMsg=" + isRoomMsg);

        // 忽略插件自身的错误提示
        String[] constellations = {
            "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
            "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
        };
        for (int i = 0; i < constellations.length; i++) {
            if (content.startsWith("获取" + constellations[i] + "运势失败")) {
                log("忽略插件自身发送的错误提示: content=" + content);
                return;
            }
        }

        // 添加帮助指令
        if (content.equals("星座帮助")) {
            String help = "🌟 星座运势查询\n" +
                         "支持指令：\n" +
                         "- 星座+运势/运程（如：处女座运势）\n" +
                         "- 星座+明天运势/运程\n" +
                         "- 星座+本周运势/运程\n" +
                         "- 星座+本月运势/运程\n" +
                         "支持星座：白羊座、金牛座、双子座、巨蟹座、狮子座、处女座、\n" +
                         "天秤座、天蝎座、射手座、摩羯座、水瓶座、双鱼座";
            sendText(talker, help);
            log("发送帮助信息");
            return;
        }

        // 触发指令处理
        String constellation = "";
        String time = "today"; // 默认今日运势
        for (int i = 0; i < constellations.length; i++) {
            String prefix = constellations[i];
            if (content.equals(prefix + "运势") || content.equals(prefix + "运程")) {
                constellation = prefix;
                break;
            } else if (content.equals(prefix + "明天运势") || content.equals(prefix + "明天运程")) {
                constellation = prefix;
                time = "nextday";
                break;
            } else if (content.equals(prefix + "本周运势") || content.equals(prefix + "本周运程")) {
                constellation = prefix;
                time = "week";
                break;
            } else if (content.equals(prefix + "本月运势") || content.equals(prefix + "本月运程")) {
                constellation = prefix;
                time = "month";
                break;
            }
        }

        if (!constellation.isEmpty()) {
            log("识别到星座指令: constellation=" + constellation + ", time=" + time);
            getHoroscope(talker, constellation, time);
        } else {
            log("未匹配到星座指令: content=" + content);
        }
    }
}

// 获取星座运势
void getHoroscope(String talker, String constellation, String time) {
    // 转换为 API 所需的星座英文名
    String astro = convertToAstro(constellation);
    if (astro.isEmpty()) {
        sendText(talker, "无效的星座名称：" + constellation);
        log("星座转换失败: constellation=" + constellation);
        return;
    }

    // 构建 API URL
    String apiUrl = "https://api.vvhan.com/api/horoscope?type=" + astro + "&time=" + time;
    log("星座 API 请求: url=" + apiUrl);

    get(apiUrl, null, new HttpCallback() {
        public void onSuccess(int code, String content) {
            log("星座 API 响应: code=" + code + ", content=" + content);
            try {
                JSONObject jsonObject = JSON.parseObject(content);
                String success = safeGetJsonPath(jsonObject, "$.success", "false");
                if (!"true".equals(success)) {
                    String errorMsg = safeGetJsonPath(jsonObject, "$.message", "未知错误");
                    sendText(talker, "获取" + constellation + "运势失败：" + errorMsg);
                    log("星座 API 失败: success=" + success + ", message=" + errorMsg + ", url=" + apiUrl);
                    return;
                }

                // 提取运势数据
                String date = safeGetJsonPath(jsonObject, "$.data.time", "未知");
                String overall = safeGetJsonPath(jsonObject, "$.data.index.all", "未知");
                String love = safeGetJsonPath(jsonObject, "$.data.index.love", "未知");
                String career = safeGetJsonPath(jsonObject, "$.data.index.work", "未知");
                String wealth = safeGetJsonPath(jsonObject, "$.data.index.money", "未知");
                String health = safeGetJsonPath(jsonObject, "$.data.index.health", "未知");
                String luckyColor = safeGetJsonPath(jsonObject, "$.data.luckycolor", "未知");
                String luckyNumber = safeGetJsonPath(jsonObject, "$.data.luckynumber", "未知");
                String summary = safeGetJsonPath(jsonObject, "$.data.shortcomment", "未知");
                String overallText = truncateText(safeGetJsonPath(jsonObject, "$.data.fortunetext.all", "未知"), 40);
                String loveText = truncateText(safeGetJsonPath(jsonObject, "$.data.fortunetext.love", "未知"), 40);
                String careerText = truncateText(safeGetJsonPath(jsonObject, "$.data.fortunetext.work", "未知"), 40);
                String wealthText = truncateText(safeGetJsonPath(jsonObject, "$.data.fortunetext.money", "未知"), 40);
                String healthText = truncateText(safeGetJsonPath(jsonObject, "$.data.fortunetext.health", "未知"), 40);

                // 检查关键字段
                if (overall.equals("未知") || summary.equals("未知")) {
                    sendText(talker, "获取" + constellation + "运势失败：数据缺失");
                    log("运势数据缺失: overall=" + overall + ", summary=" + summary);
                    return;
                }

                // 构建精简输出
                StringBuilder response = new StringBuilder();
                response.append("🌟 ").append(constellation).append(getTypeDesc(time)).append(" (").append(date).append(")\n");
                response.append("── 综合运势 ──\n");
                response.append("⭐ ").append(overall).append(" - ").append(summary).append("\n");
                response.append("── 详细运势 ──\n");
                response.append("💖 爱情: ").append(love).append(" - ").append(loveText).append("\n");
                response.append("💼 事业: ").append(career).append(" - ").append(careerText).append("\n");
                response.append("💰 财富: ").append(wealth).append(" - ").append(wealthText).append("\n");
                response.append("🏥 健康: ").append(health).append(" - ").append(healthText).append("\n");
                response.append("── 幸运锦囊 ──\n");
                response.append("🎨 幸运色: ").append(luckyColor).append("\n");
                response.append("🔢 幸运数: ").append(luckyNumber);

                sendText(talker, response.toString());
                log("运势信息发送成功: constellation=" + constellation + ", time=" + time);
            } catch (Exception e) {
                sendText(talker, "获取" + constellation + "运势失败：数据解析错误");
                log("星座 API 解析异常: error=" + e.toString());
            }
        }

        public void onError(Exception e) {
            sendText(talker, "获取" + constellation + "运势失败：请稍后重试");
            log("星座 API 请求异常: error=" + (e != null ? e.toString() : "未知异常"));
        }
    });
}

// 截取文本，限制长度
String truncateText(String text, int maxLength) {
    if (text.equals("未知") || text.length() <= maxLength) {
        return text;
    }
    // 找到最后一个句号或逗号
    int endIndex = text.lastIndexOf("。", maxLength);
    if (endIndex == -1) {
        endIndex = text.lastIndexOf("，", maxLength);
    }
    if (endIndex == -1) {
        endIndex = maxLength;
    }
    return text.substring(0, endIndex) + "…";
}

// 安全获取 JSONPath 值
String safeGetJsonPath(JSONObject json, String path, String defaultValue) {
    try {
        Object result = JSONPath.eval(json, path);
        return result != null ? result.toString() : defaultValue;
    } catch (Exception e) {
        log("JSONPath 解析失败: path=" + path + ", error=" + e.toString());
        return defaultValue;
    }
}

// 转换中文星座名为 API 所需的英文名
String convertToAstro(String constellation) {
    if (constellation.equals("白羊座")) return "aries";
    if (constellation.equals("金牛座")) return "taurus";
    if (constellation.equals("双子座")) return "gemini";
    if (constellation.equals("巨蟹座")) return "cancer";
    if (constellation.equals("狮子座")) return "leo";
    if (constellation.equals("处女座")) return "virgo";
    if (constellation.equals("天秤座")) return "libra";
    if (constellation.equals("天蝎座")) return "scorpio";
    if (constellation.equals("射手座")) return "sagittarius";
    if (constellation.equals("摩羯座")) return "capricorn";
    if (constellation.equals("水瓶座")) return "aquarius";
    if (constellation.equals("双鱼座")) return "pisces";
    return "";
}

// 获取运势类型描述
String getTypeDesc(String time) {
    if (time.equals("today")) return "今日运势";
    if (time.equals("nextday")) return "明日运势";
    if (time.equals("week")) return "本周运势";
    if (time.equals("month")) return "本月运势";
    return "运势";
}

// 调试日志
void log(String message) {
    boolean debugMode = true;
    if (debugMode) {
        me.hd.wauxv.plugin.api.Logger.log("Wa_星座运程_JP: " + message);
    }
}