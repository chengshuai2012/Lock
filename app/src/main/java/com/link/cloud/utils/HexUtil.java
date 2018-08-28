package com.link.cloud.utils;

import java.security.SecureRandom;

public class HexUtil {
    public HexUtil() {
    }

    public static String bytesToHexString(byte[] var0) {
        StringBuffer var1 = new StringBuffer(var0.length);

        for(int var3 = 0; var3 < var0.length; ++var3) {
            String var2;
            if((var2 = Integer.toHexString(255 & var0[var3])).length() < 2) {
                var1.append(0);
            }

            var1.append(var2.toUpperCase());
        }

        return var1.toString();
    }

    public static byte[] generateSeedKey() {
        SecureRandom var0 = new SecureRandom();
        byte[] var1 = new byte[16];
        var0.nextBytes(var1);

        for(int var2 = 0; var2 < 16; ++var2) {
            var1[var2] = (byte)(var1[var2] & 127);
        }

        return var1;
    }

    public static byte[] hexStringToByte(String var0) {
        int var1;
        byte[] var2 = new byte[var1 = var0.length() / 2];
        char[] var6 = var0.toCharArray();

        for(int var3 = 0; var3 < var1; ++var3) {
            int var4 = var3 * 2;
            char var5 = var6[var4];
            int var10002 = (byte)"0123456789ABCDEF".indexOf(var5) << 4;
            var5 = var6[var4 + 1];
            var2[var3] = (byte)(var10002 | (byte)"0123456789ABCDEF".indexOf(var5));
        }

        return var2;
    }
}
