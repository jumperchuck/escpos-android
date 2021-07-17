package com.jumperchuck.escpos.constant;

public enum PrinterCommand {
    /**
     * ESC指令 查询打印机状态
     */
    ESC(new byte[]{0x10, 0x04, 0x02}),

    /**
     * TSC指令 查询打印机状态
     */
    TSC(new byte[]{0x1b, '!', '?'}),

    /**
     * CPCL指令 查询打印机状态
     */
    CPCL(new byte[]{0x1b, 0x68}),
    ;

    private byte[] checkCommand;

    PrinterCommand(byte[] checkCommand) {
        this.checkCommand = checkCommand;
    }

    public byte[] getCheckCommand() {
        return checkCommand;
    }
}
