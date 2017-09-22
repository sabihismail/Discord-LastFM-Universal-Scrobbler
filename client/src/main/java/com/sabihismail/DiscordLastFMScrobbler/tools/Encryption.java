/*
 * Code snippet retrieved and edited from Stack Overflow:
 * Question: https://stackoverflow.com/questions/1132567
 * Answer: https://stackoverflow.com/a/1133815
 * Author: Johannes Brodwall (https://stackoverflow.com/users/27658/johannes-brodwall)
 * Changes: Creation of randomized salt rather than a salt that is constant.
 *
 * This work is licensed under a Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * Licence: https://creativecommons.org/licenses/by-sa/3.0/
 */

package com.sabihismail.DiscordLastFMScrobbler.tools;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;

/**
 * This class manages the encryption and decryption of all sensitive data including the Last.FM password, Last.FM
 * session key, and the Discord token.
 *
 * @since 1.0
 */
public class Encryption {
    private static String password = "d%HVBf8&mUY$>KK&";

    private static int iterationCount = 4000;
    private static int keyLength = 128;

    /**
     * Generates a random salt composed of numbers entirely.
     *
     * @return A {@link Byte} array of the generated salt.
     */
    public static byte[] generateRandomSalt() {
        Random random = new Random();
        StringBuilder salt = new StringBuilder();
        while (salt.length() <= 10) {
            salt.append(random.nextInt());
        }

        return salt.toString().getBytes();
    }

    public static SecretKeySpec createSecretKey(byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    public static String encrypt(String property, SecretKeySpec key) throws GeneralSecurityException, UnsupportedEncodingException {
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters parameters = pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = pbeCipher.doFinal(property.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    public static String decrypt(String string, SecretKeySpec key) throws GeneralSecurityException, IOException {
        String iv = string.split(":")[0];
        String property = string.split(":")[1];
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(base64Decode(iv)));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] base64Decode(String property) throws IOException {
        return Base64.getDecoder().decode(property);
    }
}
