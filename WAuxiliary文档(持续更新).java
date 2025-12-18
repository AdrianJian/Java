# WAuxiliary Plugin 完整 API 文档（v1.2.6）

**适用于 WAuxiliary v1.2.6**  
**文档性质**：官方 API + 社区验证整合版  
**文档版本**：v1.2.6  
**整理/去重/排版**：JP（基于社区贡献）  
**最后更新时间**：2025-12-14

---

## 1. 全局变量（PluginGlobal）

| 变量名              | 说明                  |
|---------------------|-----------------------|
| hostContext        | 宿主上下文             |
| hostVerName        | 宿主版本名             |
| hostVerCode        | 宿主版本号             |
| hostVerClient      | 宿主客户端             |
| moduleVer          | 模块版本               |
| cacheDir           | 缓存目录               |
| pluginDir          | 插件目录               |
| pluginId           | 插件标识               |
| pluginName         | 插件名称               |
| pluginAuthor       | 插件作者               |
| pluginVersion      | 插件版本               |
| pluginUpdateTime   | 插件更新时间           |

---

## 2. 回调方法

| 方法签名                                      | 说明                  |
|-----------------------------------------------|-----------------------|
| `void onLoad()`                               | 插件加载时调用         |
| `void onUnLoad()`                             | 插件卸载时调用         |
| `void onHandleMsg(Object msgInfoBean)`        | 监听收到消息（msgInfoBean 为 MsgInfo 对象） |
| `boolean onClickSendBtn(String text)`          | 单击发送按钮前拦截，返回 true 表示拦截（不发送） |
| `void onMemberChange(String type, String groupWxid, String userWxid, String userName)` | 监听群成员变动（type 为 add/del 等） |
| `void onNewFriend(String wxid, String ticket, int scene)` | 监听新好友申请 |

---

## 3. 消息结构 MsgInfo

### MsgInfo 方法

| 方法                                          | 返回类型 | 说明                          |
|-----------------------------------------------|----------|-------------------------------|
| `long getMsgId()`                             | long     | 消息 ID                       |
| `int getType()`                               | int      | 消息类型                      |
| `long getCreateTime()`                        | long     | 创建时间（时间戳）            |
| `String getTalker()`                           | String   | 聊天对象 ID（群或私聊）       |
| `String getSendTalker()`                       | String   | 发送者 ID                     |
| `String getContent()`                         | String   | 消息内容                      |
| `String getMsgSource()`                       | String   | 消息来源                      |
| `List<String> getAtUserList()`                | List     | @ 用户列表                    |
| `boolean isAnnounceAll()`                     | boolean  | 是否公告@全体                 |
| `boolean isNotifyAll()`                       | boolean  | 是否通知@全体                 |
| `boolean isAtMe()`                            | boolean  | 是否@我                       |
| `QuoteMsg getQuoteMsg()`                      | QuoteMsg | 引用消息（若有）              |
| `PatMsg getPatMsg()`                          | PatMsg   | 拍一拍消息（若有）            |
| `FileMsg getFileMsg()`                        | FileMsg  | 文件消息（若有）              |
| `boolean isPrivateChat()`                     | boolean  | 是否私聊                      |
| `boolean isOpenIM()`                          | boolean  | 是否企业微信                  |
| `boolean isGroupChat()`                       | boolean  | 是否群聊                      |
| `boolean isChatroom()`                        | boolean  | 是否普通群聊                  |
| `boolean isImChatroom()`                      | boolean  | 是否企业群聊                  |
| `boolean isOfficialAccount()`                 | boolean  | 是否公众号                    |
| `boolean isSend()`                            | boolean  | 是否自己发送                  |
| `boolean isText()` / `isImage()` / `isVoice()` 等 | boolean  | 判断具体消息类型（文本、图片、语音、名片、视频、表情、位置、应用、通话、系统、链接、转账、红包、视频号、接龙、引用、拍一拍、文件） |

### QuoteMsg（引用消息）

| 方法                       | 返回类型 | 说明            |
|----------------------------|----------|-----------------|
| `String getTitle()`        | String   | 回复标题        |
| `String getMsgSource()`    | String   | 消息来源        |
| `String getSendTalker()`   | String   | 发送者 ID       |
| `String getDisplayName()`  | String   | 显示昵称        |
| `String getTalker()`       | String   | 聊天 ID         |
| `int getType()`            | int      | 消息类型        |
| `String getContent()`      | String   | 内容            |

