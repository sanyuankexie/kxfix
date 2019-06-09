package org.kexie.android.hotfix;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;

/**
 * 被上此标记的类才具有热更新能力
 * 通过这个注解来过滤掉绝对不需要热更新的类
 *
 */
@Keep
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Hotfix {
}
