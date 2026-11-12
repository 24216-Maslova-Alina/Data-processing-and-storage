package main.java;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedStringList implements StringList {

    private static class Node {
        private final String value;
        private Node next;
        private final ReentrantLock lock = new ReentrantLock();

        private Node(String value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    private final Node sentinel = new Node(null, null); // фиктивная голова

    private final AtomicInteger size = new AtomicInteger();

    @Override
    public void addFirst(String str) {
        sentinel.lock.lock();
        try {
            sentinel.next = new Node(str, sentinel.next);
            size.incrementAndGet();
        } finally {
            sentinel.lock.unlock();
        }
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public Iterator<String> iterator() {
        List<String> snapshot = new ArrayList<>();

        Node current = sentinel;
        current.lock.lock();
        try {
            while (true) {
                Node next = current.next;
                if (next == null) {
                    break;
                }
                next.lock.lock();
                current.lock.unlock();
                current = next;
                snapshot.add(current.value);
            }
        } finally {
            current.lock.unlock();
        }

        return snapshot.iterator();
    }

    @Override
    public boolean sortStep(int position, long delayInsideStep) {
        Node previous = sentinel;
        previous.lock.lock();

        Node first = null;
        Node second;

        try {
            first = previous.next;
            if (first == null) {
                return false;
            }
            first.lock.lock();

            for (int i = 0; i < position; i++) {
                Node next = first.next;
                if (next == null) {
                    return false;
                }
                next.lock.lock();
                previous.lock.unlock();
                previous = first;
                first = next;
            }

            second = first.next;
            if (second == null) {
                return false;
            }
            second.lock.lock();

            try {
                sleepInside(delayInsideStep);

                if (first.value.compareTo(second.value) > 0) {
                    first.next = second.next;
                    second.next = first;
                    previous.next = second;
                }
            } finally {
                second.lock.unlock();
            }

            return true;

        } finally {
            if (first != null) {
                first.lock.unlock();
            }
            previous.lock.unlock();
        }
    }

    private static void sleepInside(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}