import java.net.*;
import java.io.*;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton;
import java.util.*;
import android.text.*;
import java.util.Locale;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import me.hd.wauxv.plugin.api.callback.PluginCallBack;

class Tool {
	public static boolean decryptFile(String inPath, String outPath) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			byte[] keyBytes = Tool.fix16("qDu4ugIvyqVCamME");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));

			File in = new File(inPath);
			File out = new File(outPath);
			if (out.exists()) out.delete();

			fis = new FileInputStream(in);
			fos = new FileOutputStream(out);

			long skipped = fis.skip(70);
			if (skipped != 70) return false;

			long totalLen = in.length() - 70;
			byte[] all = new byte[(int) totalLen];
			int read = 0, n;
			while (read < all.length && (n = fis.read(all, read, all.length - read)) > 0) {
				read += n;
			}
			if (read != all.length) return false;
			byte[] cipherBytes = new byte[all.length - 4];
			System.arraycopy(all, 0, cipherBytes, 0, cipherBytes.length);

			byte[] plain = cipher.doFinal(cipherBytes);
			fos.write(plain);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			Tool.close(fis);
			Tool.close(fos);
		}
	}

	public static byte[] fix16(String s) throws Exception {
		byte[] src = s.getBytes("UTF-8");
		byte[] dst = new byte[16];
		System.arraycopy(src, 0, dst, 0, Math.min(src.length, 16));
		return dst;
	}

	public static void close(Closeable c) {
		if (c != null)
			try {
				c.close();
			} catch (IOException e) {}
	}
}


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;

JSONObject searchRequest(String keyword) {
	JSONObject headerObj = new JSONObject();
	headerObj.put("iUid", "0");
	headerObj.put("iSid", "0");
	headerObj.put("iCv", 671154790);
	headerObj.put("sPhoneType", "Android");
	headerObj.put("sCountry", "CN");
	headerObj.put("sLang", "zh_CN");
	headerObj.put("iWmid", "0");
	headerObj.put("iChid", "000");
	headerObj.put("sOpenUdid", "0");
	headerObj.put("iMcc", "0");
	headerObj.put("iMnc", "0");
	headerObj.put("sBackendCountry", "hk");
	headerObj.put("iUserType", 2);
	headerObj.put("sOsVer", "33");
	headerObj.put("iNetType", 1);
	headerObj.put("iMlid", "0");
	headerObj.put("iVip", 0);
	headerObj.put("iVvip", 0);
	headerObj.put("iAppStoreChannel", 0);
	headerObj.put("iTerminalType", 1);
	headerObj.put("sAppid", "1000716");
	JSONObject jsonRequest = new JSONObject();
	jsonRequest.put("header", headerObj);
	jsonRequest.put("type", 0);
	jsonRequest.put("keyword", keyword);
	jsonRequest.put("keyword_source", 0);
	jsonRequest.put("search_id", "0");
	jsonRequest.put("sin", 0);
	jsonRequest.put("ein", 9);
	jsonRequest.put("nqc_flag", 0);
	return jsonRequest;
}

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

String getText(String filePath, String tag) {
	try {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(false);
		Document doc = f.newDocumentBuilder().parse(new File(filePath));
		NodeList list = doc.getElementsByTagName(tag);
		return list.getLength() > 0 ? list.item(0).getTextContent() : null;
	} catch (Exception e) {
		return null;
	}
}

HashMap winterMap = new HashMap();

class WinterBen {
	public static JSONArray jsonArray;
	public static long time;
}

// ==================== DY点歌插件UI配置 ====================
String C_BG_ROOT, C_TEXT_PRIMARY, C_TEXT_SECONDARY, C_CARD_BG, C_CARD_STROKE;
String C_EDIT_BG, C_EDIT_STROKE, C_DIVIDER, C_ACCENT_START, C_ACCENT_END;
String C_BUTTON_TEXT, C_HINT_TEXT;

// 触发指令存储键
String DYMUSIC_TRIGGER1_KEY = "dymusic_trigger1";
String DYMUSIC_TRIGGER2_KEY = "dymusic_trigger2";
String DYMUSIC_VOICE_SWITCH_KEY = "dymusic_voice_switch";
String DYMUSIC_PLAYLIST_SWITCH_KEY = "dymusic_playlist_switch";  // 歌单选择开关
String DYMUSIC_SEGMENT_TYPE_KEY = "dymusic_segment_type";  // 分段类型
String DYMUSIC_OTHER_SWITCH_KEY = "dymusic_other_switch";  // 他人使用开关
String DEFAULT_TRIGGER1 = "抖音点歌";
String DEFAULT_TRIGGER2 = "来一首";
boolean DEFAULT_VOICE_SWITCH = false;  // 默认关闭语音
boolean DEFAULT_PLAYLIST_SWITCH = true;  // 默认开启歌单选择
boolean DEFAULT_OTHER_SWITCH = false;  // 默认不允许他人使用
String DEFAULT_SEGMENT_TYPE = "qb";  // 默认分段类型

// 获取配置的方法
String getTrigger1() {
    return getString(DYMUSIC_TRIGGER1_KEY, DEFAULT_TRIGGER1);
}

String getTrigger2() {
    return getString(DYMUSIC_TRIGGER2_KEY, DEFAULT_TRIGGER2);
}

boolean getVoiceSwitch() {
    return getBoolean(DYMUSIC_VOICE_SWITCH_KEY, DEFAULT_VOICE_SWITCH);
}

