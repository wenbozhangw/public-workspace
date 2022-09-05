package org.example.concurrent.collection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wenbo.zhang
 * @date 2022/9/5 17:38
 */
public class AQSTest {

    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {

        new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread " + Thread.currentThread().getName() + " is running, locked!");
                TimeUnit.SECONDS.sleep(30);
                System.out.println("Thread " + Thread.currentThread().getName() + " is run success, unlocked!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }).start();

        new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread " + Thread.currentThread().getName() + " is running, locked!");
                TimeUnit.SECONDS.sleep(30);
                System.out.println("Thread " + Thread.currentThread().getName() + " is run success, unlocked!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }).start();
    }
}
