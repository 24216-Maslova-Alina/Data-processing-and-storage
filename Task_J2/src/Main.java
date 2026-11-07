import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        // Выбор реализации списка
        System.out.println("Выберите реализацию списка:");
        System.out.println("1 - LinkedStringList");
        System.out.println("2 - SynchronizedArrayList");

        int choice = scanner.nextInt();
        scanner.nextLine();

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

        // Создание потоков
        InputThread inputThread = new InputThread(list, scanner);

        SortWorker worker1 = new SortWorker(list, 500, 100);
        SortWorker worker2 = new SortWorker(list, 500, 100);

        // Запуск потоков
        worker1.start();
        worker2.start();
        inputThread.start();

        // Ожидание завершения ввода
        inputThread.join();

        // Остановка сортировщиков
        worker1.stopSorting();
        worker2.stopSorting();

        // Ожидание завершения сортировщиков
        worker1.join();
        worker2.join();

        // Вывод результата
        System.out.println("\nПосле сортировки:");
        list.printList();

        System.out.println("\nСтатистика:");
        System.out.println("Шаги первого потока: " + worker1.getSteps());
        System.out.println("Шаги второго потока: " + worker2.getSteps());
        System.out.println("Общее количество шагов: "
                + (worker1.getSteps() + worker2.getSteps()));
        System.out.println("Размер списка: " + list.size());

        scanner.close();
    }
}
