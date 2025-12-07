import org.json.JSONObject;
import org.json.JSONArray;
import me.hd.wauxv.plugin.api.callback.PluginCallBack.HttpCallback;

private static final String AMAP_API_KEY = "b2e6660a652a64a68b8238fbbf10129c"; // 高德 API Key

// 日志记录方法
private void log(String message) {
    log(message); // WAuxiliary 提供的日志方法
}

// 提示方法
private void toast(String message) {
    toast(message); // WAuxiliary 提供的提示方法
}

// 显式定义 HttpCallback 类，兼容 WAuxiliary 限制
private class MyHttpCallback implements HttpCallback {
    private String query;

    public MyHttpCallback(String query) {
        this.query = query;
    }

    public void onSuccess(int respCode, String respContent) {
        try {
            log("MyHttpCallback.onSuccess: 收到响应，查询=" + query + ", 响应码=" + respCode + ", 内容=" + respContent);
            if (respContent == null || respContent.trim().isEmpty()) {
                toast("地图查询失败: 无响应数据");
                log("MyHttpCallback.onSuccess: 响应为空，查询=" + query);
                return;
            }

            JSONObject json = new JSONObject(respContent);
            String status = json.getString("status");
            if (!status.equals("1")) {
                toast("地图查询失败: API 错误 - " + json.getString("info"));
                log("MyHttpCallback.onSuccess: API 错误，查询=" + query + ", 响应=" + respContent);
                return;
            }

            // 普通地点查询
            JSONArray geocodes = json.getJSONArray("geocodes");
            if (geocodes.length() == 0) {
                toast("未找到地点: " + query);
                log("MyHttpCallback.onSuccess: 未找到地点，查询=" + query);
                return;
            }
            JSONObject location = geocodes.getJSONObject(0);
            String[] latLng = location.getString("location").split(",");
            String longitude = latLng[0];
            String latitude = latLng[1];
            String formattedAddress = location.getString("formatted_address");

            // 保存查询结果到配置
            putString("last_query_latitude", latitude);
            putString("last_query_longitude", longitude);
            putString("last_query_address", formattedAddress);
            putString("last_query_scale", getString("last_query_scale", "25")); // 保留当前缩放级别
            toast("已保存地点: " + formattedAddress + " (纬度: " + latitude + ", 经度: " + longitude + ")");
            log("MyHttpCallback.onSuccess: 保存查询结果，地点=" + formattedAddress + ", 纬度=" + latitude + ", 经度=" + longitude);
        } catch (Exception e) {
            toast("解析地图数据失败: " + e.getMessage());
            log("MyHttpCallback.onSuccess: 解析异常，查询=" + query + ", 错误=" + (e != null ? e.toString() : "未知异常"));
        }
    }

    public void onError(Exception e) {
        toast("地图查询失败: 网络错误");
        log("MyHttpCallback.onError: 网络异常，查询=" + query + ", 错误=" + (e != null ? e.toString() : "未知异常"));
    }
}

