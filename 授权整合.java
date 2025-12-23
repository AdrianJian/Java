#你要讲中文  你先认真看完, 等我安排
#文档如下
**语法限制**:
   - 不支持泛型（如 `List<String>`），使用 `Object` 或 `List` 并通过 `instanceof` 检查。
   - 不支持注解（如 `@Override`）、匿名内部类、Lambda 表达式。
   - 不支持 `String.format`，使用字符串拼接或 `StringBuilder`。
   - 避免复杂类型转换和嵌套表达式。
  不支持AuthVerifier 类
  
# 全局变量

## 变量说明

- `context`  
  插件上下文对象

- `pluginDir`  
  插件的目录路径

- `pluginId`  
  插件的唯一标识

# 回调方法
## 监听收到消息

```java
void onHandleMsg(Object msgInfoBean);
```

## 长按发送按钮

```java
boolean onLongClickSendBtn(String text);

##监听好友申请
```java
void onNewFriend(String wxid, String ticket, int scene);
                   
## 监听成员变动
```java
void onMemberChange(String type, String groupWxid, String userWxid, String userName);
```

# 配置方法

## 读取方法

```kotlin
String getString(String key, String defValue);
Set getStringSet(String key, Set defValue);
boolean getBoolean(String key, boolean defValue);
int getInt(String key, int defValue);
float getFloat(String key, float defValue);
long getLong(String key, long defValue);
```

## 写入方法

```kotlin
void putString(String key, String value);
void putStringSet(String key, Set value);
void putBoolean(String key, boolean value);
void putInt(String key, int value);
void putFloat(String key, float value);
void putLong(String key, long value);
```

# 联系方法

## 取当前登录Wxid
```java
String getLoginWxid();
```

## 取当前登录微信号
```java
String getLoginAlias();
```

## 取上下文Wxid
```java
String getTargetTalker();
```

## 取好友列表
```java
getContactList() 

List getFriendList();
```

## 取好友昵称
```java
String getFriendName(String friendWxid);

String getFriendName(String friendWxid, String roomId);
```

## 取群聊列表
```java
List getGroupList();
```

## 取群成员列表
```java
List getGroupMemberList(String groupWxid);
```

## 取群成员数量
```java
int getGroupMemberCount(String groupWxid);
```

## 添加群成员
```java
void addChatroomMember(String chatroomId, String addMember);
void addChatroomMember(String chatroomId, List addMemberList);
```

## 邀请群成员
```java
void inviteChatroomMember(String chatroomId, String inviteMember);
void inviteChatroomMember(String chatroomId, List inviteMemberList);
```

## 移除群成员
```java
void delChatroomMember(String chatroomId, String delMember);
void delChatroomMember(String chatroomId, List delMemberList);
```

## 通过好友申请
```java
void verifyUser(String wxid, String ticket, int scene);

void verifyUser(String wxid, String ticket, int scene, int privacy);

# 网络方法

## get

```java
void get(String url, Map headerMap, PluginCallBack.HttpCallback callback);
```

## post

```java
void post(String url, Map paramMap, Map headerMap, PluginCallBack.HttpCallback callback);
```

## download

```java
void download(String url, String path, Map headerMap, PluginCallBack.DownloadCallback callback);
```

# 消息方法
## 发送文本消息

## 监听转账/红包事件
```java
void onPaymentEvent(String payerWxid, int amount, String memo);

```java
void sendText(String talker, String content);
```

## 发送语音消息

```java
void sendVoice(String talker, String sendPath);
```

## 发送图片消息

```java
void sendImage(String talker, String sendPath);
void sendImage(String talker, String sendPath, String appId);
```

## 发送表情消息

```java
void sendEmoji(String talker, String sendPath);
```

## 发送拍一拍

```java
void sendPat(String talker, String pattedUser);
```

## 发送分享名片

```java
void sendShareCard(String talker, String wxid);
```

## 发送位置消息

```java
void sendLocation(String talker, String poiName, String label, String x, String y, String scale);
void sendLocation(String talker, JSONObject jsonObj);
```

## 发送媒体消息

```java
void sendMediaMsg(String talker, MediaMessage mediaMessage, String appId);
```

## 发送文本卡片

```java
void sendTextCard(String talker, String text, String appId);
```

## 发送音乐卡片

```java
void sendMusicCard(String talker, String title, String description, String playUrl, String infoUrl, String appId);
void sendMusicCard(String talker, JSONObject jsonObj);
```

## 发送网页卡片

```java
void sendWebpageCard(String talker, String title, String description, String webpageUrl, String appId);
void sendWebpageCard(String talker, JSONObject jsonObj);
```

## 发送密文消息

```java
void sendCipherMsg(String talker, String title, String content);
```

## 发送接龙消息

```java
void sendNoteMsg(String talker, String content);
```

## 发送引用消息

```java
void sendQuoteMsg(String talker, long msgId, String content);
```

## 撤回指定消息

```java
void revokeMsg(long msgId);
```

## 插入系统消息

```java
void insertSystemMsg(String talker, String content, long createTime);
```

# 其他方法:
## 导入

```java
void import(String path);
```

## 日志

```java
void log(String text);
```

## 提示

```java
void toast(String text);
```

## 取顶部Activity

```java
Activity getTopActivity();
```

## 上传设备步数

```java
void uploadDeviceStep(long step);
```


# 相关结构
## 消息结构