boolean getPlaylistSwitch() {
    return getBoolean(DYMUSIC_PLAYLIST_SWITCH_KEY, DEFAULT_PLAYLIST_SWITCH);
}

boolean getOtherSwitch() {
    return getBoolean(DYMUSIC_OTHER_SWITCH_KEY, DEFAULT_OTHER_SWITCH);
}

String getSegmentType() {
    return getString(DYMUSIC_SEGMENT_TYPE_KEY, DEFAULT_SEGMENT_TYPE);
}

// 应用主题
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

void applyDYMusicTheme() {
    boolean dark = isDarkMode();
    if (dark) {
        C_BG_ROOT = "#121212";
        C_TEXT_PRIMARY = "#E8F5E9";
        C_TEXT_SECONDARY = "#B0BEC5";
        C_CARD_BG = "#1E1E1E";
        C_CARD_STROKE = "#37474F";
        C_EDIT_BG = "#2D2D2D";
        C_EDIT_STROKE = "#455A64";
        C_DIVIDER = "#333333";
        C_ACCENT_START = "#4CAF50";
        C_ACCENT_END = "#00BCD4";
        C_BUTTON_TEXT = "#FFFFFF";
        C_HINT_TEXT = "#888888";
    } else {
        C_BG_ROOT = "#F5FDFF";
        C_TEXT_PRIMARY = "#00695C";
        C_TEXT_SECONDARY = "#546E7A";
        C_CARD_BG = "#FFFFFF";
        C_CARD_STROKE = "#B2DFDB";
        C_EDIT_BG = "#FFFFFF";
        C_EDIT_STROKE = "#80DEEA";
        C_DIVIDER = "#E0F2F1";
        C_ACCENT_START = "#00BFA5";
        C_ACCENT_END = "#2979FF";
        C_BUTTON_TEXT = "#FFFFFF";
        C_HINT_TEXT = "#78909C";
    }
}

int dp(int v) {
    try {
        Activity a = getTopActivity();
        float d = a.getResources().getDisplayMetrics().density;
        return (int)(v * d + 0.5f);
    } catch (Throwable e) { return v; }
}

GradientDrawable qqMusicShape(String color, int radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(Color.parseColor(color));
    g.setCornerRadius(dp(radius));
    return g;
}

GradientDrawable qqMusicShapeStroke(String fill, int radius, String stroke) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(Color.parseColor(fill));
    g.setCornerRadius(dp(radius));
    g.setStroke(dp(1), Color.parseColor(stroke));
    return g;
}

GradientDrawable qqMusicGradientBtn() {
    int[] colors = {Color.parseColor(C_ACCENT_START), Color.parseColor(C_ACCENT_END)};
    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    gd.setCornerRadius(dp(8));
    return gd;
}

void styleDYMusicTextPrimary(TextView tv) {
    tv.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
}

void styleDYMusicTextSecondary(TextView tv) {
    tv.setTextColor(Color.parseColor(C_TEXT_SECONDARY));
}

void onHandleMsg(Object data) {
    String text = data.content;
    String qun = data.talker;
    String wxid = data.sendTalker;
    String loginWxid = getLoginWxid();
    
    // 检查是否是本人
    boolean isMe = loginWxid != null && loginWxid.equals(wxid);
    
    // 如果不是本人，检查他人使用开关
    if (!isMe) {
        boolean allowOthers = getOtherSwitch();
        if (!allowOthers) {
            return;  // 他人使用开关关闭，不处理
        }
    }
    
    // 获取配置的触发指令
    String trigger1 = getTrigger1();
    String trigger2 = getTrigger2();
    
    // 检查触发指令
    if (text.startsWith(trigger1)) {
        String song = text.substring(trigger1.length()).trim();
        getJxMusicSong(qun, wxid, song);
    } else if (text.startsWith(trigger2)) {
        String song = text.substring(trigger2.length()).trim();
        getJxMusicSong(qun, wxid, song);
    }
    
    // 后续的数字回复逻辑
    if (text.matches("^\\d+")) {
        String key = qun + "_" + wxid;
        if (winterMap.containsKey(key)) {
            WinterBen winterBen = winterMap.get(key);
            
            // 检查是否在有效时间内
            if (winterBen.time + 600 * 1000 > System.currentTimeMillis()) {
                JSONArray jsonArr = winterBen.jsonArray;
                int page = Integer.parseInt(text);
                
                if (page <= 0 || page > jsonArr.size()) {
                    sendText(qun, "序号超出范围，请输入1-" + jsonArr.size());
                    return;
                }
                
                JSONObject dataObj = jsonArr.getJSONObject(page - 1);
                
                // 立即清除状态，防止重复选择
                winterMap.remove(key);
                
                // 点歌
                getJxMusicUrl(qun, wxid, dataObj);
                
            } else {
                // 超时，清除状态
                winterMap.remove(key);
                sendText(qun, "选择已超时，请重新搜索");
            }
        }
    }
}



import java.util.Base64;

String b64Decrypt(String encStr) {
	byte[] decBytes = Base64.getDecoder().decode(encStr);
	String decStr = new String(decBytes);
	return decStr;
}

