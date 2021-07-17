package com.jumperchuck.escpos.constant;

public enum PrintWidth {
    /**
     * 58mm打印机
     */
    WIDTH_58(384),

    /**
     * 80mm打印机
     */
    WIDTH_80(576),
    ;

    private int width;

    PrintWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }
}
