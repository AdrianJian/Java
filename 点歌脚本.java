import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.*;
import java.util.*;
import java.time.Duration;
import okhttp3.*;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;

public boolean onLongClickSendBtn(String text) {
    String group = getTargetTalker();
    if (text.equals("开启点歌")) {
        putInt(group, "开关", 1);
        insertSystemMsg(group, "该聊天点歌已开启", System.currentTimeMillis());
        return true;
    } else if (text.equals("关闭点歌")) {
        putInt(group, "开关", 0);
        insertSystemMsg(group, "该聊天点歌已关闭", System.currentTimeMillis());
        return true;
    }
    return false;
}

public void onHandleMsg(Object data) {
    String text = data.content;
    String qun = data.talker;
    String wxid = data.sendTalker;
    if (isFiltered(data)) return;
    String avatarUrl = getAvatarUrl(wxid, true);
    if (avatarUrl == null || avatarUrl.isEmpty()) {
        sendQuoteMsg(qun, data.msgId, "无法获取用户头像");
        return;
    }
    String nickname = data.isGroupChat() ? getFriendName(wxid, qun) : getFriendName(wxid);
    if (nickname == null || nickname.isEmpty()) {
        sendQuoteMsg(qun, data.msgId, "无法获取用户昵称");
        return;
    }
    new Thread(new Runnable() {
        public void run() {
            if (text.startsWith("点歌")) {
                String title = text.substring(2);
                if (!title.isEmpty()) {
                    GetNeteaseMusic(qun, title, data.msgId, avatarUrl, nickname);
                }
            }
            if (text.startsWith("播歌")) {
                String title = text.substring(2);
                if (!title.isEmpty()) {
                    GetMGMusic(qun, title, data.msgId, avatarUrl, nickname);
                }
            }
        }
    }).start();
}

public void GetNeteaseMusic(String qun, String song, long msgId, String avatarUrl, String nickname) {
    try {
        String url = "https://api.jkyai.top/API/wyyyjs.php?msg=" + URLEncoder.encode(song, StandardCharsets.UTF_8.name());
        Request.Builder builder = new Request.Builder().url(url).get();
        Response response = client.newCall(builder.build()).execute();
        if (response.code() != 200) {
            sendQuoteMsg(qun, msgId, "API 请求失败，状态码: " + response.code());
            response.close();
            return;
        }
        String jsonData = response.body().string();
        response.close();
        if (jsonData == null || jsonData.trim().startsWith("<")) {
            sendQuoteMsg(qun, msgId, "API 返回非 JSON 数据，可能服务不可用");
            return;
        }
        JSONObject json = JSON.parseObject(jsonData);
        Integer code = json.getInteger("code");
        if (code == null || code != 200) {
            sendQuoteMsg(qun, msgId, "未搜到歌曲");
            return;
        }
        JSONObject data = json.getJSONObject("data");
        JSONObject basicInfo = data.getJSONObject("basic_info");
        String name = basicInfo.getString("title");
        JSONObject media = data.getJSONObject("media");
        String audioUrl = media.getString("audio_url");
        String lyric = data.getString("lyrics");
        if (lyric == null || lyric.isEmpty()) {
            lyric = "[99:99.99]暂无歌词";
        }
        sendMusicMsg(qun, name, nickname, audioUrl, lyric, avatarUrl);
    } catch (Exception e) {
        sendQuoteMsg(qun, msgId, "处理时: " + e.getMessage());
    }
}

public void GetMGMusic(String qun, String song, long msgId, String avatarUrl, String nickname) {
    try {
        Map header = new HashMap();
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");
        header.put("Referer", "https://m.music.migu.cn/v4/");
        header.put("Host", "m.music.migu.cn");
        header.put("channel", "014000D");
        header.put("Cookie", "SESSION=ZTIwODkyMDQtOTE1NS00MDhlLThhMWEtMjQ0N2Y2Mzk2OTAz");
        header.put("By", "3c0effb5be907dd7fc209a9001a18713");
        String jsonData = get("https://m.music.migu.cn/migumusic/h5/search/all?text=" + song + "&pageSize=1", header);
        JSONObject json = JSON.parseObject(jsonData);
        Integer code = json.getInteger("code");
        JSONObject data = json.getJSONObject("data");
        JSONObject songsData = data.getJSONObject("songsData");
        JSONArray resultList = songsData.getJSONArray("items");
        if (resultList == null || resultList.size() == 0) {
            sendQuoteMsg(qun, msgId, "未搜到");
            return;
        }
        JSONObject jsonObject = resultList.getJSONObject(0);
        String name = jsonObject.getString("name");
        String id = jsonObject.getString("id");
        String albumId = jsonObject.getJSONObject("album").getString("id");
        String jsonp = get("https://app.c.nf.migu.cn/MIGUM2.0/strategy/listen-url/v2.4?albumId=" + albumId + "&lowerQualityContentId=&netType=00&resourceType=2&songId=" + id + "&toneFlag=PQ", header);
        JSONObject jsonObject2 = JSON.parseObject(jsonp);
        JSONObject data2 = jsonObject2.getJSONObject("data");
        String url = data2.getString("url");
        String lyric = data2.getString("lrcUrl");
        if (!lyric.equals("")) {
            lyric = get(lyric, null);
        }
        sendMusicMsg(qun, name, nickname, url, lyric, avatarUrl);
    } catch (Exception e) {
        sendQuoteMsg(qun, msgId, "处理时: " + e);
    }
}

