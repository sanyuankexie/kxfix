package org.keixe.android.hotfix.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class Patch {

    final Map<Method, Integer> mFixedMethodIds = new HashMap<>();

    protected abstract Object apply(int methodId, Object target, Object[] pram) throws Throwable;
}
