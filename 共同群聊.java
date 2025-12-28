void SystemMsg(String group, String text) {
	insertSystemMsg(group, text, System.currentTimeMillis());
}

import me.hd.wauxv.data.bean.GroupInfo;

List CommonGroup(String wxid) {
	List RoomIds = new ArrayList();
	List Groups = new ArrayList();
	for (GroupInfo groupInfo: getGroupList()) {
		String roomId = groupInfo.getRoomId();
		RoomIds.add(roomId);
	}
	for (String group: RoomIds) {
		List groupMemberList = getGroupMemberList(group);
		for (String member: groupMemberList) {
			if (wxid.equals(member)) {
				Groups.add(group);
				break;
			}
		}
	}
	return Groups;
}

boolean onLongClickSendBtn(String text) {
	String qun = getTargetTalker();
	if ("共同群聊".equals(text)) {
		String common = getFriendName(qun) + "(" + qun + ")\n共同群聊如下:\n";
		for (String group: CommonGroup(qun)) {
			common += getFriendName(group) + "(" + group + ")\n";
		}
		SystemMsg(qun, common);
	}
	return false;
}

void onHandleMsg(Object data) {
	String text = data.content;
	String qun = data.talker;
	String wxid = data.sendTalker;
	if ("共同群聊".equals(text)) {
		String common = getFriendName(qun) + "(" + qun + ")\n共同群聊如下:\n";
		for (String group: CommonGroup(qun)) {
			common += getFriendName(group) + "(" + group + ")\n";
		}
		SystemMsg(qun, common);
	}
}