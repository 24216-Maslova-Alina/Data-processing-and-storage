package test.java;

import main.java.Main;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    // запускает Main с нашим "вводом" и возвращает всё, что он напечатал
    private String runMain(String input) throws Exception {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream(input.getBytes("UTF-8")));
        System.setOut(new PrintStream(buffer, true, "UTF-8"));

        try {
            Main.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        return buffer.toString("UTF-8");
    }

    // проверяет весь запуск с LinkedStringList: выбор списка, ввод строк,
    // вывод списка по пустой строке, exit и итоговый размер
    @Test
    public void linkedListFullRun() throws Exception {
        // 1 - выбор списка, потом строки, пустая строка (вывод), exit
        String output = runMain("1\nb\na\n\nexit\n");

        assertTrue(output.contains("Используется LinkedStringList"), output);
        assertTrue(output.contains("Текущий список"), output);
        assertTrue(output.contains("Размер списка: 2"), output);
    }

    // проверяет весь запуск с SynchronizedArrayList: выбор списка, ввод 3 строк, exit
    @Test
    public void synchronizedListFullRun() throws Exception {
        String output = runMain("2\nb\na\nc\nexit\n");

        assertTrue(output.contains("Используется SynchronizedArrayList"), output);
        assertTrue(output.contains("Размер списка: 3"), output);
    }

    // проверяет, что при неверном выборе (5) программа пишет "Неверный выбор!" и завершается
    @Test
    public void wrongChoice() throws Exception {
        String output = runMain("5\n");

        assertTrue(output.contains("Неверный выбор!"), output);
    }
}