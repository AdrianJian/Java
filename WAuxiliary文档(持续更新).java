WAuxiliary Plugin 完整 API 文档（v1.2.6）

> 适用于 WAuxiliary v1.2.6

文档性质：官方 API + 社区验证整合版

1. 全局变量（PluginGlobal）

变量名	说明

hostContext	宿主上下文
hostVerName	宿主版本名
hostVerCode	宿主版本号
hostVerClient	宿主客户端
moduleVer	模块版本
cacheDir	缓存目录
pluginDir	插件目录
pluginId	插件标识
pluginName	插件名称
pluginAuthor	插件作者
pluginVersion	插件版本
pluginUpdateTime	插件更新时间



---

2. 回调方法（PluginCallback）

void onLoad();

插件加载时调用

void onUnLoad();

插件卸载时调用

void onHandleMsg(Object msgInfoBean);

监听收到的所有消息（包括自己发送的）

boolean onClickSendBtn(String text);

点击发送按钮前触发

返回 true：拦截消息，不发送

返回 false：正常发送


void onMemberChange(String type, String groupWxid, String userWxid, String userName);

群成员变动（add / del / invite / kick）

void onNewFriend(String wxid, String ticket, int scene);

新好友申请


---

3. 消息结构（PluginStruct）

3.1 MsgInfo（直接对 msgInfoBean 调用，禁止强转）

long getMsgId();
int getType();
long getCreateTime();
String getTalker();
String getSendTalker();
String getContent();
String getMsgSource();
List<String> getAtUserList();
boolean isAnnounceAll();
boolean isNotifyAll();
boolean isAtMe();
QuoteMsg getQuoteMsg();
PatMsg getPatMsg();
FileMsg getFileMsg();

3.2 消息类型判断

boolean isSend();
boolean isText();
boolean isImage();
boolean isVoice();
boolean isVideo();
boolean isEmoji();
boolean isLocation();
boolean isLink();
boolean isSystem();
boolean isFile();
boolean isRedBag();
boolean isTransfer();
boolean isQuote();
boolean isPat();
boolean isNote();
boolean isVideoNumberVideo();
boolean isPrivateChat();
boolean isGroupChat();
boolean isChatroom();
boolean isImChatroom();
boolean isOfficialAccount();
boolean isOpenIM();

> ⚠ 重要
请直接调用 msgInfoBean.isText() / getTalker()
禁止 instanceof / 强转，否则必崩




---

3.3 QuoteMsg

String getTitle();
String getMsgSource();
String getSendTalker();
String getDisplayName();
String getTalker();
int getType();
String getContent();

3.4 PatMsg

String getTalker();
String getFromUser();
String getPattedUser();
String getTemplate();
long getCreateTime();

3.5 FileMsg

String getTitle();
long getSize();
String getExt();
String getMd5();


---

4. 音频方法（PluginAudioMethod）

File mp3ToSilkFile(String mp3Path);
String mp3ToSilkPath(String mp3Path);
void mp3ToSilk(String mp3Path, String silkPath);

File silkToMp3File(String silkPath);
String silkToMp3Path(String silkPath);
void silkToMp3(String silkPath, String mp3Path);

void getSilkDuration(String silkPath);


---

5. 配置方法（PluginConfigMethod）

读取

String getString(String key, String defValue);
Set getStringSet(String key, Set defValue);
boolean getBoolean(String key, boolean defValue);
int getInt(String key, int defValue);
float getFloat(String key, float defValue);
long getLong(String key, long defValue);

写入

void putString(String key, String value);
void putStringSet(String key, Set value);
void putBoolean(String key, boolean value);
void putInt(String key, int value);
void putFloat(String key, float value);
void putLong(String key, long value);


---

6. 网络方法（PluginHttpMethod）

GET

void get(String url, Map<String,String> headerMap, PluginCallBack.HttpCallback callback);
void get(String url, Map<String,String> headerMap, long timeout, PluginCallBack.HttpCallback callback);

POST

void post(String url, Map<String,String> paramMap, Map<String,String> headerMap, PluginCallBack.HttpCallback callback);
void post(String url, Map<String,String> paramMap, Map<String,String> headerMap, long timeout, PluginCallBack.HttpCallback callback);

下载

void download(String url, String path, Map<String,String> headerMap, PluginCallBack.DownloadCallback callback);
void download(String url, String path, Map<String,String> headerMap, long timeout, PluginCallBack.DownloadCallback callback);


---

7. 媒体消息（PluginMediaMsgMethod）

void sendMediaMsg(String talker, MediaMessage mediaMessage, String appId);
void shareFile(String talker, String title, String filePath, String appId);
void shareMiniProgram(String talker, String title, String description, String userName, String path, byte[] thumbData, String appId);
void shareMusic(String talker, String title, String description, String musicUrl, String musicDataUrl, byte[] thumbData, String appId);
void shareMusicVideo(String talker, String title, String description, String musicUrl, String musicDataUrl, String singerName, String duration, String songLyric, byte[] thumbData, String appId);
void shareText(String talker, String text, String appId);
void shareVideo(String talker, String title, String description, String videoUrl, byte[] thumbData, String appId);
void shareWebpage(String talker, String title, String description, String webpageUrl, byte[] thumbData, String appId);


---

8. 消息发送（PluginMsgMethod）

void sendText(String talker, String content);
void sendVoice(String talker, String sendPath);
void sendVoice(String talker, String sendPath, int duration);
void sendImage(String talker, String sendPath);
void sendImage(String talker, String sendPath, String appId);
void sendVideo(String talker, String sendPath);
void sendEmoji(String talker, String sendPath);
void sendPat(String talker, String pattedUser);
void sendShareCard(String talker, String wxid);
void sendLocation(String talker, String poiName, String label, String x, String y, String scale);
void sendLocation(String talker, JSONObject jsonObj);
void sendCipherMsg(String talker, String title, String content);
void sendAppBrandMsg(String talker, String title, String pagePath, String ghName);
void sendNoteMsg(String talker, String content);
void sendQuoteMsg(String talker, long msgId, String content);
void revokeMsg(long msgId);
void insertSystemMsg(String talker, String content, long createTime);


---

9. 其他方法（PluginOtherMethod）

void eval(String code);
void loadJava(String path);
void loadDex(String path);
void log(Object msg);
void toast(String text);
void notify(String title, String text);
Activity getTopActivity();
void uploadDeviceStep(long step);

社区验证可用（未官方文档化）

String getTargetTalker();
String getLoginWxid();


---

10. 朋友圈（PluginSnsMethod）

void uploadText(String content);
void uploadText(String content, String sdkId, String sdkAppName);
void uploadText(JSONObject jsonObj);

void uploadTextAndPicList(String content, String picPath);
void uploadTextAndPicList(String content, String picPath, String sdkId, String sdkAppName);
void uploadTextAndPicList(String content, List<String> picPathList);
void uploadTextAndPicList(String content, List<String> picPathList, String sdkId, String sdkAppName);
void uploadTextAndPicList(JSONObject jsonObj);


---

11. 开发建议（实践总结）

最佳指令拦截
onClickSendBtn() + getTargetTalker() → 指令不发出、结果单独回显

MsgInfo：只调用方法，禁止强转

稳定性：隐藏 API 当前可用，但未来可能调整



---

文档版本：WAuxiliary v1.2.6
整理 / 去重 / 排版：JP
更新时间：2025-12-14


---
