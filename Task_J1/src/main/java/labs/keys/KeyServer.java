package labs.keys;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * Нити сервера:
 *  - главная нить с Selector: принимает клиентов, читает имя, досылает ответ медленным клиентам;
 *  - пул генерирующих нитей: берут имена из genQueue;
 *  - одна передающая нить: берёт готовые запросы из sendQueue и отправляет.
 */
public class KeyServer {

    // один запрос клиента
    static class Request {
        String name;
        SocketChannel channel;
        KeyPair keys;
        X509Certificate cert; // Выпущенный сертификат
        ByteBuffer data; // ответ, который отправляем
    }

    // уже готовый результат для имени
    record Result(KeyPair keys, X509Certificate cert) { }

    private final int port;
    private final int threads;
    private final CryptoService crypto;
    private Selector selector; // объект для управления событиями сетевых каналов

    private final BlockingQueue<String> genQueue = new LinkedBlockingQueue<>();       // имена на генерацию
    private final BlockingQueue<Request> sendQueue = new LinkedBlockingQueue<>();     // готово к отправке
    private final Queue<Request> writeQueue = new ConcurrentLinkedQueue<>();          // не влезло в сокет, дошлёт selector

    private final Object lock = new Object();
    private final Map<String, Result> cache = new HashMap<>();             // имя -> готовые ключи
    private final Map<String, List<Request>> waiting = new HashMap<>();    // имя -> кто ждёт генерацию

    public KeyServer(int port, CryptoService crypto, int threads) {
        this.port = port;
        this.crypto = crypto;
        this.threads = threads;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Использование: KeyServer <port> <issuerName> <threads>");
            System.out.println("Пример: KeyServer 8080 CN=LabIssuer,O=MyUni,C=RU 4");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[2]);
        CryptoService crypto = new CryptoService(args[1], "keys/issuer-private.pem");
        new KeyServer(port, crypto, threads).start();
    }

    public void start() throws Exception {
        selector = Selector.open();

        // Создаёт пул фиксированного размера.
        // Если threads = 4, пул рассчитан на выполнение четырёх задач одновременно
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(this::generateLoop);
        }
        Thread sender = new Thread(this::sendLoop);
        sender.setDaemon(true);
        sender.start();

        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(port), 256); // без адреса = все интерфейсы; 256 = очередь ожидающих подключений
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Сервер запущен, порт " + port + ", нитей генерации: " + threads);

        while (true) {
            selector.select();
            registerWrites();

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                try {
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) accept(server);
                    else if (key.isReadable()) readName(key);
                    else if (key.isWritable()) writeMore(key);
                } catch (Exception e) {
                    System.out.println("Ошибка соединения: " + e);
                    close(key.channel());
                }
            }
        }
    }
    

    private void accept(ServerSocketChannel server) throws IOException {
        SocketChannel client = server.accept();
        if (client == null) return;
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(257)); // 256 байт имени + нулевой байт
        System.out.println("Новое соединение: " + client.getRemoteAddress());
    }

    // читаем имя до нулевого байта
    private void readName(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();

        if (client.read(buf) == -1) {
            client.close();
            return;
        }

        int end = -1;
        for (int i = 0; i < buf.position(); i++) {
            if (buf.get(i) == 0) {
                end = i;
                break;
            }
        }
        if (end == -1) {
            if (!buf.hasRemaining()) client.close(); // слишком длинное имя
            return; // ждём остальные байты
        }

        String name = new String(buf.array(), 0, end, StandardCharsets.US_ASCII);
        if (!name.matches("\\p{Print}+")) { // пустое имя или странные символы
            client.close();
            return;
        }

        key.interestOps(0); // больше ничего от клиента не читаем
        System.out.println("Запрос от \"" + name + "\"");

        Request request = new Request();
        request.name = name;
        request.channel = client;
        newRequest(request);
    }

    // три случая: уже есть в кэше / уже генерируется / надо генерировать
    private void newRequest(Request request) {
        synchronized (lock) {
            Result result = cache.get(request.name);
            if (result != null) {
                request.keys = result.keys();
                request.cert = result.cert();
                sendQueue.add(request);
            } else if (waiting.containsKey(request.name)) {
                waiting.get(request.name).add(request);
            } else {
                List<Request> list = new ArrayList<>();
                list.add(request);
                waiting.put(request.name, list);
                genQueue.add(request.name);
            }
        }
    }

    // работа генерирующей нити
    private void generateLoop() {
        while (true) {
            String name;
            // если очередь пуста нить спит
            try {
                name = genQueue.take();
            } catch (InterruptedException e) {
                return;
            }

            System.out.println("[" + Thread.currentThread().getName() + "] генерирую для \"" + name + "\"");
            try {
                KeyPair keys = CryptoService.generateKeys(8192);
                X509Certificate cert = crypto.makeCertificate(name, keys);

                List<Request> list;
                synchronized (lock) {
                    cache.put(name, new Result(keys, cert));
                    list = waiting.remove(name);
                }
                for (Request r : list) {
                    r.keys = keys;
                    r.cert = cert;
                    sendQueue.add(r);
                }
                System.out.println("Готово для \"" + name + "\", ждали клиентов: " + list.size());
            } catch (Exception e) {
                System.out.println("Ошибка генерации для \"" + name + "\": " + e);
                List<Request> list;
                synchronized (lock) {
                    list = waiting.remove(name);
                }
                for (Request r : list) close(r.channel); // иначе клиенты ждали бы вечно
            }
        }
    }

    // работа передающей нити
    private void sendLoop() {
        while (true) {
            Request request;
            try {
                request = sendQueue.take();
            } catch (InterruptedException e) {
                return;
            }
            send(request);
        }
    }

    private void send(Request r) {
        try {
            byte[] key = CryptoService.toPem(r.keys.getPrivate()).getBytes(StandardCharsets.UTF_8);
            byte[] cert = CryptoService.toPem(r.cert).getBytes(StandardCharsets.UTF_8);

            // формат ответа: [длина ключа][ключ][длина сертификата][сертификат]
            r.data = ByteBuffer.allocate(8 + key.length + cert.length);
            r.data.putInt(key.length).put(key).putInt(cert.length).put(cert).flip();

            r.channel.write(r.data); // пишем сколько влезет
            if (r.data.hasRemaining()) {
                // остаток допишет нить selector
                writeQueue.add(r);
                selector.wakeup();
            } else {
                System.out.println("Отправлено клиенту \"" + r.name + "\"");
                r.channel.close();
            }
        } catch (Exception e) {
            System.out.println("Ошибка отправки \"" + r.name + "\": " + e);
            close(r.channel);
        }
    }

    // включаем OP_WRITE для тех, кому не всё отправили
    private void registerWrites() {
        Request r;
        while ((r = writeQueue.poll()) != null) {
            try {
                SelectionKey key = r.channel.keyFor(selector);
                key.attach(r);
                key.interestOps(SelectionKey.OP_WRITE);
            } catch (Exception e) {
                close(r.channel);
            }
        }
    }

    private void writeMore(SelectionKey key) throws IOException {
        Request r = (Request) key.attachment();
        r.channel.write(r.data);
        if (!r.data.hasRemaining()) {
            System.out.println("Отправлено клиенту \"" + r.name + "\" (досылка)");
            r.channel.close();
        }
    }

    private static void close(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
        }
    }

    
}
