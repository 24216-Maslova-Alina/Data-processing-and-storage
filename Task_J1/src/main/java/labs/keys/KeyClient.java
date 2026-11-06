package labs.keys;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Клиент отправляет имя, ожидает ответ, 
// читает два блока и сохраняет их в файлы.
public class KeyClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Использование: KeyClient <name> <host> <port> [--delay <сек>] [--crash]");
            return;
        }
        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        int delay = 0;
        boolean crash = false;

        for (int i = 3; i < args.length; i++) {
            if (args[i].equals("--delay")) {
                delay = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--crash")) {
                crash = true;
            }
        }

        // Конструктор Socket(host, port) обычно блокирует текущий поток 
        // до установления соединения или возникновения ошибки.
        try (Socket socket = new Socket(host, port)) {
            // отправляем имя и нулевой байт серверу.
            OutputStream out = socket.getOutputStream();
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();
            System.out.println("Имя отправлено: " + name);

            if (crash) {
                System.out.println("Аварийное завершение: закрываем соединение, ответ не читаем");
                return;
            }
            // Клиент просто не читает ответ в течение указанного времени. 
            if (delay > 0) {
                System.out.println("Ждём " + delay + " сек...");
                Thread.sleep(delay * 1000L);
            }

            DataInputStream in = new DataInputStream(socket.getInputStream());
            String key = readBlock(in);
            String cert = readBlock(in);

            // в имени файла оставляем только безопасные символы
            String fileName = name.replaceAll("[^A-Za-z0-9._-]", "_");
            Files.createDirectories(Path.of("clients"));
            Files.writeString(Path.of("clients", fileName + ".key"), key);
            Files.writeString(Path.of("clients", fileName + ".crt"), cert);
            System.out.println("Сохранено: clients/" + fileName + ".key и .crt");
        }
    }

    // читаем блок: сначала 4 байта длины, потом данные
    private static String readBlock(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
