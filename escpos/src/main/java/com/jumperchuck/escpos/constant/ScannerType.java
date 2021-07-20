package com.jumperchuck.escpos.constant;

public enum ScannerType {
    /**
     * 蓝牙扫描
     */
    BLUETOOTH("BLUETOOTH"),

    /**
     * 局域网扫描
     */
    WLAN("WLAN");

    private String name;

    ScannerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