public void sendMusicMsg(String talker, String title, String singer, String url, String lyric, String pic) {
    WXMusicObject music = new WXMusicObject();
    music.musicDataUrl = url;
    if (lyric == null || lyric.isEmpty()) {
        lyric = "[99:99.99]暂无歌词";
    }
    music.songLyric = lyric;
    music.songAlbumUrl = pic;
    WXMediaMessage media = new WXMediaMessage(music);
    media.title = title;
    media.description = singer;
    sendMediaMsg(talker, media, "wx485a97c844086dc9");
}

public boolean isFiltered(Object data) {
    boolean switchFlag = getInt(data.talker, "开关", 0) == 1;
    boolean commonCondition = isHalfMinute(data.createTime) || data.isSystem();
    return switchFlag ? commonCondition : commonCondition || !data.isSend();
}

public boolean isHalfMinute(long createTime) {
    long currentTime = System.currentTimeMillis();
    long timeDifference = currentTime - createTime;
    return timeDifference >= 30 * 1000;
}

private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(30))
    .callTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(30))
    .build();

private void addHeaders(Request.Builder builder, Map header) {
    if (header != null) {
        for (Map.Entry entry: header.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
    }
}

private String executeRequest(Request.Builder builder) {
    try {
        Response response = client.newCall(builder.build()).execute();
        return response.body().string();
    } catch (IOException e) {
        return null;
    }
}

public String get(String url, Map header) {
    Request.Builder builder = new Request.Builder().url(url).get();
    addHeaders(builder, header);
    return executeRequest(builder);
}

public String post(String url, String data, Map header) {
    String mediaType = (header != null && header.containsKey("Content-Type")) ?
        header.get("Content-Type").toString() : "application/json";
    RequestBody body = RequestBody.create(MediaType.parse(mediaType), data);
    Request.Builder builder = new Request.Builder().url(url).post(body);
    addHeaders(builder, header);
    return executeRequest(builder);
}

private static final String JavaPath = pluginDir.replace("Plugin/", "");

public void NewFile(String Path) {
    File file = new File(Path);
    if (!file.exists()) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {}
    }
}

public String Read(String Path) {
    String Text = "";
    try {
        File file = new File(Path);
        if (!file.exists()) {
            return null;
        }
        BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String str;
        while ((str = bf.readLine()) != null) {
            Text += "\n" + str;
        }
        bf.close();
        if (Text.isEmpty()) {
            return null;
        }
        return Text.substring(1);
    } catch (IOException ioe) {
        return null;
    }
}

public void Write(String Path, String WriteData) {
    try {
        File file = new File(Path);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        osw.write(WriteData);
        osw.flush();
        osw.close();
    } catch (IOException e) {}
}

public void putString(String setName, String itemName, String value) {
    try {
        String filePath = JavaPath + "/data/" + pluginId + "/" + setName;
        NewFile(filePath);
        String userData = Read(filePath);
        JSONObject json = JSONObject.parseObject(userData == null ? "{}" : userData);
        json.put(itemName, value);
        Write(filePath, JSON.toJSONString(json));
    } catch (Exception e) {}
}

public String getString(String setName, String itemName, String defaultValue) {
    try {
        String filePath = JavaPath + "/data/" + pluginId + "/" + setName;
        String userData = Read(filePath);
        if (userData == null) return defaultValue;
        JSONObject userDataJson = JSONObject.parseObject(userData);
        return userDataJson.getString(itemName) != null ? userDataJson.getString(itemName) : defaultValue;
    } catch (Exception e) {
        return defaultValue;
    }
}

public void putInt(String setName, String itemName, int value) {
    try {
        String filePath = JavaPath + "/data/" + pluginId + "/" + setName;
        NewFile(filePath);
        String userData = Read(filePath);
        JSONObject json = JSONObject.parseObject(userData == null ? "{}" : userData);
        json.put(itemName, value);
        Write(filePath, JSON.toJSONString(json));
    } catch (Exception e) {}
}

public int getInt(String setName, String itemName, int defaultValue) {
    try {
        String filePath = JavaPath + "/data/" + pluginId + "/" + setName;
        String userData = Read(filePath);
        if (userData == null) return defaultValue;
        JSONObject userDataJson = JSONObject.parseObject(userData);
        return userDataJson.getInteger(itemName) != null ? userDataJson.getInteger(itemName) : defaultValue;
    } catch (Exception e) {
        return defaultValue;
    }
}