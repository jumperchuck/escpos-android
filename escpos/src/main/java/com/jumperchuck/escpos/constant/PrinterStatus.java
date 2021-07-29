package com.jumperchuck.escpos.constant;

public enum PrinterStatus {
    /**
     * 正常，检测打印机实时状态
     */
    NORMAL(0, "normal"),

    /**
     * 打印机连接中
     */
    CONNECTING(1, "printer connecting"),

    /**
     * 打印机连接成功，但未检测打印机实时状态
     */
    CONNECTED(2, "printer connected"),

    /**
     * 打印机连接超时
     */
    CONNECT_TIMEOUT(3, "printer connect timeout"),

    /**
     * 打印机未连接或未通电
     */
    DISCONNECTED(4, "printer is not connected"),

    /**
     * 打印机读取数据超时
     */
    READ_TIMEOUT(5, "read status timeout"),

    /**
     * 打印机报错
     */
    ERROR(6, "printer error"),

    /**
     * 开盖
     */
    COVER_OPEN(7, "cover open"),

    /**
     * 走纸中
     */
    FEEDING(8, "feeding"),

    /**
     * 缺纸
     */
    PAPER_OUT(9, "out of paper"),

    /**
     * 纸异常(卡纸)
     */
    PAPER_ERROR(10, "out of paper"),

    /**
     * 缺碳带
     */
    CARBON_OUT(11, "out of carbon"),

    /**
     * 切刀错误
     */
    KNIFE_ERROR(12, "knife error"),

    /**
     * 打印头过热
     */
    OVER_HEATING(13, "print head overheating"),

    /**
     * 黑标错误
     */
    BLACK_LABEL_ERROR(14, "black label error"),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR(-1, "unknown error"),
    ;

    private int code;

    private String message;

    PrinterStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
