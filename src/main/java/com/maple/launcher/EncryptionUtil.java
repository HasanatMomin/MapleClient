package com.maple.launcher;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGO = "AES";

    private static SecretKeySpec getHardwareKey() throws Exception {
        String hwId = getCommandOutput("wmic baseboard get serialnumber") + getCommandOutput("wmic cpu get processorid");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(hwId.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(key, 16), ALGO);
    }

    private static String getCommandOutput(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("SerialNumber") && !trimmed.equalsIgnoreCase("ProcessorId")) {
                        output.append(trimmed);
                    }
                }
            }
        } catch (Exception e) { return "MapleFallbackKey786"; }
        return output.toString();
    }

    public static String encrypt(String data) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, getHardwareKey());
        return Base64.getEncoder().encodeToString(c.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, getHardwareKey());
        return new String(c.doFinal(Base64.getDecoder().decode(encryptedData)), StandardCharsets.UTF_8);
    }
}