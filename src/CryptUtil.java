import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtil {

    // 默认的AES加密算法，选择NoPadding是为了修改文件时不会改到后面的内容
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/NoPadding";
    private static final IvParameterSpec iv = new IvParameterSpec(new byte[] { (byte) 0x43, (byte) 0xe3, (byte) 0x79,
            (byte) 0xad, (byte) 0xa6, (byte) 0xd6, (byte) 0x6c, (byte) 0x15, (byte) 0xf6, (byte) 0xea, (byte) 0x16,
            (byte) 0x1f, (byte) 0x67, (byte) 0xba, (byte) 0xd2, (byte) 0xb5 });

    /**
     * AES加密算法选择了NoPadding，就要求明文的字节数必须为16的倍数！！
     * 
     * @param key     加密密钥
     * @param content 明文
     * @return
     */
    public static byte[] aesEncrypt(String key, byte[] content, String algorString) {
        try {
            Cipher cipher = Cipher.getInstance(algorString);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"));
            return cipher.doFinal(content);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] aesDecrypt(String key, byte[] content, String algorString) {
        try {
            Cipher cipher = Cipher.getInstance(algorString);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"));
            return cipher.doFinal(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] aesEncrypt(String key, byte[] content) {
        return aesEncrypt(key, content, DEFAULT_CIPHER_ALGORITHM);
    }

    public static byte[] aesDecrypt(String key, byte[] content) {
        return aesDecrypt(key, content, DEFAULT_CIPHER_ALGORITHM);
    }

    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] digest = md.digest();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < digest.length; ++i) {
                stringBuilder.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }

    public static byte[] getBytes(long data) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; ++i) {
            bytes[i] = (byte) ((data >> (8 * i)) & 0xff);
        }
        return bytes;
    }

    public static long getLong(byte[] bytes) {
        long res = 0;
        for (int i = 0; i < 8; ++i) {
            res |= (0xffL << (i * 8)) & ((long) bytes[i] << i * 8);
        }
        return res;
    }

    public static byte[] getBytes(int data) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; ++i) {
            bytes[i] = (byte) ((data >> (8 * i)) & 0xff);
        }
        return bytes;
    }

    public static int getInteger(byte[] bytes) {
        int res = 0;
        for (int i = 0; i < 4; ++i) {
            res |= (0xff << (i * 8)) & ((long) bytes[i] << i * 8);
        }
        return res;
    }

    /**
     * 计算数组的信息熵，即香农熵
     * 
     * @param bytes
     * @return
     */
    public static double calculateShannonEntropy(byte[] bytes) {
        int[] bCnt = new int[256];
        // 赋默认值0
        for (int i = 0; i < 256; ++i) {
            bCnt[i] = 0;
        }
        for (int i = 0; i < bytes.length; ++i) {
            ++bCnt[bytes[i] & 0xff];
        }
        double e = 0;
        for (int i = 0; i < bCnt.length; ++i) {
            if (bCnt[i] == 0)
                continue;
            double p = (double) bCnt[i] / (double) bytes.length;
            e += p * (Math.log(p) / Math.log(2));
        }
        return e * -1;
    }
}
