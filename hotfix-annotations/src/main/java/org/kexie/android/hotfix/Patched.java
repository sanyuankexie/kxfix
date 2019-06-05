package org.kexie.android.hotfix;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;

/**
 * 对于:
 * 1.新增的字段
 * 2.新增的方法
 * 3.替换的方法
 * 请打上此标记以便生成补丁的程序可以扫描到
 */

@Keep
@Target({ElementType.METHOD,ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Patched {
}
