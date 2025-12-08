import java.io.File;
import java.net.URLEncoder;
import me.hd.wauxv.plugin.api.callback.PluginCallBack;
import org.json.JSONObject;

final String TARGET_ROOM_ID = "\u0035\u0030\u0035\u0030\u0035\u0031\u0036\u0038\u0037\u0036\u0030\u0040\u0063\u0068\u0061\u0074\u0072\u006f\u006f\u006d";
String DOUYIN_API = "https://api.yunmge.com/api/analyze?token=72d2956e463eab7bd246d72621cf20b5&url=";
String DOUYIN_HOME_API = "https://www.yx520.ltd/API/dysc/zyspqb.php?";
String CACHE_DIR = cacheDir.endsWith("/") ? cacheDir + "video/" : cacheDir + "/video/";
int MAX_RETRIES = 2;

class GroupAuth {
    private static final String AUTH_TOKEN_KEY_PREFIX = "auth_token_";
    private static final String STATUS_KEY_PREFIX = "status_";
    private static final String TIME_KEY_PREFIX = "time_";
    private static final long VALID_PERIOD = 3000;

    public static boolean verify(String groupId) {
        try {
            String authKey = AUTH_TOKEN_KEY_PREFIX + groupId;
            String statusKey = STATUS_KEY_PREFIX + groupId;
            String timeKey = TIME_KEY_PREFIX + groupId;
            String lastCheck = getString(timeKey, "");
            if (!lastCheck.isEmpty()) {
                try {
                    long lastCheckTime = Long.parseLong(lastCheck);
                    long timeDiff = System.currentTimeMillis() - lastCheckTime;
                    if (timeDiff < VALID_PERIOD) {
                        int status = getInt(statusKey, 0);
                        return status == 1;
                    }
                } catch (NumberFormatException e) {
                    toast("联系管理员授权使用");
                }
            }
            String wxid = getLoginWxid();
            if (wxid == null || wxid.isEmpty()) {
                putInt(statusKey, 0);
                putString(authKey, "");
                toast("联系管理员授权使用");
                return false;
            }
            List memberList = getGroupMemberList(groupId);
            if (memberList == null || memberList.isEmpty()) {
                putInt(statusKey, 0);
                putString(authKey, "");
                toast("联系管理员授权使用");
                return false;
            }
            boolean isMember = false;
            for (Object member : memberList) {
                String memberWxid = member != null ? member.toString() : "null";
                if (wxid.equalsIgnoreCase(memberWxid) || wxid.equals(memberWxid)) {
                    isMember = true;
                    break;
                }
            }
            putInt(statusKey, isMember ? 1 : 0);
            putString(timeKey, String.valueOf(System.currentTimeMillis()));
            if (isMember) {
                String token = generateToken(groupId);
                putString(authKey, token);
            } else {
                putString(authKey, "");
                toast("联系管理员授权使用");
            }
            return isMember;
        } catch (Exception e) {
            toast("联系管理员授权使用");
            putInt(STATUS_KEY_PREFIX + groupId, 0);
            putString(AUTH_TOKEN_KEY_PREFIX + groupId, "");
            return false;
        }
    }

    private static String generateToken(String groupId) {
        String token = String.valueOf(System.currentTimeMillis() ^ groupId.hashCode());
        return token;
    }
}

boolean onLongClickSendBtn(String text) {
    try {
        if (!GroupAuth.verify(TARGET_ROOM_ID)) {
            toast("联系管理员授权使用");
            return true;
        }
        if (text == null) {
            return false;
        }
        String talker = getTargetTalker();
        if (text.startsWith("*") && text.contains("#")) {
            String[] parts = text.split("#");
            if (parts.length < 2) {
                return true;
            }
            String douyinHomeUrl = extractDouyinUrl(parts[0]);
            if (douyinHomeUrl == null) {
                return true;
            }
            int videoIndex = 1;
            try {
                videoIndex = Integer.parseInt(parts[1].trim());
            } catch (Throwable t) {
            }
            String encodedUrl = encodeUrl(douyinHomeUrl);
            String apiUrl = DOUYIN_HOME_API + "url=" + encodedUrl + "&a=1&n=" + videoIndex;
            String fileName = generateVideoFileName("douyin_home");
            String savePath = CACHE_DIR + fileName;
            tryGetWithRetry(apiUrl, talker, savePath, 0);
            return true;
        }
        if (text.startsWith("视频号解析")) {
            String videoUrl = extractWeixinUrl(text);
            if (videoUrl == null) {
                return true;
            }
            String fileName = generateVideoFileName("weixin");
            String savePath = CACHE_DIR + fileName;
            downloadVideo(talker, videoUrl, savePath, "视频号视频");
            return true;
        }
        if (text.startsWith("解析")) {
            String douyinUrl = extractDouyinUrl(text);
            if (douyinUrl == null) {
                return true;
            }
            String encodedUrl = encodeUrl(douyinUrl);
            String apiUrl = DOUYIN_API + encodedUrl;
            String fileName = generateVideoFileName("douyin");
            String savePath = CACHE_DIR + fileName;
            tryGetWithRetry(apiUrl, talker, savePath, 0);
            return true;
        }
        return false;
    } catch (Throwable t) {
        return true;
    }
}