### PatMsg（拍一拍）

| 方法                       | 返回类型 | 说明            |
|----------------------------|----------|-----------------|
| `String getTalker()`       | String   | 聊天 ID         |
| `String getFromUser()`     | String   | 发起者 ID       |
| `String getPattedUser()`   | String   | 被拍者 ID       |
| `String getTemplate()`     | String   | 模板内容        |
| `long getCreateTime()`     | long     | 创建时间        |

### FileMsg（文件）

| 方法                  | 返回类型 | 说明            |
|-----------------------|----------|-----------------|
| `String getTitle()`   | String   | 文件标题        |
| `long getSize()`      | long     | 文件大小（字节）|
| `String getExt()`     | String   | 文件后缀        |
| `String getMd5()`     | String   | 文件 MD5        |

---

## 4. 音频转换方法

| 方法                                          | 返回类型 | 说明                          |
|-----------------------------------------------|----------|-------------------------------|
| `File mp3ToSilkFile(String mp3Path)`          | File     | MP3 → Silk 文件对象           |
| `String mp3ToSilkPath(String mp3Path)`        | String   | MP3 → Silk 文件路径           |
| `void mp3ToSilk(String mp3Path, String silkPath)` | void | MP3 → 指定路径 Silk           |
| `File silkToMp3File(String silkPath)`         | File     | Silk → MP3 文件对象           |
| `String silkToMp3Path(String silkPath)`       | String   | Silk → MP3 文件路径           |
| `void silkToMp3(String silkPath, String mp3Path)` | void | Silk → 指定路径 MP3           |
| `void getSilkDuration(String silkPath)`       | void     | 获取 Silk 时长（毫秒，实际返回方式需查看日志或回调） |

---

## 5. 配置方法（PluginConfigMethod）

### 读取

| 方法                                          | 返回类型 | 默认值参数            |
|-----------------------------------------------|----------|-----------------------|
| `String getString(String key, String defValue)`     | String   | defValue             |
| `Set getStringSet(String key, Set defValue)`        | Set      | defValue             |
| `boolean getBoolean(String key, boolean defValue)` | boolean  | defValue             |
| `int getInt(String key, int defValue)`             | int      | defValue             |
| `float getFloat(String key, float defValue)`       | float    | defValue             |
| `long getLong(String key, long defValue)`          | long     | defValue             |

### 写入

| 方法                                          | 参数                     |
|-----------------------------------------------|--------------------------|
| `void putString(String key, String value)`     |                          |
| `void putStringSet(String key, Set value)`     |                          |
| `void putBoolean(String key, boolean value)`  |                          |
| `void putInt(String key, int value)`          |                          |
| `void putFloat(String key, float value)`      |                          |
| `void putLong(String key, long value)`        |                          |

---

## 6. 联系人相关方法

| 方法                                          | 返回类型 / 说明                          |
|-----------------------------------------------|------------------------------------------|
| `String getLoginWxid()`                       | 当前登录账号 wxid                        |
| `String getLoginAlias()`                      | 当前登录微信号                           |
| `String getTargetTalker()`                    | 当前上下文聊天对象 wxid                  |
| `List<FriendInfo> getFriendList()`            | 好友列表                                 |
| `String getFriendName(String friendWxid)`     | 获取好友昵称（私聊）                     |
| `String getFriendName(String friendWxid, String roomId)` | 获取群内显示昵称/备注                  |
| `void getAvatarUrl(String username)`          | 获取小头像 URL（异步）                   |
| `void getAvatarUrl(String username, boolean isBigHeadImg)` | 大/小头像                     |
| `List<GroupInfo> getGroupList()`              | 群聊列表                                 |
| `List<String> getGroupMemberList(String groupWxid)` | 群成员 wxid 列表                   |
| `int getGroupMemberCount(String groupWxid)`   | 群成员数量                               |
| `void addChatroomMember(String chatroomId, String/List<String> addMember)` | 加人           |
| `void inviteChatroomMember(String chatroomId, String/List<String> inviteMember)` | 邀请加群 |
| `void delChatroomMember(String chatroomId, String/List<String> delMember)` | 踢人         |
| `void verifyUser(String wxid, String ticket, int scene)` | 通过好友申请                   |
| `void verifyUser(String wxid, String ticket, int scene, int privacy)` | 通过好友申请（带隐私设置） |
| `void modifyContactLabelList(String username, String/List<String> label)` | 修改好友标签   |

