package main.java;

public class Experiment {

    // запускает два сортировщика на заданное число секунд и возвращает общее число шагов
    static long run(StringList list, long between, long inside, int seconds) throws InterruptedException {
        // кладём в список 10 строк
        for (int i = 0; i < 10; i++) {
            list.addFirst("строка" + i);
        }

        SortWorker worker1 = new SortWorker(list, between, inside);
        SortWorker worker2 = new SortWorker(list, between, inside);

        worker1.start();
        worker2.start();

        Thread.sleep(seconds * 1000L);   // ждём

        worker1.stopSorting();
        worker2.stopSorting();
        worker1.join();
        worker2.join();

        System.out.println("  поток 1: " + worker1.getSteps()
                + ", поток 2: " + worker2.getSteps());
        return worker1.getSteps() + worker2.getSteps();
    }

    public static void main(String[] args) throws InterruptedException {
        int seconds = 30;

        // опыт 1: как в Main (пауза между шагами 500, внутри шага 100)
        long between = 500;
        long inside = 100;
        System.out.println("Опыт 1: паузы " + between + " и " + inside + ", время " + seconds + " с");

        System.out.println("LinkedStringList:");
        long own = run(new LinkedStringList(), between, inside, seconds);
        System.out.println("  всего: " + own);

        System.out.println("SynchronizedArrayList:");
        long lib = run(new SynchronizedArrayList(), between, inside, seconds);
        System.out.println("  всего: " + lib);

        // опыт 2: без паузы между шагами
        between = 0;
        System.out.println();
        System.out.println("Опыт 2: паузы " + between + " и " + inside + ", время " + seconds + " с");

        System.out.println("LinkedStringList:");
        own = run(new LinkedStringList(), between, inside, seconds);
        System.out.println("  всего: " + own);

        System.out.println("SynchronizedArrayList:");
        lib = run(new SynchronizedArrayList(), between, inside, seconds);
        System.out.println("  всего: " + lib);
    }
}