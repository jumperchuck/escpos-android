package com.jumperchuck.escpos.constant;

public enum PrinterStatus {
    /**
     * 正常
     */
    NORMAL(0, "normal", 18, 0),

    /**
     * 打印机连接中
     */
    CONNECTING(1, "printer connecting"),

    /**
     * 打印机未连接或未通电
     */
    DISCONNECTED(2, "printer is not connected or not on the power"),

    /**
     * 打印机报错
     */
    ERROR(3, "printer error", 0x40, 0x80),

    /**
     * 开盖
     */
    COVER_OPEN(4, "cover open", 0x04, 0x01),

    /**
     * 走纸中
     */
    FEEDING(5, "feeding", 0x08, 0x20),

    /**
     * 缺纸
     */
    PAPER_OUT(6, "out of paper", 0x20, 0x04),

    /**
     * 纸异常(卡纸)
     */
    PAPER_ERROR(7, "out of paper", 0, 0x02),

    /**
     * 缺碳带
     */
    CARBON_OUT(8, "out of carbon", 0, 0x08),

    /**
     * 切刀错误
     */
    KNIFE_ERROR(9, "knife error", 0x08, 0),

    /**
     * 打印头过热
     */
    OVER_HEATING(10, "print head overheating", 0x40, 0),

    /**
     * 黑标错误
     */
    BLACK_LABEL_ERROR(11, "black label error"),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR(-1, "unknown error"),
    ;

    private int code;

    private String message;

    private int esc;

    private int tsc;

    private int cpcl;

    PrinterStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    PrinterStatus(int code, String message, int esc) {
        this.code = code;
        this.message = message;
        this.esc = esc;
    }

    PrinterStatus(int code, String message, int esc, int tsc) {
        this.code = code;
        this.message = message;
        this.esc = esc;
        this.tsc = tsc;
    }

    PrinterStatus(int code, String message, int esc, int tsc, int cpcl) {
        this.code = code;
        this.message = message;
        this.esc = esc;
        this.tsc = tsc;
        this.cpcl = cpcl;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getEsc() {
        return esc;
    }

    public int getTsc() {
        return tsc;
    }

    public int getCpcl() {
        return cpcl;
    }
}
