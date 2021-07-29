package com.jumperchuck.escpos.printer;

public class PrintResult {
    /**
     * 数据是否成功发送
     */
    private final boolean sent;
    /**
     * 数据总大小
     */
    private final int totalBytes;

    public PrintResult(boolean sent, int totalBytes) {
        this.sent = sent;
        this.totalBytes = totalBytes;
    }

    public boolean isSent() {
        return sent;
    }

    public int getTotalBytes() {
        return totalBytes;
    }
}
