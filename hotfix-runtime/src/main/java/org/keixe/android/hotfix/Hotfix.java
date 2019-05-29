package org.keixe.android.hotfix;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;

/**
 * 被上此标记的类才具有热更新能力
 */
@Keep
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Hotfix {
}
