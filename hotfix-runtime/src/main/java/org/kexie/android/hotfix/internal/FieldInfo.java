package org.kexie.android.hotfix.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

@Keep
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public @interface FieldInfo {
    int id();
}