void tryGetWithRetry(String apiUrl, String talker, String savePath, int retryCount) {
    try {
        get(apiUrl, null, new PluginCallBack.HttpCallback() {
            public void onSuccess(int respCode, String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (!json.has("code") || json.getInt("code") != 200) {
                        if (apiUrl.contains("zyspqb.php")) {
                            toast("联系管理员授权使用");
                        } else {
                            toast("联系管理员授权使用");
                        }
                        return;
                    }
                    if (apiUrl.contains("zyspqb.php")) {
                        if (!json.has("data")) {
                            toast("联系管理员授权使用");
                            return;
                        }
                        JSONObject videoData = json.getJSONObject("data");
                        String videoUrl = videoData.getString("play_url");
                        String title = videoData.has("desc") ? videoData.getString("desc") : "无标题";
                        if (videoUrl == null || videoUrl.isEmpty()) {
                            toast("联系管理员授权使用");
                            return;
                        }
                        downloadVideo(talker, videoUrl, savePath, title);
                    } else {
                        JSONObject data = json.getJSONObject("data");
                        if (!data.has("playurl")) {
                            toast("联系管理员授权使用");
                            return;
                        }
                        String videoUrl = data.getString("playurl");
                        String title = data.has("desc") ? data.getString("desc") : "无标题";
                        if (videoUrl == null || videoUrl.isEmpty()) {
                            toast("联系管理员授权使用");
                            return;
                        }
                        downloadVideo(talker, videoUrl, savePath, title);
                    }
                } catch (Throwable t) {
                    if (apiUrl.contains("zyspqb.php")) {
                        toast("联系管理员授权使用");
                    } else {
                        toast("联系管理员授权使用");
                    }
                }
            }
            public void onError(Exception e) {
                if (retryCount < MAX_RETRIES) {
                    tryGetWithRetry(apiUrl, talker, savePath, retryCount + 1);
                } else {
                    if (apiUrl.contains("zyspqb.php")) {
                        toast("联系管理员授权使用");
                    } else {
                        toast("联系管理员授权使用");
                    }
                }
            }
        });
    } catch (Throwable t) {
        if (retryCount < MAX_RETRIES) {
            tryGetWithRetry(apiUrl, talker, savePath, retryCount + 1);
        } else {
            if (apiUrl.contains("zyspqb.php")) {
                toast("联系管理员授权使用");
            } else {
                toast("联系管理员授权使用");
            }
        }
    }
}

void downloadVideo(String talker, String videoUrl, String savePath, String title) {
    try {
        initCacheDir();
        java.util.HashMap headers = new java.util.HashMap();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Referer", "https://www.douyin.com/");
        download(videoUrl, savePath, headers, new PluginCallBack.DownloadCallback() {
            public void onSuccess(File file) {
                try {
                    if (!file.exists() || !file.canRead()) {
                        return;
                    }
                    try {
                        sendVideo(talker, file.getAbsolutePath());
                        toast("视频发送成功: " + title);
                    } catch (Throwable t) {
                        try {
                            MediaMessage media = new MediaMessage();
                            media.setPath(file.getAbsolutePath());
                            sendMediaMsg(talker, media, null);
                            toast("视频发送成功: " + title);
                        } catch (Throwable t2) {
                        }
                    }
                } catch (Throwable t) {
                }
            }
            public void onError(Exception e) {
                if (savePath.contains("douyin_home")) {
                    toast("联系管理员授权使用");
                } else if (savePath.contains("douyin")) {
                    toast("联系管理员授权使用");
                } else if (savePath.contains("weixin")) {
                    toast("联系管理员授权使用");
                }
            }
            public void onProgress(int percent) {
            }
        });
    } catch (Throwable t) {
        if (savePath.contains("douyin_home")) {
            toast("联系管理员授权使用");
        } else if (savePath.contains("douyin")) {
            toast("联系管理员授权使用");
        } else if (savePath.contains("weixin")) {
            toast("联系管理员授权使用");
        }
    }
}

String extractWeixinUrl(String text) {
    try {
        String prefix = "http://51.wxapp.tc.qq.com/";
        int start = text.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        int end = text.indexOf(" ", start);
        if (end == -1) {
            end = text.length();
        }
        String url = text.substring(start, end);
        return url;
    } catch (Throwable t) {
        return null;
    }
}

String extractDouyinUrl(String text) {
    try {
        String prefix = "https://v.douyin.com/";
        int start = text.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        int end = text.indexOf(" ", start);
        if (end == -1) {
            end = text.length();
        }
        String url = text.substring(start, end);
        return url;
    } catch (Throwable t) {
        return null;
    }
}

String encodeUrl(String text) {
    try {
        String encoded = URLEncoder.encode(text, "UTF-8");
        return encoded;
    } catch (Throwable t) {
        return text;
    }
}

String generateVideoFileName(String platform) {
    try {
        long timestamp = System.currentTimeMillis();
        String fileName = platform + "_" + timestamp + ".mp4";
        return fileName;
    } catch (Throwable t) {
        return platform + "_default.mp4";
    }
}

void initCacheDir() {
    try {
        File dir = new File(CACHE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null && files.length > 1) {
            for (int i = 1; i < files.length; i++) {
                String name = files[i].getName();
                if ((name.startsWith("douyin_") || name.startsWith("weixin_") || name.startsWith("douyin_home_")) && name.endsWith(".mp4")) {
                    files[i].delete();
                }
            }
        }
    } catch (Throwable t) {
    }
}