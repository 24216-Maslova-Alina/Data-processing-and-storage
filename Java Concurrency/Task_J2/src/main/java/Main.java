package main.java;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите реализацию списка:");
        System.out.println("1 - LinkedStringList");
        System.out.println("2 - SynchronizedArrayList");

        int choice = scanner.nextInt();

        StringList list;

        if (choice == 1) {
            list = new LinkedStringList();
            System.out.println("Используется LinkedStringList");
        } else if (choice == 2) {
            list = new SynchronizedArrayList();
            System.out.println("Используется SynchronizedArrayList");
        } else {
            System.out.println("Неверный выбор!");
            scanner.close();
            return;
        }

        System.out.println("Количество потоков-сортировщиков:");
        int count = scanner.nextInt();
        System.out.println("Пауза между шагами (мс):");
        long between = scanner.nextLong();
        System.out.println("Пауза внутри шага (мс):");
        long inside = scanner.nextLong();
        scanner.nextLine();

        // создаём и запускаем сортировщиков
        SortWorker[] workers = new SortWorker[count];
        for (int i = 0; i < count; i++) {
            workers[i] = new SortWorker(list, between, inside);
            workers[i].start();
        }

        // родительский поток сам читает ввод
        System.out.println("Вводите строки. Пустая строка - вывод списка");
        System.out.println("Чтобы завершить ввод и начать сортировку введите 'exit'");

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            } else if (input.isEmpty()) {
                System.out.println("Текущий список: ");
                list.printList();
            } else {
                // длинные строки режем на куски по 80 символов
                for (int i = 0; i < input.length(); i += 80) {
                    list.addFirst(input.substring(i, Math.min(i + 80, input.length())));
                }
            }
        }

        // остановка сортировщиков
        for (SortWorker worker : workers) {
            worker.stopSorting();
        }
        for (SortWorker worker : workers) {
            worker.join();
        }

        System.out.println("\nПосле сортировки:");
        list.printList();

        System.out.println("\nСтатистика:");
        long total = 0;
        for (int i = 0; i < count; i++) {
            System.out.println("Шаги потока " + (i + 1) + ": " + workers[i].getSteps());
            total += workers[i].getSteps();
        }
        System.out.println("Общее количество шагов: " + total);
        System.out.println("Размер списка: " + list.size());

        scanner.close();
    }
}