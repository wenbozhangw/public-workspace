package org.example.concurrent.collection;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/4/16 10:00 上午
 */
public class SynchronizedSetTest {


    public static void test() throws InterruptedException {
        // List<Integer> list = Collections.synchronizedList(new ArrayList<>());
        List<Integer> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Task(list, latch).start();
        }
        latch.await();
        System.out.println(" list size = " + list.size());

    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            test();
        }
    }

    public static class Task extends Thread {

        private final List<Integer> list;

        private final CountDownLatch latch;

        public Task(List<Integer> list, CountDownLatch latch) {
            this.latch = latch;
            this.list = list;
        }

        @Override
        public void run() {
            for (int j = 0; j < 100; j++) {
                list.add(j);
            }
            latch.countDown();
        }
    }
}
