package org.kexie.android.hotfix.plugins.workflow;

final class Constants {
    static final String CODE_SCOPE_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Overload$CodeScope";
    static final String CODE_SCOPE_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.CodeScope";
    static final String OBJECT_SUPER_CLASS_NAME
            = "org.kexie.android.hotfix.internal.RedirectTarget";
    /**
     * 内部指令所使用的工具类名字
     *
     */
    static final String UTIL_CLASS_NAME
            = "org.kexie.android.hotfix.internal.Intrinsics";
    static final String HOTFIX_ANNOTATION = "org.kexie.android.hotfix.Hotfix";
    static final String OVERLOAD_ANNOTATION = "org.kexie.android.hotfix.Overload";
    /**
     * @see <a href="https://developer.android.google.cn/studio/write/java8-support?hl=zh_cn"/>
     * desugar 处理过的lambda表达式的前缀
     */
    static final String LAMBDA_PREFIX = "-$$Lambda$";

    private Constants() {
        throw new AssertionError();
    }
}
