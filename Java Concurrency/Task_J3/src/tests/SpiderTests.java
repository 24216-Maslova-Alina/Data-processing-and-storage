
package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import main.Spider;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpiderTests {

    private static List<String> messages;

    // Один раз запускает обход сервера перед всеми тестами.
    @BeforeClass
    public static void setUp() throws InterruptedException {
        String port = System.getProperty("server.port", "8080");
        Spider spider = new Spider(port);
        messages = spider.crawl();
    }

    // Проверяет, что обход вернул непустой список сообщений.
    @Test
    public void crawlReturnsMessages() {
        assertNotNull(messages);
        assertFalse(messages.isEmpty());
    }

    // Проверяет лексикографическую сортировку сообщений.
    @Test
    public void messagesAreSorted() {
        List<String> sorted = messages.stream()
                .sorted()
                .toList();

        assertEquals(sorted, messages);
    }
}
