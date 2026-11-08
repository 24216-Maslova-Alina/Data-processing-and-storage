package test.java;

import main.java.LinkedStringList;
import main.java.SortWorker;
import main.java.StringList;
import main.java.SynchronizedArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SortWorkerTest {

    // проверяет, что два сортировщика вместе полностью сортируют список (получается abcd)
    // и что шаги посчитались
    @Test
    public void workerSortsList() throws InterruptedException {
        StringList[] lists = {new LinkedStringList(), new SynchronizedArrayList()};

        for (StringList list : lists) {
            list.addFirst("a");
            list.addFirst("c");
            list.addFirst("b");
            list.addFirst("d");

            SortWorker worker1 = new SortWorker(list, 0, 0);
            SortWorker worker2 = new SortWorker(list, 0, 0);
            worker1.start();
            worker2.start();

            Thread.sleep(500);   // ждём, пока отсортируют

            worker1.stopSorting();
            worker2.stopSorting();
            worker1.join();
            worker2.join();

            String result = "";
            for (String s : list) {
                result = result + s;
            }
            assertEquals("abcd", result);
            assertTrue(worker1.getSteps() + worker2.getSteps() > 0);
        }
    }

    // проверяет, что на пустом списке сортировщик не делает шагов и нормально останавливается
    @Test
    public void workerStopsOnEmptyList() throws InterruptedException {
        StringList list = new LinkedStringList();
        SortWorker worker = new SortWorker(list, 10, 0);
        worker.start();

        Thread.sleep(100);
        worker.stopSorting();
        worker.join(1000);

        assertFalse(worker.isAlive());
        assertEquals(0, worker.getSteps());
    }
}