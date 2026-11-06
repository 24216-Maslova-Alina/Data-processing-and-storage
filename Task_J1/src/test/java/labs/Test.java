package labs;

import labs.keys.CryptoService;
import labs.keys.KeyClient;
import labs.keys.KeyServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class Test {

    static final int PORT = 18081;
    static int failed = 0;

    static void check(boolean ok, String text) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + text);
        if (!ok) failed++;
    }

    // Нить для сервера
    static class ServerThread extends Thread {
        KeyServer server;

        ServerThread(KeyServer server) {
            this.server = server;
            setDaemon(true);
        }

        public void run() {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Нить-клиент: просит у сервера ключи и запоминает ответ
    static class Asker extends Thread {
        String name;
        String answer;

        Asker(String name) {
            this.name = name;
        }

        public void run() {
            try {
                answer = ask(name);
            } catch (Exception e) {
                answer = null;
            }
        }
    }

    // Простой клиент: отправляет имя и читает ответ (ключ + сертификат)
    static String ask(String name) throws Exception {
        Socket socket = new Socket("localhost", PORT);
        socket.getOutputStream().write(name.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().write(0);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        String result = readBlock(in) + readBlock(in);
        socket.close();
        return result;
    }

    static String readBlock(DataInputStream in) throws Exception {
        byte[] data = new byte[in.readInt()];
        in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        // ключ издателя во временном файле
        KeyPair issuerKeys = CryptoService.generateKeys(2048);
        Path issuerFile = Files.createTempFile("issuer", ".pem");
        Files.writeString(issuerFile, CryptoService.toPem(issuerKeys.getPrivate()));
        CryptoService crypto = new CryptoService("CN=Test Issuer", issuerFile.toString());

        // 1. Сертификат
        KeyPair clientKeys = CryptoService.generateKeys(2048);
        X509Certificate cert = crypto.makeCertificate("alice", clientKeys);
        check(cert.getSubjectX500Principal().getName().equals("CN=alice"), "сертификат: Subject = имя клиента");
        check(cert.getIssuerX500Principal().getName().equals("CN=Test Issuer"), "сертификат: Issuer = имя издателя");
        try {
            cert.verify(issuerKeys.getPublic());
            check(true, "сертификат подписан ключом издателя");
        } catch (Exception e) {
            check(false, "сертификат подписан ключом издателя");
        }

        // запускаем сервер (2 нити генерации)
        new ServerThread(new KeyServer(PORT, crypto, 2)).start();
        Thread.sleep(1000);

        // 2. Два клиента с одним именем одновременно: ключ один и тот же 
        System.out.println("Ждём генерацию ключа 8192 бита...");
        Asker first = new Asker("alice");
        Asker second = new Asker("alice");
        first.start();
        second.start();
        first.join();
        second.join();
        check(first.answer != null && first.answer.equals(second.answer), "два клиента с одним именем получили одинаковые ключи");

        // 3. Повторный запрос берётся из кэша
        check(ask("alice").equals(first.answer), "повторный запрос: те же ключи из кэша");

        // 4. Настоящий клиент сохраняет файлы
        KeyClient.main(new String[]{"alice", "localhost", String.valueOf(PORT)});
        check(Files.exists(Path.of("clients", "alice.key")) && Files.exists(Path.of("clients", "alice.crt")),
                "KeyClient сохранил файлы .key и .crt");

        // 5. Медленный клиент (--delay 2)
        long start = System.currentTimeMillis();
        KeyClient.main(new String[]{"alice", "localhost", String.valueOf(PORT), "--delay", "2"});
        check(System.currentTimeMillis() - start >= 2000, "--delay 2: клиент подождал 2 секунды");

        // 6. Аварийный клиент (--crash): сервер после него жив
        KeyClient.main(new String[]{"bob", "localhost", String.valueOf(PORT), "--crash"});
        check(ask("alice").equals(first.answer), "после --crash сервер продолжает отвечать");

        // 7. Плохое имя (пустое): сервер закрывает соединение
        check(closedByServer(), "пустое имя: сервер закрывает соединение");

        System.out.println(failed == 0 ? "ВСЕ ТЕСТЫ ПРОШЛИ" : "ПРОВАЛЕНО ТЕСТОВ: " + failed);
        System.exit(failed);
    }

    // Отправляем только нулевой байт (пустое имя) и смотрим, закрыл ли сервер соединение
    static boolean closedByServer() {
        try {
            Socket socket = new Socket("localhost", PORT);
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(0);
            int answer = socket.getInputStream().read();
            socket.close();
            return answer == -1;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
