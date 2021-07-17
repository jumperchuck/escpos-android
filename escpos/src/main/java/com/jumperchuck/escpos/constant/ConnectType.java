package com.jumperchuck.escpos.constant;

public enum ConnectType {
    /**
     * 蓝牙连接
     */
    BLUETOOTH("BLUETOOTH"),

    /**
     * USB连接
     */
    USB("USB"),

    /**
     * tcp/ip连接
     */
    TCP("TCP"),

    /**
     * 商米内置打印机
     */
    SUNMI("SUNMI");

    private String name;

    ConnectType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
