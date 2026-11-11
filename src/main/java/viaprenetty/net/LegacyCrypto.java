package viaprenetty.net;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

public final class LegacyCrypto {

    private static volatile KeyPair keyPair;

    private LegacyCrypto() {
    }

    public static KeyPair keyPair() {
        KeyPair pair = keyPair;
        if (pair == null) {
            synchronized (LegacyCrypto.class) {
                pair = keyPair;
                if (pair == null) {
                    try {
                        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                        generator.initialize(1024);
                        pair = generator.generateKeyPair();
                        keyPair = pair;
                    } catch (final GeneralSecurityException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return pair;
    }

    public static byte[] decryptRsa(final PrivateKey key, final byte[] data) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static Cipher aes(final int mode, final byte[] secret) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(mode, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
        return cipher;
    }

}