// 处理位置消息（支持直接使用保存的经纬度）
private void handleLocationMsg(String talker, String input) {
    try {
        // 分割输入，允许任意长度的标题
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 1) {
            toast("请输入位置标题");
            log("handleLocationMsg: 输入为空，输入=" + input);
            return;
        }

        // 标题为整个输入（简短格式）或第一个部分（完整格式）
        String title = input.trim();
        String latitude;
        String longitude;
        String scale;

        // 检查是否为完整格式（至少包含标题、纬度、经度）
        if (parts.length >= 3) {
            title = parts[0];
            latitude = parts[1];
            longitude = parts[2];
            scale = parts.length >= 4 ? parts[3] : getString("last_query_scale", "25");
            // 存储经纬度和缩放级别到配置
            putString("last_query_latitude", latitude);
            putString("last_query_longitude", longitude);
            putString("last_query_scale", scale);
        } else {
            // 简短格式：使用上次保存的经纬度和缩放级别
            latitude = getString("last_query_latitude", "");
            longitude = getString("last_query_longitude", "");
            scale = getString("last_query_scale", "25");
            if (latitude.isEmpty() || longitude.isEmpty()) {
                toast("未找到保存的经纬度，请先使用地图命令查询地点");
                log("handleLocationMsg: 无保存的经纬度，输入=" + input);
                return;
            }
        }

        // 验证标题
        if (title.isEmpty()) {
            toast("位置标题不能为空");
            log("handleLocationMsg: 标题为空，输入=" + input);
            return;
        }

        // 验证经纬度格式
        try {
            Double.parseDouble(latitude);
            Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            toast("经纬度格式错误，请输入有效数字");
            log("handleLocationMsg: 无效的经纬度，输入=" + input + ", 纬度=" + latitude + ", 经度=" + longitude + ", 错误=" + e.toString());
            return;
        }

        // 验证缩放级别
        try {
            int scaleValue = Integer.parseInt(scale);
            if (scaleValue < 1 || scaleValue > 50) { // 缩放级别范围1-50
                toast("缩放级别必须在 1-50 之间");
                log("handleLocationMsg: 无效的缩放级别=" + scale + ", 输入=" + input);
                return;
            }
        } catch (NumberFormatException e) {
            toast("缩放级别格式错误，请输入有效整数");
            log("handleLocationMsg: 无效的缩放级别，输入=" + input + ", 错误=" + e.toString());
            return;
        }

        // 获取当前标签
        String label = getString("location_label", "Adrian 地图小能手");

        // 发送位置消息
        sendLocation(talker, title, label, latitude, longitude, scale);
        toast("位置消息已发送: " + title);
        log("handleLocationMsg: 发送位置，标题=" + title + ", 标签=" + label + ", 纬度=" + latitude + ", 经度=" + longitude + ", 缩放=" + scale);
    } catch (Exception e) {
        toast("发送位置失败，请稍后重试");
        log("handleLocationMsg: 异常，输入=" + input + ", 错误=" + (e != null ? e.toString() : "未知异常"));
    }
}

// 处理地图查询
private void handleMapQuery(String talker, String query) {
    try {
        if (query.isEmpty()) {
            toast("请输入要查询的地点");
            log("handleMapQuery: 地点名称为空");
            return;
        }

        // 构造高德地图 API 请求
        String url = "https://restapi.amap.com/v3/geocode/geo?address=" + query + "&key=" + AMAP_API_KEY;
        toast("正在查询地点: " + query);
        log("handleMapQuery: 发送请求，查询=" + query + ", URL=" + url);

        // 使用 get 方法和显式回调
        MyHttpCallback callback = new MyHttpCallback(query);
        get(url, null, callback);
    } catch (Exception e) {
        toast("地图查询失败，请稍后重试");
        log("handleMapQuery: 异常，查询=" + query + ", 错误=" + (e != null ? e.toString() : "未知异常"));
    }
}

// 处理缩放等级设置
private void handleScaleUpdate(String talker, String input) {
    try {
        // 调试：记录原始输入
        log("handleScaleUpdate: 原始输入=" + input);

        // 提取数字部分，兼容“缩放等级30”或“缩放等级 30”
        String scale = input.replace("缩放等级", "").trim();

        // 验证缩放级别
        try {
            int scaleValue = Integer.parseInt(scale);
            if (scaleValue < 1 || scaleValue > 50) {
                toast("缩放级别必须在 1-50 之间");
                log("handleScaleUpdate: 无效的缩放级别=" + scale);
                return;
            }
            // 存储缩放级别到配置
            putString("last_query_scale", scale);
            toast("缩放等级已设置为: " + scale);
            log("handleScaleUpdate: 缩放等级更新，缩放=" + scale);
        } catch (NumberFormatException e) {
            toast("缩放级别格式错误，请输入有效整数 (如: 缩放等级 30)");
            log("handleScaleUpdate: 无效的缩放级别，输入=" + input + ", 错误=" + e.toString());
        }
    } catch (Exception e) {
        toast("更新缩放等级失败，请稍后重试");
        log("handleScaleUpdate: 异常，输入=" + input + ", 错误=" + (e != null ? e.toString() : "未知异常"));
    }
}

