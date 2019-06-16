package org.kexie.android.hotfix.internal;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;


/**
 * 为修复的dex中的每一个成员添加id
 * 在热修复框架运行时使用Id索引那些成员
 */
@Keep
@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public @interface Id {
    int value();
}
