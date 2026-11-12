package labs.keys;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

// Запускается один раз: создаёт ключ издателя в папке keys
public class IssuerKeys {

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("keys"));
        System.out.println("Генерирую ключ издателя, подождите...");
        KeyPair keys = CryptoService.generateKeys(4096);

        try (FileWriter file = new FileWriter("keys/issuer-private.pem")) {
            file.write(CryptoService.toPem(keys.getPrivate()));
        }
        try (FileWriter file = new FileWriter("keys/issuer-public.pem")) {
            file.write(CryptoService.toPem(keys.getPublic()));
        }
        System.out.println("Готово: папка keys");
    }
}
