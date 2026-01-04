import java.util.List;
import java.util.Arrays;

String AUTO_REPLY_KEY = "auto_reply_content";
String AUTO_REPLY_ENABLED_KEY = "auto_reply_enabled";
String autoReplyContent = "";
boolean autoReplyEnabled = false;

void init() {
    try {
        autoReplyContent = getString(AUTO_REPLY_KEY, "").replaceAll("\\|\\|", "\n");
        autoReplyEnabled = getBoolean(AUTO_REPLY_ENABLED_KEY, false);
        log("Init completed, auto-reply enabled: " + autoReplyEnabled + ", content: " + autoReplyContent.replaceAll("\n", "||"));
    } catch (Exception e) {
        log("Init failed: " + e.toString());
    }
}

void onNewFriend(String wxid, String ticket, int scene) {
    try {
        verifyUser(wxid, ticket, scene);
        toast("Auto-passed friend request: " + wxid);
        log("New friend request accepted: wxid=" + wxid + ", scene=" + scene);
        if (autoReplyEnabled && autoReplyContent != null && !autoReplyContent.equals("")) {
            try {
                Thread.sleep(3000);
                sendText(wxid, autoReplyContent);
                log("Sent auto-reply to " + wxid + ": " + autoReplyContent.replaceAll("\n", "||") + " after 3s delay");
            } catch (Exception e) {
                log("Failed to send auto-reply to " + wxid + ": " + e.toString());
            }
        }
    } catch (Exception e) {
        log("Handle friend request failed: wxid=" + wxid + ", error: " + e.toString());
    }
}

boolean onLongClickSendBtn(String text) {
    try {
        String cleanedText = text == null ? "" : text.trim().replaceAll("[\\p{Cntrl}\\p{Space}\\p{Z}\\p{C}&&[^\\n]]+", "");
        boolean isReplyCommand = cleanedText.length() >= 2 &&
            (int) cleanedText.charAt(0) == 22238 && (int) cleanedText.charAt(1) == 22797 ||
            cleanedText.startsWith("reply");
        if (isReplyCommand) {
            String replyContent = cleanedText.startsWith("reply") 
                ? cleanedText.substring(5).trim() 
                : cleanedText.substring(2).trim();
            if (replyContent.equals("")) {
                autoReplyEnabled = false;
                autoReplyContent = "";
                putBoolean(AUTO_REPLY_ENABLED_KEY, false);
                putString(AUTO_REPLY_KEY, "");
                toast("Auto-reply disabled");
                log("Auto-reply disabled");
            } else {
                autoReplyEnabled = true;
                autoReplyContent = replyContent;
                putBoolean(AUTO_REPLY_ENABLED_KEY, true);
                putString(AUTO_REPLY_KEY, replyContent.replaceAll("\n", "||"));
                toast("Auto-reply set: " + replyContent);
                log("Auto-reply set to: " + replyContent.replaceAll("\n", "||"));
            }
            return true;
        }
    } catch (Exception e) {
        log("Handle long-click failed: text=" + (text == null ? "null" : text) + ", error: " + e.toString());
    }
    return false;
}

init();