String buildXml(String qun, String desc) {
	return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<msg><appmsg>" +
		"<title>歌曲列表</title>" +
		"<des>点击查看</des>" +
		"<action>view</action>" +
		"<type>19</type>" +
		"<recorditem>" +
		"<![CDATA[<recordinfo>" +
		"<title>我想陪你走的何止是路</title>" +
		"<desc>点击查看</desc>" +
		"<datalist count=\"2\">" +
		"<dataitem datatype=\"1\" dataid=\"1\" datasourceid=\"\">" +
		"<datadesc>" + desc + "</datadesc>" +
		"<sourcename>" + getFriendName(getLoginWxid(), qun) + "</sourcename>" +
		"<sourceheadurl>" + getAvatarUrl(getLoginWxid()) + "</sourceheadurl>" +
		"<sourcetime>浪漫至死不渝</sourcetime>" +
		"</dataitem>" +
		"</datalist>" +
		"</recordinfo>]]>" +
		"</recorditem>" +
		"</appmsg></msg>";
}

void getJxMusicSong(String qun, String wxid, String song) {
	new Thread(new Runnable() {
		public void run() {
			try {
				String url = "https://www.yx520.ltd/API/dyyy/wa.php?a=10&msg=" + song;
				String response = get(url, null);
				
				JSONObject jsonObj = JSON.parseObject(response);
				JSONArray songList = jsonObj.getJSONArray("data"); 
				
				if (songList == null || songList.size() == 0) {
					sendText(qun, "未搜到");
					return;
				}
				
				boolean playlistEnabled = getPlaylistSwitch();
				
				if (playlistEnabled) {
					// 歌单选择开启，显示完整列表
					String text = "";
					for (int i = 0; i < songList.size(); i++) {
						JSONObject dataObj = songList.getJSONObject(i);
						String singer = dataObj.getString("singer");
						String songName = dataObj.getString("song");
						text += (i + 1) + ". " + songName + "--" + singer + "\n";
					}
					
					text += "\n请发送序号进行点歌\n十分钟之内有效";
					if (this.interpreter.get("sendXml") != null) {
						sendXml.invoke(null, qun, buildXml(qun, text));
					} else {
						sendText(qun, text);
					}
					
					WinterBen winterBen = new WinterBen();
					winterBen.jsonArray = songList;
					winterBen.time = System.currentTimeMillis();
					winterMap.put(qun + "_" + wxid, winterBen);
				} else {
					// 歌单选择关闭，直接发送第一首歌
					if (songList.size() > 0) {
						JSONObject firstSong = songList.getJSONObject(0);
						getJxMusicUrl(qun, wxid, firstSong);
					}
				}
			} catch (Exception e) {
				sendText(qun, "搜索时出错: " + e);
			}
		}
	}).start();
}

import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;

void sendMusicMsg(String talker, String title, String singer, String url, String lyric, String album) {
	WXMusicObject music = new WXMusicObject();
	music.musicDataUrl = url;
	if (lyric == null || lyric.isEmpty()) {
		lyric = "[99:99.99]暂无歌词";
	}
	music.songLyric = lyric;
	music.songAlbumUrl = album;
	WXMediaMessage media = new WXMediaMessage(music);
	media.title = title;
	media.description = singer;
	sendMediaMsg(talker, media, "wx485a97c844086dc9");
}

void getJxMusicUrl(String qun, String wxid, JSONObject dataObj) {
	new Thread(new Runnable() {
		public void run() {
			try {
				String singer = dataObj.getString("singer");
				String song = dataObj.getString("song");
				String url = dataObj.getString("url");
				String album = dataObj.getString("cover");
				String lyric = dataObj.getString("lyric");
				
				if (url != null && !url.isEmpty()) {
					// 检查语音开关
					boolean voiceEnabled = getVoiceSwitch();
					
					if (voiceEnabled) {
						// 语音开关开启，只发送语音
						processVoiceOnly(qun, url, singer, song);
					} else {
						// 语音开关关闭，只发送音乐消息
						sendMusicMsg(qun, song, singer, url, lyric, album);
					}
				} else {
					sendText(qun, "获取音乐链接失败，请重试。");
				}
				
			} catch (Exception e) {
				sendText(qun, "发送音乐消息时出错: " + e);
			}
		}
	}).start();
}

// 只处理语音的功能
void processVoiceOnly(String qun, String musicUrl, String singer, String song) {
	try {
		// 对URL进行编码
		String encodedUrl = URLEncoder.encode(musicUrl, "UTF-8");
		// 获取分段类型
		String segmentType = getSegmentType();
		// 使用您的接口，根据配置使用不同的fd参数
		String apiUrl = "https://109a.cn/API/silk/apicj.php?cj=59.9&fd=" + segmentType + "&url=" + encodedUrl;
		
		get(apiUrl, null, new PluginCallBack.HttpCallback() {
			public void onSuccess(int code, String response) {
				try {
					JSONObject jsonResp = JSON.parseObject(response);
					int respCode = jsonResp.getIntValue("code");
					
					if (respCode == 1) {
						// 获取segments数组
						JSONArray segments = jsonResp.getJSONArray("segments");
						if (segments != null && segments.size() > 0) {
							int totalSegments = segments.size();
							
							// 使用插件目录作为缓存目录
							String cacheDir = pluginDir + "/DYMusic/cache/";
							File cacheFolder = new File(cacheDir);
							if (!cacheFolder.exists()) {
								cacheFolder.mkdirs();
							}
							
							// 处理所有语音片段
							processSegmentsSequentially(qun, segments, cacheDir, 0);
						} else {
							sendText(qun, "❌ 未获取到语音片段");
						}
					} else {
						String errorMsg = jsonResp.getString("message");
						sendText(qun, "❌ 语音转换失败: " + errorMsg);
					}
				} catch (Exception e) {
					sendText(qun, "❌ 解析语音API响应失败: " + e.getMessage());
				}
			}
			
			public void onError(Exception e) {
				sendText(qun, "❌ 语音API请求失败: " + e.getMessage());
			}
		});
		
	} catch (Exception e) {
		sendText(qun, "❌ 语音处理异常: " + e.getMessage());
	}
}

