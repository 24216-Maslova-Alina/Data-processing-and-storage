package main.java;

public class SortWorker extends Thread {

    private final StringList list;
    private final long delayBetweenSteps;
    private final long delayInsideStep;

    private volatile long steps = 0;
    private volatile boolean running = true;

    public SortWorker(
            StringList list,
            long delayBetweenSteps,
            long delayInsideStep) {

        this.list = list;
        this.delayBetweenSteps = delayBetweenSteps;
        this.delayInsideStep = delayInsideStep;
    }

    @Override
    public void run() {
        int position = 0;

        while (running && !isInterrupted()) {
            boolean done = list.sortStep(position, delayInsideStep);   // пауза внутри шага теперь в sortStep

            if (!done) {
                break;                     // дошли до конца списка, проход завершён
            }

            steps++;                       // единственный писатель, поэтому volatile
            // достаточно
            position++;
            pause(delayBetweenSteps);
        }

        // если список пустой или из одного элемента, не крутимся впустую
        if (position == 0) {
            pause(Math.max(delayBetweenSteps, 50));
        }
    }

    private void pause(long milliseconds) {
        if (milliseconds <= 0) {
            return;
        }

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            interrupt();
            running = false;
        }
    }

    public long getSteps() {
        return steps;
    }

    public void stopSorting() {
        running = false;
        interrupt();
    }
}