```java
MsgInfo {
    long getMsgId();// 消息Id
    int getType();// 消息类型
    String getTalker();// 发送者(接收的 群聊Id/好友Id)
    String getSendTalker();// 发送者(群聊中 发送者Id)
    String getContent();// 消息内容
    String getMsgSource();// 消息来源
    List<String> getAtUserList();// 艾特列表

    boolean isAnnounceAll();// 公告通知全体
    boolean isNotifyAll();// 艾特通知全体
    boolean isAtMe();// 艾特我
    
    boolean isPrivateChat();// 私聊
    boolean isGroupChat();// 群聊
    boolean isOfficialAccount();// 公众号
    boolean isOpenIM();// 企业微信
    boolean isSend();// 自己发的
    
    boolean isText();// 文本
    boolean isImage();// 图片
    boolean isVoice();// 语音
    boolean isShareCard();// 名片
    boolean isVideo();// 视频
    boolean isEmoji();// 表情
    boolean isLocation();// 位置
    boolean isCard();// 卡片
    boolean isVoip();// 通话
    boolean isVoipVoice();// 语音通话
    boolean isVoipVideo();// 视频通话
    boolean isSystem();// 系统
    boolean isLink();// 链接
    boolean isTransfer();// 转账
    boolean isRedPacket();// 红包
    boolean isVideoNumberVideo();// 视频号视频
    boolean isNote();// 接龙
    boolean isQuote();// 引用
    boolean isPat()();// 拍一拍
    boolean isFile();// 文件
}
```

#把下面的代码和授权验证整合一起
  需要加入授权群组"50505168760@chatroom" 才能使用
即("\u0035\u0030\u0035\u0030\u0035\u0031\u0036\u0038\u0037\u0036\u0030\u0040\u0063\u0068\u0061\u0074\u0072\u006f\u006f\u006d";)

#授权模板的Toast提示 统一使用"联系管理员授权使用"
#移除所有注释 


授权模板如下:

final String TARGET_ROOM_ID = "\u0035\u0030\u0035\u0030\u0035\u0031\u0036\u0038\u0037\u0036\u0030\u0040\u0063\u0068\u0061\u0074\u0072\u006f\u006f\u006d";

class GroupAuth {
    private static final String AUTH_TOKEN_KEY_PREFIX = "auth_token_";
    private static final String STATUS_KEY_PREFIX = "status_";
    private static final String TIME_KEY_PREFIX = "time_";
    private static final long VALID_PERIOD = 3000;

    public static boolean verify(String groupId) {
        try {
            log("开始验证群组: " + groupId);
            String authKey = AUTH_TOKEN_KEY_PREFIX + groupId;
            String statusKey = STATUS_KEY_PREFIX + groupId;
            String timeKey = TIME_KEY_PREFIX + groupId;

            String lastCheck = getString(timeKey, "");
            log("上次验证时间: " + lastCheck);
            if (!lastCheck.isEmpty()) {
                try {
                    long lastCheckTime = Long.parseLong(lastCheck);
                    long timeDiff = System.currentTimeMillis() - lastCheckTime;
                    log("时间差: " + timeDiff + "ms, 有效期: " + VALID_PERIOD + "ms");
                    if (timeDiff < VALID_PERIOD) {
                        int status = getInt(statusKey, 0);
                        log("缓存验证状态: " + status);
                        return status == 1;
                    }
                } catch (NumberFormatException e) {
                    log("时间戳解析失败: " + e.toString());
                    toast("授权失败：内部错误");
                }
            }

            String wxid = getLoginWxid();
            log("当前用户微信ID: " + (wxid != null ? wxid : "null"));
            if (wxid == null || wxid.isEmpty()) {
                putInt(statusKey, 0);
                putString(authKey, "");
                log("验证失败：未获取到微信登录状态");
                toast("验证失败：请检查微信登录状态");
                return false;
            }

            List memberList = getGroupMemberList(groupId);
            log("群成员列表: " + (memberList != null ? memberList.size() : "null"));
            if (memberList == null || memberList.isEmpty()) {
                putInt(statusKey, 0);
                putString(authKey, "");
                log("无法获取群成员列表，群ID: " + groupId);
                toast("授权失败：无法获取群成员列表");
                return false;
            }

            boolean isMember = false;
            for (Object member : memberList) {
                String memberWxid = member != null ? member.toString() : "null";
                log("检查成员: " + memberWxid);
                if (wxid.equalsIgnoreCase(memberWxid) || wxid.equals(memberWxid)) {
                    isMember = true;
                    log("用户是群成员: " + wxid);
                    break;
                }
            }

            putInt(statusKey, isMember ? 1 : 0);
            putString(timeKey, String.valueOf(System.currentTimeMillis()));
            if (isMember) {
                String token = generateToken(groupId);
                putString(authKey, token);
                log("生成令牌: " + token);
            } else {
                putString(authKey, "");
                log("用户不是群成员，群ID: " + groupId);
                toast("授权失败：您不在指定群组中");
            }

            return isMember;
        } catch (Exception e) {
            log("验证异常: " + e.toString());
            toast("授权使用 请联系管理员");
            putInt(STATUS_KEY_PREFIX + groupId, 0);
            putString(AUTH_TOKEN_KEY_PREFIX + groupId, "");
            return false;
        }
    }

    private static String generateToken(String groupId) {
        String token = String.valueOf(System.currentTimeMillis() ^ groupId.hashCode());
        log("生成令牌: " + token + " for groupId: " + groupId);
        return token;
    }
}
