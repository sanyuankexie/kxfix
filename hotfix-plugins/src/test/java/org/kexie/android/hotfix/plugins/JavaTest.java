package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import io.reactivex.functions.Function;

public class JavaTest implements Function<Object,Object> {

    public static Collection<Integer> data(int xczxc, int as) {
        return Arrays.asList(xczxc, xczxc, xczxc, xczxc);
    }

    @Override
    public Object apply(Object object) {
        return null;
    }

    @Test
    public void test() throws Throwable {
    }
}