// 处理标签修改
private void handleLabelUpdate(String talker, String input) {
    try {
        // 分割输入，预期格式：名称 标签内容
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length < 2) {
            toast("请输入完整的标签信息: 名称 标签内容");
            log("handleLabelUpdate: 参数不足，输入=" + input);
            return;
        }

        String name = parts[0];
        String labelContent = parts[1];

        // 验证名称和标签内容
        if (name.isEmpty() || labelContent.isEmpty()) {
            toast("名称或标签内容不能为空");
            log("handleLabelUpdate: 名称或标签内容为空，输入=" + input);
            return;
        }

        // 拼接名称和标签内容
        String fullLabel = name + " " + labelContent;

        // 存储标签到配置
        putString("location_label", fullLabel);
        toast("标签已更新为: " + fullLabel);
        log("handleLabelUpdate: 标签更新，名称=" + name + ", 标签内容=" + labelContent + ", 完整标签=" + fullLabel);
    } catch (Exception e) {
        toast("更新标签失败，请稍后重试");
        log("handleLabelUpdate: 异常，输入=" + input + ", 错误=" + (e != null ? e.toString() : "未知异常"));
    }
}

// 长按发送按钮事件处理
boolean onLongClickSendBtn(String text) {
    try {
        // 调试：记录脚本启动
        log("onLongClickSendBtn: 脚本启动，输入=" + text);

        // 验证输入
        if (text == null || text.trim().isEmpty()) {
            toast("输入不能为空");
            log("onLongClickSendBtn: 输入为空");
            return true;
        }

        String talker = getTargetTalker();

        // 处理位置命令
        if (text.startsWith("位置")) {
            String location = text.substring(2).trim();
            if (location.isEmpty()) {
                toast("位置信息不能为空");
                log("onLongClickSendBtn: 位置信息为空");
                return true;
            }
            handleLocationMsg(talker, location);
            return true;
        }

        // 处理地图查询命令
        if (text.startsWith("地图")) {
            String query = text.substring(2).trim();
            if (query.isEmpty()) {
                toast("请输入要查询的地点");
                log("onLongClickSendBtn: 地图查询为空");
                return true;
            }
            handleMapQuery(talker, query);
            return true;
        }

        // 处理缩放等级命令
        if (text.startsWith("缩放等级")) {
            String scaleInput = text; // 直接传递整个输入
            handleScaleUpdate(talker, scaleInput);
            return true;
        }

        // 处理标签修改命令
        if (text.startsWith("标签")) {
            String labelInput = text.substring(2).trim();
            if (labelInput.isEmpty()) {
                toast("标签信息不能为空");
                log("onLongClickSendBtn: 标签信息为空");
                return true;
            }
            handleLabelUpdate(talker, labelInput);
            return true;
        }

        // 避免WAUXV其他钩子记录“无效指令”
        log("onLongClickSendBtn: 未匹配任何指令，输入=" + text);
        return false; // 允许其他钩子处理
    } catch (Exception e) {
        toast("操作失败，请稍后重试");
        log("onLongClickSendBtn: 异常，输入=" + text + ", 错误=" + (e != null ? e.toString() : "未知异常"));
        return true;
    }
}

// 脚本初始化时恢复持久化数据
void onLoad() {
    try {
        // 恢复上次保存的经纬度和缩放级别
        String latitude = getString("last_query_latitude", "");
        String longitude = getString("last_query_longitude", "");
        String scale = getString("last_query_scale", "25");
        String address = getString("last_query_address", "");
        String label = getString("location_label", "Adrian 地图小能手");

        log("onLoad: 脚本初始化，恢复数据 - 纬度=" + latitude + ", 经度=" + longitude + ", 缩放=" + scale + ", 地址=" + address + ", 标签=" + label);
    } catch (Exception e) {
        log("onLoad: 初始化异常，错误=" + (e != null ? e.toString() : "未知异常"));
    }
}