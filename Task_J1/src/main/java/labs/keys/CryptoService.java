package labs.keys;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PEMKeyPair;

import java.io.FileReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

// Генерация ключей и выпуск сертификатов
public class CryptoService {

    private final X500Name issuerName;
    private final PrivateKey issuerKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(String issuerName, String issuerKeyFile) throws Exception {
        this.issuerName = new X500Name(issuerName);
        // читаем ключ издателя из файла
        try (PEMParser parser = new PEMParser(new FileReader(issuerKeyFile))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair pair) {            // формат "RSA PRIVATE KEY"
                this.issuerKey = converter.getKeyPair(pair).getPrivate();
            } else if (object instanceof PrivateKeyInfo info) { // формат "PRIVATE KEY" (PKCS#8)
                this.issuerKey = converter.getPrivateKey(info);
            } else {
                throw new IllegalArgumentException("Неизвестный формат ключа в " + issuerKeyFile);
            }
        }
    }

    public static KeyPair generateKeys(int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }

    // Создание сертификата (рассчитан на год)
    public X509Certificate makeCertificate(String name, KeyPair keys) throws Exception {
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, name).build();
        BigInteger serial = new BigInteger(63, random).add(BigInteger.ONE);
        Date from = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
        Date to = new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000);

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(issuerName, serial, from, to, subject, keys.getPublic());
        // Добавляем цифровую подпись к сертификату
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey); // приватный ключ издателя
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    // ключ или сертификат -> текст PEM
    public static String toPem(Object object) throws Exception {
        StringWriter text = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(text)) {
            writer.writeObject(object);
        }
        return text.toString();
    }
}