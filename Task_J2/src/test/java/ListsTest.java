package test.java;

import main.java.LinkedStringList;
import main.java.StringList;
import main.java.SynchronizedArrayList;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class ListsTest {

    // оба списка проверяем одними и теми же тестами
    private StringList[] makeLists() {
        return new StringList[]{new LinkedStringList(), new SynchronizedArrayList()};
    }

    private ArrayList<String> toArrayList(StringList list) {
        ArrayList<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(s);
        }
        return result;
    }

    // проверяет, что новый список пустой: size = 0 и isEmpty = true
    @Test
    public void newListIsEmpty() {
        for (StringList list : makeLists()) {
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
    }

    // проверяет, что addFirst кладёт элемент в начало и размер растёт
    @Test
    public void addFirstPutsElementToStart() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            list.addFirst("b");

            assertEquals(2, list.size());
            assertFalse(list.isEmpty());
            assertEquals("b", toArrayList(list).get(0));
            assertEquals("a", toArrayList(list).get(1));
        }
    }

    // проверяет, что sortStep меняет местами пару, если она стоит неправильно (b, a -> a, b)
    @Test
    public void sortStepSwapsWrongPair() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            list.addFirst("b");   // список: b, a

            assertTrue(list.sortStep(0, 0));

            assertEquals("a", toArrayList(list).get(0));
            assertEquals("b", toArrayList(list).get(1));
        }
    }

    // проверяет, что sortStep не трогает пару, которая уже стоит правильно (a, b)
    @Test
    public void sortStepDoesNotSwapRightPair() {
        for (StringList list : makeLists()) {
            list.addFirst("b");
            list.addFirst("a");   // список: a, b

            assertTrue(list.sortStep(0, 0));

            assertEquals("a", toArrayList(list).get(0));
            assertEquals("b", toArrayList(list).get(1));
        }
    }

    // проверяет, что на пустом списке sortStep возвращает false
    @Test
    public void sortStepReturnsFalseOnEmptyList() {
        for (StringList list : makeLists()) {
            assertFalse(list.sortStep(0, 0));
        }
    }

    // проверяет, что с одним элементом сравнивать не с чем, sortStep возвращает false
    @Test
    public void sortStepReturnsFalseOnOneElement() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            assertFalse(list.sortStep(0, 0));
        }
    }

    // проверяет, что если позиция за пределами списка, sortStep возвращает false
    @Test
    public void sortStepReturnsFalseWhenPositionTooBig() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            list.addFirst("b");
            assertFalse(list.sortStep(5, 0));
        }
    }

    // проверяет, что sortStep работает и с паузой внутри шага (10 мс)
    @Test
    public void sortStepWorksWithDelay() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            list.addFirst("b");
            assertTrue(list.sortStep(0, 10));
        }
    }

    // проверяет, что sortStep с position = 1 сравнивает пару из середины списка, а не с начала
    @Test
    public void sortStepWorksInMiddleOfList() {
        for (StringList list : makeLists()) {
            list.addFirst("c");
            list.addFirst("a");
            list.addFirst("b");
            list.addFirst("z");   // список: z, b, a, c

            assertTrue(list.sortStep(1, 0));   // сравниваем b и a

            assertEquals("a", toArrayList(list).get(1));
            assertEquals("b", toArrayList(list).get(2));
        }
    }

    // проверяет, что итератор - это снимок списка: новые элементы в него не попадают
    @Test
    public void iteratorIsSnapshot() {
        for (StringList list : makeLists()) {
            list.addFirst("a");
            java.util.Iterator<String> it = list.iterator();
            list.addFirst("b");   // добавили уже после создания итератора

            assertEquals("a", it.next());
            assertFalse(it.hasNext());
        }
    }

    // проверяет, что printList выполняется без ошибок
    @Test
    public void printListWorks() {
        for (StringList list : makeLists()) {
            list.addFirst("hello");
            list.printList();   // просто проверяем, что не падает
        }
    }
}
