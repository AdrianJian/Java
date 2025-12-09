import android.content.Context;

Object getAppLoader(String pkg) {
	try {
		Context ctx = context.createPackageContext(
			pkg,
			Context.CONTEXT_IGNORE_SECURITY |
			Context.CONTEXT_INCLUDE_CODE);
		return ctx.getClassLoader();
	} catch (Exception e) {
		return null;
	}
}

Object yun = getAppLoader("me.yun.plugin");
if (yun != null) {
	this.interpreter.getClassManager()
		.setClassLoader(yun);
	me.yun.plugin.Native.init(this);
} else toast("未安装插件工具, 无法使用");