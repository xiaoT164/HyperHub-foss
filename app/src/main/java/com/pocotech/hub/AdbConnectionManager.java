package com.pocotech.hub;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

public class AdbConnectionManager extends AbsAdbConnectionManager {

    private static final String TAG = "AdbConnectionManager";

    private static final String KEY_FILE = "adb_private.key";
    private static final String CERT_FILE = "adb_cert.pem";

    private static AdbConnectionManager INSTANCE;

    private final PrivateKey privateKey;
    private final Certificate certificate;

    
    public static synchronized AdbConnectionManager getInstance(Context context) throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new AdbConnectionManager(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private AdbConnectionManager(Context context) throws Exception {
        super();
        setApi(Build.VERSION.SDK_INT);

        File keyFile = new File(context.getFilesDir(), KEY_FILE);
        File certFile = new File(context.getFilesDir(), CERT_FILE);

        PrivateKey loadedKey = null;
        Certificate loadedCert = null;

        if (keyFile.exists() && certFile.exists()) {
            try {
                loadedKey = loadPrivateKey(keyFile);
                loadedCert = loadCertificate(certFile);
            } catch (Exception e) {
                Log.w(TAG, "Не удалось загрузить сохранённый ключ, генерируем новый", e);
                loadedKey = null;
                loadedCert = null;
            }
        }

        if (loadedKey == null || loadedCert == null) {
            KeyPair keyPair = generateKeyPair();
            Certificate cert = generateCertificate(keyPair);
            saveKeyAndCert(keyFile, certFile, keyPair.getPrivate(), cert);
            loadedKey = keyPair.getPrivate();
            loadedCert = cert;
        }

        this.privateKey = loadedKey;
        this.certificate = loadedCert;
    }

    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    protected Certificate getCertificate() {
        return certificate;
    }

    @Override
    protected String getDeviceName() {
        return "PocoTechHub";
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"));
        return generator.generateKeyPair();
    }

    
    private static Certificate generateCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 24L * 60 * 60 * 1000); // на день в прошлое, чтобы избежать проблем с часовыми поясами
        Date notAfter = new Date(now + 20L * 365 * 24 * 60 * 60 * 1000); // ~20 лет

        X500Name subject = new X500Name("CN=PocoTechHub");
        BigInteger serial = BigInteger.valueOf(now);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, // issuer = subject (самоподписанный)
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA512withRSA")
                .build(keyPair.getPrivate());

        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private static void saveKeyAndCert(File keyFile, File certFile, PrivateKey key, Certificate cert) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(key.getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream(certFile)) {
            fos.write(cert.getEncoded());
        }
    }

    private static PrivateKey loadPrivateKey(File keyFile) throws Exception {
        byte[] bytes = readAll(keyFile);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    private static Certificate loadCertificate(File certFile) throws Exception {
        byte[] bytes = readAll(certFile);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new ByteArrayInputStream(bytes));
    }

    private static byte[] readAll(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }
}
