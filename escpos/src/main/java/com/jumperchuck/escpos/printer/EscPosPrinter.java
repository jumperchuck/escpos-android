package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.content.Intent;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.ConnectType;
import com.jumperchuck.escpos.constant.PrintWidth;
import com.jumperchuck.escpos.constant.PrinterStatus;

public abstract class EscPosPrinter {
    protected int id;

    protected String name;

    protected Context context;

    protected PrinterConnection connection;

    protected int printWidth;

    byte feedBeforeCut;

    protected Listener listener;

    EscPosPrinter(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.context = builder.context;
        this.connection = builder.connection;
        this.printWidth = builder.printWidth;
        this.feedBeforeCut = builder.feedBeforeCut;
        this.listener = builder.listener;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public ConnectType getConnectType() {
        return connection.connectType();
    }

    /**
     * 连接
     */
    public abstract void open();

    /**
     * 关闭
     */
    public abstract void close();

    /**
     * 发送数据打印
     */
    public abstract PrinterStatus print(Paper paper);

    /**
     * 检测打印机状态
     */
    public abstract PrinterStatus getPrinterStatus();

    /**
     * 发送广播
     */
    protected void sendStatusBroadcast(PrinterStatus printerStatus) {
        Intent intent = new Intent("PRINTER_STATUS");
        intent.putExtra("id", getId());
        intent.putExtra("name", getName());
        intent.putExtra("connectType", connection.connectType().getName());
        intent.putExtra("code", printerStatus.getCode());
        intent.putExtra("message", printerStatus.getMessage());
        context.sendBroadcast(intent);
    }

    public interface Listener {
        void onOpening();

        void onOpened();

        void onPrinted(Paper paper, PrinterStatus printerStatus);

        void onClosed();

        void onError(Exception e);
    }

    public abstract static class Builder<T extends Builder> {

        int id;

        String name;

        Context context;

        PrinterConnection connection;

        int printWidth = PrintWidth.WIDTH_80.getWidth();

        byte feedBeforeCut;

        EscPosPrinter.Listener listener;

        Builder(Context context) {
            this.context = context;
        }

        public T id(int id) {
            this.id = id;
            return (T) this;
        }

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T context(Context context) {
            this.context = context;
            return (T) this;
        }

        public T connection(PrinterConnection connection) {
            this.connection = connection;
            return (T) this;
        }

        public T printWidth(int printWidth) {
            this.printWidth = printWidth;
            return (T) this;
        }

        public T feedBeforeCut(byte feedBeforeCut) {
            this.feedBeforeCut = feedBeforeCut;
            return (T) this;
        }

        public T listener(EscPosPrinter.Listener listener) {
            this.listener = listener;
            return (T) this;
        }

        public abstract EscPosPrinter build();
    }
}