**补充结构说明**：

- `GroupInfo`: `getRoomId()`（群 wxid，@chatroom 结尾）、`getName()`（群名称）
- `FriendInfo`: `getWxid()`（好友 wxid）、`getNickname()`（昵称）、`getRemark()`（备注名）

     data.isSend() → 返回 true 表示当前这条消息是自己发的
     data.isSend() → 返回 false 表示是对方发的

---

## 7. 网络方法（PluginHttpMethod）

| 方法                                          | 说明                                     |
|-----------------------------------------------|------------------------------------------|
| GET / POST / DOWNLOAD                         | 支持 header、timeout、回调               |
| `void get/post/download(..., PluginCallBack.HttpCallback/DownloadCallback callback)` | 异步回调结果 |

---

## 8. 媒体消息分享（PluginMediaMsgMethod）

| 方法                                          | 说明                                     |
|-----------------------------------------------|------------------------------------------|
| `void shareFile(...)`                         | 分享文件                                 |
| `void shareMiniProgram(...)`                  | 分享小程序                               |
| `void shareMusic(...)`                        | 分享音乐                                 |
| `void shareMusicVideo(...)`                   | 分享音乐视频                             |
| `void shareText(...)`                         | 分享纯文本                               |
| `void shareVideo(...)`                        | 分享视频                                 |
| `void shareWebpage(...)`                      | 分享网页链接                             |

---

## 9. 消息发送（PluginMsgMethod）

| 方法                                          | 说明                                     |
|-----------------------------------------------|------------------------------------------|
| `void sendText(String talker, String content)` | 发送文本                                |
| `void sendVoice(String talker, String path, int duration?)` | 发送语音（Silk 格式）            |
| `void sendImage(String talker, String path, String appId?)` | 发送图片                         |
| `void sendVideo(String talker, String path)`  | 发送视频                                 |
| `void sendEmoji(String talker, String path)`   | 发送表情                                 |
| `void sendPat(String talker, String pattedUser)` | 发送拍一拍                            |
| `void sendShareCard(String talker, String wxid)` | 发送名片                              |
| `void sendLocation(...)`                      | 发送位置                                 |
| `void sendCipherMsg(...)`                     | 发送密文消息                             |
| `void sendAppBrandMsg(...)`                   | 发送小程序消息                           |
| `void sendNoteMsg(...)`                       | 发送接龙                                 |
| `void sendQuoteMsg(String talker, long msgId, String content)` | 发送引用消息            |
| `void revokeMsg(long msgId)`                  | 撤回消息                                 |
| `void insertSystemMsg(...)`                   | 插入系统消息                             |

---

## 10. 其他方法（PluginOtherMethod）

| 方法                                          | 说明                                     |
|-----------------------------------------------|------------------------------------------|
| `void eval(String code)`                      | 执行 BeanShell 代码                      |
| `void loadJava(String path)`                  | 加载 Java 类                             |
| `void loadDex(String path)`                   | 加载 Dex 文件                            |
| `void log(Object msg)`                        | 打印日志                                 |
| `void toast(String text)`                     | 弹吐司                                   |
| `void notify(String title, String text)`      | 发送通知                                 |
| `Activity getTopActivity()`                   | 获取顶部 Activity                        |
| `void uploadDeviceStep(long step)`            | 上传设备步数                             |

---

## 11. 朋友圈方法（PluginSnsMethod）

| 方法                                          | 说明                                     |
|-----------------------------------------------|------------------------------------------|
| `void uploadText(String content, ...)`        | 发纯文字朋友圈                           |
| `void uploadTextAndPicList(String content, String/List<String> picPathList, ...)` | 发图文朋友圈 |

支持 JSONObject 方式传入更多参数。

---

## 注意事项

- 脚本引擎为 **BeanShell**，对 Java 8 Lambda 表达式支持不完善（尤其是需要接口强制转换的场景），建议使用匿名内部类方式。
- 所有异步方法（如网络、头像获取）通常通过回调或日志返回结果。
- 部分方法（如音频时长）可能需通过日志查看返回值。

WAuxiliary 框架使用的 BeanShell 脚本引擎不支持 Java 8+ 的注解语法（尤其是 @Override），而且对某些关键字（如 final 在 lambda 或匿名类参数中）解析也很严格。

**文档结束**