package org.example.concurrent.collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/16 10:00 上午
 */
public class SynchronizedSetTest {

    public static volatile Set<Integer> set = Collections.synchronizedSet(new HashSet<>());
    public static final Object mutex = new Object();
//    public static volatile Set<Integer> set = new HashSet<>();

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            int finalI = 100 * i;
            Thread t1 = new Thread(() -> {
                for (int j = 1; j < 101; j++) {
                    synchronized (mutex) {
                        set.add(finalI + j);
                    }
                }
            });
            t1.start();
        }
        System.out.println(set.size());

    }
}