// 递归处理所有语音片段
void processSegmentsSequentially(String qun, JSONArray segments, String cacheDir, int index) {
	if (index >= segments.size()) {
		return;
	}
	
	try {
		JSONObject segment = segments.getJSONObject(index);
		String silkUrl = segment.getString("url");
		int segmentNumber = segment.getIntValue("segment");
		
		// 生成文件名
		String fileName = "voice_" + System.currentTimeMillis() + "_" + index + ".silk";
		String audioPath = cacheDir + fileName;
		
		// 下载当前语音片段
		download(silkUrl, audioPath, null, new PluginCallBack.DownloadCallback() {
			public void onSuccess(File file) {
				try {
					if (file.exists() && file.length() > 0) {
						// 发送当前语音片段
						sendVoice(qun, audioPath);
						
						// 延迟删除临时文件
						new Thread(new Runnable() {
							public void run() {
								try {
									Thread.sleep(2000);
									if (file.delete()) {
									}
								} catch (Exception e) {}
							}
						}).start();
						
						// 延迟一段时间后处理下一个片段
						new Thread(new Runnable() {
							public void run() {
								try {
									// 等待3秒再处理下一个片段
									Thread.sleep(2000);
									processSegmentsSequentially(qun, segments, cacheDir, index + 1);
								} catch (Exception e) {
								}
							}
						}).start();
					} else {
						if (file.exists()) file.delete();
						processSegmentsSequentially(qun, segments, cacheDir, index + 1);
					}
				} catch (Exception e) {
					if (file.exists()) file.delete();
					processSegmentsSequentially(qun, segments, cacheDir, index + 1);
				}
			}
			
			public void onError(Exception e) {
				processSegmentsSequentially(qun, segments, cacheDir, index + 1);
			}
		});
		
	} catch (Exception e) {
		processSegmentsSequentially(qun, segments, cacheDir, index + 1);
	}
}

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;

OkHttpClient client = new OkHttpClient.Builder()
	.connectTimeout(Duration.ofSeconds(30))
	.callTimeout(Duration.ofSeconds(30))
	.readTimeout(Duration.ofSeconds(30))
	.build();

void addHeaders(Request.Builder builder, Map header) {
	if (header != null) {
		for (Map.Entry entry: header.entrySet()) {
			builder.addHeader(entry.getKey(), entry.getValue());
		}
	}
}

String executeRequest(Request.Builder builder) {
	try {
		Response response = client.newCall(builder.build()).execute();
		return response.body().string();
	} catch (IOException e) {
		return null;
	}
}

String get(String url, Map header) {
	Request.Builder builder = new Request
		.Builder()
		.url(url)
		.get();
	addHeaders(builder, header);
	return executeRequest(builder);
}

String post(String url, String data, Map header) {
	String mediaType = (header != null &&
			header.containsKey("Content-Type")) ?
		header.get("Content-Type").toString() : "application/json";
	RequestBody body = RequestBody.create(MediaType.parse(mediaType), data);
	Request.Builder builder = new Request
		.Builder()
		.url(url)
		.post(body);
	addHeaders(builder, header);
	return executeRequest(builder);
}

boolean uploadFile(String url, File file, Map header, String savePath) {
	RequestBody body = RequestBody.create(
		MediaType.parse("application/octet-stream"),
		file);
	Request.Builder builder = new Request.Builder()
		.url(url)
		.post(body);
	addHeaders(builder, header);
	OutputStream os = null;
	try {
		Response resp = client.newCall(builder.build()).execute();
		if (!resp.isSuccessful()) return false;
		File outputFile = new File(savePath);
		File parentDir = outputFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		os = new FileOutputStream(savePath);
		os.write(resp.body().bytes());
		return true;
	} catch (IOException e) {
		return false;
	} finally {
		if (os != null) try {
			os.close();
		} catch (IOException e) {}
	}
}

// 发送按钮点击处理
boolean onClickSendBtn(String text) {
    String talker = getTargetTalker();
    if (talker == null) {
        return false;
    }
    
    // 打开抖音音乐配置界面
    if (text.equals("抖音音乐设置") || text.equals("抖音音乐设置") || 
        text.equals("抖音音乐配置") || text.equals("抖音音乐配置")) {
        showDYMusicConfigDialog();
        return true;
    }
    
    return false;
}

