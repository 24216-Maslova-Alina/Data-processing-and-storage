package main;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String port = args.length > 0 ? args[0] : "8080";

        Spider spider = new Spider(port);
        try {
            List<String> messages = spider.crawl();
            for (String message : messages) {
                System.out.println(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Обход был прерван.");
        }

    }
}