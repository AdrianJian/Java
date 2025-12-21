// ==================== 授权模块开始 ====================
class AuthModule {
    private static final String[] TARGET_AUTH_GROUP_IDS = {
        "48438612763@chatroom",               
    }; 
    public static boolean check() {
        boolean isAuthorized = false;
        
        try {
            String wxid = getLoginWxid();
            if (wxid == null || wxid.isEmpty()) {
                toast("联系授权WeChat: JP_Adrian ");
                isAuthorized = false;
            } else {
                for (int i = 0; i < TARGET_AUTH_GROUP_IDS.length; i++) {
                    String groupId = TARGET_AUTH_GROUP_IDS[i];
                    if (groupId == null || groupId.isEmpty()) continue;
                    
                    List memberList = getGroupMemberList(groupId);
                    if (memberList == null || memberList.isEmpty()) {
                        continue;
                    }
                    for (Object member : memberList) {
                        String memberWxid = member != null ? member.toString() : "null";
                        if (wxid.equalsIgnoreCase(memberWxid) || wxid.equals(memberWxid)) {
                            isAuthorized = true;
                            break;
                        }
                    }
                    
                    if (isAuthorized) {
                        break;
                    }
                }
            }

            if (!isAuthorized) {
                toast("联系授权WeChat: JP_Adrian ");
            }
            
            return isAuthorized;
        } catch (Exception e) {
            try {
                toast("联系授权WeChat: JP_Adrian ");
            } catch (Exception ignore) {}
            isAuthorized = false;
            return false;
        }
    }
}

// 脚本启动时立即执行授权检测
final boolean IS_AUTHORIZED = AuthModule.check();
if (!IS_AUTHORIZED) {
    throw new RuntimeException("联系授权WeChat: JP_Adrian ");
}
// ==================== 授权模块结束 =====================