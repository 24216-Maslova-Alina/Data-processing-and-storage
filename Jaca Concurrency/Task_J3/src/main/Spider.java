package main;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Spider {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

    private final AtomicInteger pendingTasks = new AtomicInteger(0);
    private final Client client;
    public Spider(String port) {
        this.client = new Client(port);
    }

    public synchronized void addUrl(String path) {
        if (visited.add(path)) {
            pendingTasks.incrementAndGet();
            queue.add(path);
            notifyAll();
        }
    }

    public void processUrl(String path) {
        try {
            Client.ServerResponse response = client.sendRequest(path);

            if (response == null) {
                return;
            }

            messages.add(response.message());

            for (String successor : response.successors()) {
                addUrl(successor);
            }

        } catch (IOException e) {
            System.err.println("Ошибка HTTP: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            synchronized (this) {
                pendingTasks.decrementAndGet();
                notifyAll();
            }
        }
    }

    public List<String> crawl() throws InterruptedException {
        addUrl("/");

        while (true) {
            String path;

            synchronized (this) {
                while (queue.isEmpty() && pendingTasks.get() > 0) {
                    wait();
                }

                if (pendingTasks.get() == 0) {
                    break;
                }

                path = queue.poll();
            }

            if (path != null) {
                Thread.startVirtualThread(() -> processUrl(path));
            }
        }

        return messages.stream().sorted().toList();
    }
}
