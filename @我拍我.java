//BY-JP Adrian931227 ❤️
/*· 长按发送: 拍我开/关  (开启@我拍我功能)
· */
private static final String VOICE_PATH = "/storage/emulated/0/Android/media/com.tencent.mm/WAuxiliary/plugin/语音/";
private static final String PAT_SWITCH_KEY_PREFIX = "pat_switch_"; // 会话隔离的开关键前缀

void onHandleMsg(Object msgInfo) {
    try {
        if (msgInfo == null) {
            log("msgInfo is null");
            return;
        }

        String loginWxid = getLoginWxid();
        String talker = msgInfo.getTalker();
        String sendTalker = msgInfo.getSendTalker();
        String content = msgInfo.getContent();
        String cleanedContent = content != null ? content.replaceAll("[\\s\\u2005\\u2006\\u2007\\u2008\\u2009\\u200A\\u202F\\u205F\\u3000\\ufeff]+", "").trim().toLowerCase() : "";

        boolean isAtSelf = false;
        String voiceFilePath = null;

        // 检查开关状态，会话隔离，默认关闭
        boolean isPatEnabled = getBoolean(PAT_SWITCH_KEY_PREFIX + talker, false);

        if (msgInfo.isPat() && isPatEnabled) {
            boolean isPattedSelf = false;
            String pattedUser = null;

            // 优先尝试使用 PatMsg 结构
            try {
                Object patMsg = msgInfo.getPatMsg();
                if (patMsg != null) {
                    pattedUser = patMsg.getPattedUser();
                    isPattedSelf = pattedUser != null && pattedUser.equals(loginWxid);
                    log("Pat message detected, isPattedSelf=" + isPattedSelf + ", pattedUser=" + pattedUser + ", content=" + content);
                } else {
                    log("PatMsg is null, falling back to template");
                }
            } catch (Exception e) {
                log("Error accessing PatMsg: " + e.toString());
                // 回退到使用 getTemplate() 或 getContent()
                String template = msgInfo.getTemplate();
                isPattedSelf = template != null && template.contains(loginWxid);
                log("Fallback: Using template, isPattedSelf=" + isPattedSelf + ", template=" + template);
            }

            if (isPattedSelf) {
                long currentTime = System.currentTimeMillis();
                long lastPatSentTime = getLong("pat_last_sent_time_" + talker, 0);
                if (currentTime - lastPatSentTime < 1000) {
                    log("Ignoring: Too frequent pat, lastPatSentTime=" + lastPatSentTime);
                    return;
                }
                voiceFilePath = VOICE_PATH + "拍我语音.silk";
                log("Pat message for self, voiceFilePath: " + voiceFilePath);
                isAtSelf = true;
            } else {
                log("Not patted myself, pattedUser=" + pattedUser + ", template=" + msgInfo.getTemplate());
            }
        } else if (msgInfo.isGroupChat() && !sendTalker.equals(loginWxid) && isPatEnabled) {
            voiceFilePath = VOICE_PATH + "@我语音.silk";
            isAtSelf = isAtMe(msgInfo, cleanedContent, loginWxid, talker);
        }

        if (isAtSelf && voiceFilePath != null) {
            long currentTime = System.currentTimeMillis();
            String timeKey = msgInfo.isPat() ? "pat_last_sent_time_" + talker : "at_last_sent_time_" + talker;
            long lastSentTime = getLong(timeKey, 0);
            if (currentTime - lastSentTime < 1000) {
                log("Ignoring: Too frequent message, lastSentTime=" + lastSentTime + ", timeKey=" + timeKey);
                return;
            }

            File voiceFile = new File(voiceFilePath);
            if (!voiceFile.exists()) {
                log("Error: Voice file does not exist: " + voiceFilePath);
                toast("语音文件不存在");
                return;
            }
            if (!voiceFile.canRead()) {
                log("Error: Voice file is not readable: " + voiceFilePath);
                toast("语音文件不可读");
                return;
            }

            try {
                log("Sending voice to talker: " + talker);
                sendVoice(talker, voiceFilePath);
                putLong(timeKey, currentTime);
                log("Voice sent successfully to talker: " + talker);
                toast("语音发送成功");
            } catch (Exception e) {
                log("Error: Failed to send voice: " + e.toString());
                toast("语音发送失败: " + e.getMessage());
            }
        }
    } catch (Exception e) {
        logAndToast("消息处理异常: " + e.toString(), "消息处理失败");
    }
}

boolean isAtMe(Object msgInfo, String cleanedContent, String loginWxid, String roomId) {
    try {
        String wxNickname = null;
        try {
            wxNickname = getFriendName(loginWxid);
        } catch (Exception e) {
            log("Failed to get wxNickname: " + e.toString());
        }

        String groupNickname = null;
        try {
            groupNickname = getFriendName(loginWxid, roomId);
        } catch (Exception e) {
            log("Failed to get groupNickname: " + e.toString());
        }

        return cleanedContent.equals("@" + loginWxid.toLowerCase()) ||
               cleanedContent.contains("@" + loginWxid.toLowerCase()) ||
               (wxNickname != null && !wxNickname.isEmpty() &&
                (cleanedContent.equals("@" + wxNickname.toLowerCase()) ||
                 cleanedContent.contains("@" + wxNickname.toLowerCase()))) ||
               (groupNickname != null && !groupNickname.isEmpty() &&
                (cleanedContent.equals("@" + groupNickname.toLowerCase()) ||
                 cleanedContent.contains("@" + groupNickname.toLowerCase())));
    } catch (Exception e) {
        log("isAtMe exception: " + e.toString());
        return false;
    }
}

boolean onLongClickSendBtn(String text) {
    try {
        String talker = getTargetTalker();
        if (talker == null) {
            log("onLongClickSendBtn: talker is null");
            toast("无法获取当前会话");
            return false;
        }

        String cleanedText = text != null ? text.replaceAll("[\\s\\u2005\\u2006\\u2007\\u2008\\u2009\\u200A\\u202F\\u205F\\u3000\\ufeff]+", "").trim().toLowerCase() : "";
        String switchKey = PAT_SWITCH_KEY_PREFIX + talker;

        if (cleanedText.equals("拍我开")) {
            putBoolean(switchKey, true);
            insertSystemMsg(talker, "拍我功能已开启", System.currentTimeMillis());
            log("Pat switch enabled for talker: " + talker);
            return true;
        } else if (cleanedText.equals("拍我关")) {
            putBoolean(switchKey, false);
            insertSystemMsg(talker, "拍我功能已关闭", System.currentTimeMillis());
            log("Pat switch disabled for talker: " + talker);
            return true;
        }

        return false; // 非指令，返回 false 让其他脚本处理
    } catch (Exception e) {
        logAndToast("长按发送按钮处理异常: " + e.toString(), "指令处理失败");
        return false;
    }
}

void logAndToast(String logText, String toastText) {
    log(logText);
    toast(toastText);
}