void showDYMusicConfigDialog() {
    Activity act = getTopActivity();
    if (act == null) {
        toast("无法打开配置界面");
        return;
    }
    
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            try {
                applyDYMusicTheme();
                final Dialog dialog = new Dialog(act);
                
                // 主布局容器
                final FrameLayout container = new FrameLayout(act);
                container.setBackgroundColor(Color.TRANSPARENT);
                
                // 跑马灯边框容器
                final FrameLayout borderContainer = new FrameLayout(act);
                borderContainer.setPadding(dp(2), dp(2), dp(2), dp(2));
                
                // 主内容布局
                LinearLayout mainLayout = new LinearLayout(act);
                mainLayout.setOrientation(LinearLayout.VERTICAL);
                mainLayout.setPadding(dp(13), dp(16), dp(13), dp(13));
                mainLayout.setBackground(qqMusicShape(C_BG_ROOT, 10));
                
                borderContainer.addView(mainLayout, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ));
                
                container.addView(borderContainer, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ));
                
                // 标题
                LinearLayout titleLayout = new LinearLayout(act);
                titleLayout.setOrientation(LinearLayout.HORIZONTAL);
                titleLayout.setGravity(Gravity.CENTER_VERTICAL);
                titleLayout.setPadding(0, 0, 0, dp(6));
                
                ImageView musicIcon = new ImageView(act);
                musicIcon.setImageResource(android.R.drawable.ic_media_play);
                musicIcon.setColorFilter(Color.parseColor(C_ACCENT_START));
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
                iconParams.setMargins(0, 0, dp(6), 0);
                titleLayout.addView(musicIcon, iconParams);
                
                TextView title = new TextView(act);
                title.setText("🎵 抖音音乐点歌设置");
                title.setTextSize(16);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                styleDYMusicTextPrimary(title);
                titleLayout.addView(title);
                
                mainLayout.addView(titleLayout);
                
                // 作者
                TextView author = new TextView(act);
                author.setText("抖音点歌插件配置中心");
                author.setTextSize(10);
                styleDYMusicTextSecondary(author);
                author.setGravity(Gravity.CENTER);
                author.setPadding(0, 0, 0, dp(10));
                mainLayout.addView(author);
                
                // 使用方法卡片
                LinearLayout usageCard = new LinearLayout(act);
                usageCard.setOrientation(LinearLayout.VERTICAL);
                usageCard.setBackground(qqMusicShapeStroke(C_CARD_BG, 8, C_CARD_STROKE));
                usageCard.setPadding(dp(10), dp(10), dp(10), dp(10));
                
                TextView usageTitle = new TextView(act);
                usageTitle.setText("📋 使用方法");
                usageTitle.setTextSize(12);
                usageTitle.setTypeface(Typeface.DEFAULT_BOLD);
                styleDYMusicTextPrimary(usageTitle);
                usageTitle.setPadding(0, 0, 0, dp(6));
                usageCard.addView(usageTitle);
                
                String[] steps = {
                    "1. 发送【指令+歌曲名】搜索歌曲",
                    "2. 歌单选择：开=列表，关=直接播放",
                    "3. 语音开关：开=只语音，关=只音乐",
                    "4. 分段类型：qb=全曲，数字=分段数",
                    "5. 他人使用：开=允许群友，关=仅自己"
                };
                
                for (int i = 0; i < steps.length; i++) {
                    TextView stepText = new TextView(act);
                    stepText.setText(steps[i]);
                    stepText.setTextSize(10);
                    styleDYMusicTextSecondary(stepText);
                    stepText.setPadding(0, dp(3), 0, dp(3));
                    usageCard.addView(stepText);
                }
                
                mainLayout.addView(usageCard);
                ((LinearLayout.LayoutParams)usageCard.getLayoutParams()).bottomMargin = dp(10);
                
                // 功能开关卡片
                LinearLayout switchCard = new LinearLayout(act);
                switchCard.setOrientation(LinearLayout.VERTICAL);
                switchCard.setBackground(qqMusicShapeStroke(C_CARD_BG, 8, C_CARD_STROKE));
                switchCard.setPadding(dp(10), dp(10), dp(10), dp(10));
                
                TextView switchTitle = new TextView(act);
                switchTitle.setText("⚙️ 功能开关");
                switchTitle.setTextSize(12);
                switchTitle.setTypeface(Typeface.DEFAULT_BOLD);
                styleDYMusicTextPrimary(switchTitle);
                switchTitle.setPadding(0, 0, 0, dp(6));
                switchCard.addView(switchTitle);
                
                // 他人使用开关
                LinearLayout otherSwitchLayout = new LinearLayout(act);
                otherSwitchLayout.setOrientation(LinearLayout.HORIZONTAL);
                otherSwitchLayout.setGravity(Gravity.CENTER_VERTICAL);
                otherSwitchLayout.setPadding(0, dp(3), 0, dp(3));
                
                ImageView otherIcon = new ImageView(act);
                otherIcon.setImageResource(android.R.drawable.ic_lock_power_off);
                otherIcon.setColorFilter(Color.parseColor(C_ACCENT_START));
                LinearLayout.LayoutParams otherIconParams = new LinearLayout.LayoutParams(dp(16), dp(16));
                otherIconParams.setMargins(0, 0, dp(6), 0);
                otherSwitchLayout.addView(otherIcon, otherIconParams);
                
                TextView otherLabel = new TextView(act);
                otherLabel.setText("他人使用");
                otherLabel.setTextSize(10);
                styleDYMusicTextPrimary(otherLabel);
                otherLabel.setPadding(0, 0, dp(6), 0);
                otherSwitchLayout.addView(otherLabel);
                
                final Switch otherSwitch = new Switch(act);
                otherSwitch.setChecked(getOtherSwitch());
                otherSwitch.setTextSize(10);
                otherSwitch.setPadding(dp(6), 0, 0, 0);
                LinearLayout.LayoutParams otherSwitchParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                otherSwitchParams.gravity = Gravity.RIGHT;
                otherSwitchLayout.addView(otherSwitch, otherSwitchParams);
                
                switchCard.addView(otherSwitchLayout);
                
                // 歌单选择开关
                LinearLayout playlistSwitchLayout = new LinearLayout(act);
                playlistSwitchLayout.setOrientation(LinearLayout.HORIZONTAL);
                playlistSwitchLayout.setGravity(Gravity.CENTER_VERTICAL);
                playlistSwitchLayout.setPadding(0, dp(6), 0, dp(3));
                
                ImageView playlistIcon = new ImageView(act);
                playlistIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                playlistIcon.setColorFilter(Color.parseColor(C_ACCENT_START));
                LinearLayout.LayoutParams playlistIconParams = new LinearLayout.LayoutParams(dp(16), dp(16));
                playlistIconParams.setMargins(0, 0, dp(6), 0);
                playlistSwitchLayout.addView(playlistIcon, playlistIconParams);
                
                TextView playlistLabel = new TextView(act);
                playlistLabel.setText("歌单选择");
                playlistLabel.setTextSize(10);
                styleDYMusicTextPrimary(playlistLabel);
                playlistLabel.setPadding(0, 0, dp(6), 0);
                playlistSwitchLayout.addView(playlistLabel);
                
                final Switch playlistSwitch = new Switch(act);
                playlistSwitch.setChecked(getPlaylistSwitch());
                playlistSwitch.setTextSize(10);
                playlistSwitch.setPadding(dp(6), 0, 0, 0);
                LinearLayout.LayoutParams playlistSwitchParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                playlistSwitchParams.gravity = Gravity.RIGHT;
                playlistSwitchLayout.addView(playlistSwitch, playlistSwitchParams);
                
                switchCard.addView(playlistSwitchLayout);
                
                // 语音开关布局
                LinearLayout voiceSwitchLayout = new LinearLayout(act);
                voiceSwitchLayout.setOrientation(LinearLayout.HORIZONTAL);
                voiceSwitchLayout.setGravity(Gravity.CENTER_VERTICAL);
                voiceSwitchLayout.setPadding(0, dp(6), 0, dp(3));
                
                ImageView voiceIcon = new ImageView(act);
                voiceIcon.setImageResource(android.R.drawable.ic_btn_speak_now);
                voiceIcon.setColorFilter(Color.parseColor(C_ACCENT_START));
                LinearLayout.LayoutParams voiceIconParams = new LinearLayout.LayoutParams(dp(16), dp(16));
                voiceIconParams.setMargins(0, 0, dp(6), 0);
                voiceSwitchLayout.addView(voiceIcon, voiceIconParams);
                
                TextView voiceLabel = new TextView(act);
                voiceLabel.setText("语音发送");
                voiceLabel.setTextSize(10);
                styleDYMusicTextPrimary(voiceLabel);
                voiceLabel.setPadding(0, 0, dp(6), 0);
                voiceSwitchLayout.addView(voiceLabel);
                
                final Switch voiceSwitch = new Switch(act);
                voiceSwitch.setChecked(getVoiceSwitch());
                voiceSwitch.setTextSize(10);
                voiceSwitch.setPadding(dp(6), 0, 0, 0);
                LinearLayout.LayoutParams voiceSwitchParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                voiceSwitchParams.gravity = Gravity.RIGHT;
                voiceSwitchLayout.addView(voiceSwitch, voiceSwitchParams);
                
                switchCard.addView(voiceSwitchLayout);
                
                // 功能说明
                TextView funcDesc = new TextView(act);
                funcDesc.setText("💡 他人：开=允许群友使用，关=仅自己\n💡 歌单：开=列表选择，关=直接播放\n💡 语音：开=只语音，关=只音乐");
                funcDesc.setTextSize(9);
                styleDYMusicTextSecondary(funcDesc);
                funcDesc.setPadding(0, dp(3), 0, 0);
                switchCard.addView(funcDesc);
                
                mainLayout.addView(switchCard);
                ((LinearLayout.LayoutParams)switchCard.getLayoutParams()).bottomMargin = dp(10);
                
                // 分段类型卡片
                LinearLayout segmentCard = new LinearLayout(act);
                segmentCard.setOrientation(LinearLayout.VERTICAL);
                segmentCard.setBackground(qqMusicShapeStroke(C_CARD_BG, 8, C_CARD_STROKE));
                segmentCard.setPadding(dp(10), dp(10), dp(10), dp(10));
                
                TextView segmentTitle = new TextView(act);
                segmentTitle.setText("🔧 分段设置");
                segmentTitle.setTextSize(12);
                segmentTitle.setTypeface(Typeface.DEFAULT_BOLD);
                styleDYMusicTextPrimary(segmentTitle);
                segmentTitle.setPadding(0, 0, 0, dp(6));
                segmentCard.addView(segmentTitle);
                
                // 分段类型输入框
                TextView segmentLabel = new TextView(act);
                segmentLabel.setText("分段类型：");
                segmentLabel.setTextSize(10);
                styleDYMusicTextPrimary(segmentLabel);
                segmentLabel.setPadding(0, 0, 0, dp(3));
                segmentCard.addView(segmentLabel);
                
                final EditText segmentInput = new EditText(act);
                segmentInput.setHint("请输入：qb, 1, 2 或 3");
                segmentInput.setText(getSegmentType());
                segmentInput.setHintTextColor(Color.parseColor(C_HINT_TEXT));
                segmentInput.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
                segmentInput.setBackground(qqMusicShapeStroke(C_EDIT_BG, 6, C_EDIT_STROKE));
                segmentInput.setPadding(dp(10), dp(6), dp(10), dp(6));
                segmentInput.setTextSize(10);
                segmentInput.setInputType(InputType.TYPE_CLASS_TEXT);
                segmentInput.setSingleLine(true);
                
                // 添加输入过滤器
                InputFilter filter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start, int end, 
                                               Spanned dest, int dstart, int dend) {
                        String input = source.toString().toLowerCase();
                        if (input.equals("q") || input.equals("b") || input.equals("qb") || 
                            input.equals("1") || input.equals("2") || input.equals("3")) {
                            return source;
                        }
                        return "";
                    }
                };
                segmentInput.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(2)});
                
                segmentCard.addView(segmentInput);
                ((LinearLayout.LayoutParams)segmentInput.getLayoutParams()).bottomMargin = dp(6);
                
                // 分段说明
                TextView segmentDesc = new TextView(act);
                segmentDesc.setText("💡 允许输入：qb(全曲), 1(1段), 2(2段), 3(3段)");
                segmentDesc.setTextSize(9);
                styleDYMusicTextSecondary(segmentDesc);
                segmentDesc.setPadding(0, dp(3), 0, 0);
                segmentCard.addView(segmentDesc);
                
                mainLayout.addView(segmentCard);
                ((LinearLayout.LayoutParams)segmentCard.getLayoutParams()).bottomMargin = dp(10);
                                
                // 指令设置卡片
                LinearLayout commandCard = new LinearLayout(act);
                commandCard.setOrientation(LinearLayout.VERTICAL);
                commandCard.setBackground(qqMusicShapeStroke(C_CARD_BG, 8, C_CARD_STROKE));
                commandCard.setPadding(dp(10), dp(10), dp(10), dp(10));
                
                TextView commandTitle = new TextView(act);
                commandTitle.setText("🔤 指令设置");
                commandTitle.setTextSize(12);
                commandTitle.setTypeface(Typeface.DEFAULT_BOLD);
                styleDYMusicTextPrimary(commandTitle);
                commandTitle.setPadding(0, 0, 0, dp(6));
                commandCard.addView(commandTitle);
                
                // 指令1
                TextView cmd1Label = new TextView(act);
                cmd1Label.setText("主指令：");
                cmd1Label.setTextSize(10);
                styleDYMusicTextPrimary(cmd1Label);
                cmd1Label.setPadding(0, 0, 0, dp(3));
                commandCard.addView(cmd1Label);
                
                final EditText cmd1Input = new EditText(act);
                cmd1Input.setHint("例如：抖音点歌");
                cmd1Input.setText(getTrigger1());
                cmd1Input.setHintTextColor(Color.parseColor(C_HINT_TEXT));
                cmd1Input.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
                cmd1Input.setBackground(qqMusicShapeStroke(C_EDIT_BG, 6, C_EDIT_STROKE));
                cmd1Input.setPadding(dp(10), dp(6), dp(10), dp(6));
                cmd1Input.setTextSize(10);
                commandCard.addView(cmd1Input);
                ((LinearLayout.LayoutParams)cmd1Input.getLayoutParams()).bottomMargin = dp(6);
                
                // 指令2
                TextView cmd2Label = new TextView(act);
                cmd2Label.setText("副指令：");
                cmd2Label.setTextSize(10);
                styleDYMusicTextPrimary(cmd2Label);
                cmd2Label.setPadding(0, 0, 0, dp(3));
                commandCard.addView(cmd2Label);
                
                final EditText cmd2Input = new EditText(act);
                cmd2Input.setHint("例如：来一首");
                cmd2Input.setText(getTrigger2());
                cmd2Input.setHintTextColor(Color.parseColor(C_HINT_TEXT));
                cmd2Input.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
                cmd2Input.setBackground(qqMusicShapeStroke(C_EDIT_BG, 6, C_EDIT_STROKE));
                cmd2Input.setPadding(dp(10), dp(6), dp(10), dp(6));
                cmd2Input.setTextSize(10);
                commandCard.addView(cmd2Input);
                ((LinearLayout.LayoutParams)cmd2Input.getLayoutParams()).bottomMargin = dp(6);
                
                // 提示文本
                TextView cmdTip = new TextView(act);
                cmdTip.setText("💡 设置后立即生效，可设置不同指令");
                cmdTip.setTextSize(9);
                styleDYMusicTextSecondary(cmdTip);
                cmdTip.setPadding(0, dp(3), 0, 0);
                commandCard.addView(cmdTip);
                
                mainLayout.addView(commandCard);
                ((LinearLayout.LayoutParams)commandCard.getLayoutParams()).bottomMargin = dp(10);
                
                // 当前状态
                final TextView statusText = new TextView(act);
                boolean voiceOn = getVoiceSwitch();
                boolean playlistOn = getPlaylistSwitch();
                boolean otherOn = getOtherSwitch();
                String statusMsg = "指令：" + getTrigger1() + "/" + getTrigger2() + 
                                 " | 他人：" + (otherOn ? "✅" : "⛔") +
                                 " | 歌单：" + (playlistOn ? "✅" : "⛔") +
                                 " | 语音：" + (voiceOn ? "✅" : "⛔") +
                                 " | 分段：" + getSegmentType();
                statusText.setText(statusMsg);
                statusText.setTextSize(9);
                styleDYMusicTextSecondary(statusText);
                statusText.setGravity(Gravity.CENTER);
                statusText.setPadding(0, 0, 0, dp(6));
                mainLayout.addView(statusText);
                
                // 按钮容器
                LinearLayout btnContainer = new LinearLayout(act);
                btnContainer.setOrientation(LinearLayout.HORIZONTAL);
                btnContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                btnContainer.setPadding(0, dp(3), 0, 0);
                
                // 保存按钮
                Button saveBtn = new Button(act);
                saveBtn.setText("💾 保存");
                saveBtn.setAllCaps(false);
                saveBtn.setBackground(qqMusicGradientBtn());
                saveBtn.setTextColor(Color.WHITE);
                saveBtn.setPadding(dp(20), dp(8), dp(20), dp(8));
                saveBtn.setTextSize(12);
                saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
                
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String newTrigger1 = cmd1Input.getText().toString().trim();
                        String newTrigger2 = cmd2Input.getText().toString().trim();
                        String segmentInputText = segmentInput.getText().toString().trim().toLowerCase();
                        
                        if (newTrigger1.isEmpty() || newTrigger2.isEmpty()) {
                            toast("指令不能为空");
                            return;
                        }
                        
                        if (newTrigger1.equals(newTrigger2)) {
                            toast("两条指令不能相同");
                            return;
                        }
                        
                        // 验证分段类型输入
                        if (segmentInputText.isEmpty()) {
                            toast("分段类型不能为空");
                            return;
                        }
                        
                        if (!segmentInputText.equals("qb") && !segmentInputText.equals("1") && 
                            !segmentInputText.equals("2") && !segmentInputText.equals("3")) {
                            toast("分段类型只能输入：qb, 1, 2 或 3");
                            return;
                        }
                        
                        // 保存所有设置
                        putString(DYMUSIC_TRIGGER1_KEY, newTrigger1);
                        putString(DYMUSIC_TRIGGER2_KEY, newTrigger2);
                        putBoolean(DYMUSIC_VOICE_SWITCH_KEY, voiceSwitch.isChecked());
                        putBoolean(DYMUSIC_PLAYLIST_SWITCH_KEY, playlistSwitch.isChecked());
                        putBoolean(DYMUSIC_OTHER_SWITCH_KEY, otherSwitch.isChecked());
                        putString(DYMUSIC_SEGMENT_TYPE_KEY, segmentInputText);
                        
                        toast("设置已保存");
                        dialog.dismiss();
                    }
                });
                
                LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                saveParams.setMargins(0, 0, dp(5), 0);
                btnContainer.addView(saveBtn, saveParams);
                
                // 关闭按钮
                Button closeBtn = new Button(act);
                closeBtn.setText("❌ 关闭");
                closeBtn.setAllCaps(false);
                closeBtn.setBackground(qqMusicShape(C_CARD_BG, 6));
                closeBtn.setTextColor(Color.parseColor(C_TEXT_PRIMARY));
                closeBtn.setPadding(dp(20), dp(8), dp(20), dp(8));
                closeBtn.setTextSize(12);
                
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                
                LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                btnContainer.addView(closeBtn, closeParams);
                
                mainLayout.addView(btnContainer);
                
                // 设置对话框属性
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(container);
                
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.width = Math.min(
                        (int)(act.getResources().getDisplayMetrics().widthPixels * 0.8f),
                        dp(280)
                    );
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.gravity = Gravity.CENTER;
                    window.setAttributes(lp);
                }
                
                // 创建跑马灯动画
                final Handler marqueeHandler = new Handler();
                final int[] colorIndex = {0};
                final int[] rainbowColors = {
                    Color.parseColor("#00BFA5"),
                    Color.parseColor("#2979FF"),
                    Color.parseColor("#7C4DFF"),
                    Color.parseColor("#F50057"),
                    Color.parseColor("#FF9100"),
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#64DD17"),
                    Color.parseColor("#FF4081"),
                    Color.parseColor("#536DFE"),
                    Color.parseColor("#FFD740")
                };
                
                final Runnable marqueeAnimation = new Runnable() {
                    public void run() {
                        int startIndex = colorIndex[0] % rainbowColors.length;
                        int endIndex = (colorIndex[0] + 1) % rainbowColors.length;
                        
                        GradientDrawable borderDrawable = new GradientDrawable();
                        borderDrawable.setCornerRadius(dp(10));
                        borderDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                        
                        int[] currentGradient = {
                            rainbowColors[startIndex],
                            rainbowColors[endIndex]
                        };
                        
                        borderDrawable.setColors(currentGradient);
                        borderContainer.setBackground(borderDrawable);
                        
                        colorIndex[0]++;
                        marqueeHandler.postDelayed(this, 150);
                    }
                };
                
                // 启动动画
                marqueeHandler.post(marqueeAnimation);
                
                // 对话框关闭时停止动画
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        marqueeHandler.removeCallbacks(marqueeAnimation);
                    }
                });
                
                dialog.show();
                
            } catch (Throwable e) {
                toast("打开设置界面失败: " + e.getMessage());
            }
        }
    });
}

toast("抖音点歌插件加载成功！发送【抖音音乐设置】打开配置界面");
