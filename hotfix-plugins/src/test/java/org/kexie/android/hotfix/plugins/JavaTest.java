package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import io.reactivex.Single;
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
        Single<Object> objectSingle = Single.just(new Object());
        objectSingle.map(new JavaTest()).subscribe(System.out::println);
        objectSingle.subscribe(System.out::println);
    }
}
