package com.jumperchuck.escpos.util;

public class ReadUtils {
    public interface Getter<T> {
        T invoke();
    }

    public static <T> T readSync(Getter<T> getter, int timeout) {
        try {
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            while (currentTime - startTime < timeout) {
                if (getter.invoke() != null) {
                    return getter.invoke();
                }
                Thread.sleep(100);
                currentTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getter.invoke();
    }
}
