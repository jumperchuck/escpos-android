package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.webkit.WebView;

import com.jumperchuck.escpos.command.FactoryCommander;
import com.jumperchuck.escpos.command.PrinterCommander;
import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.ConnectType;
import com.jumperchuck.escpos.constant.PrintWidth;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.HtmlUtils;

import java.io.IOException;
import java.util.Vector;

public abstract class EscPosPrinter extends PrinterConnection {
    protected int id;

    protected String name;

    protected Context context;

    protected PrinterConnection connection;

    protected PrinterCommander commander;

    protected Listener listener;

    protected int printWidth;

    protected byte feedBeforeCut;

    protected int timeout;

    protected int soTimeout;

    EscPosPrinter(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.context = builder.context;
        this.connection = builder.connection;
        this.commander = builder.commander;
        this.listener = builder.listener;
        this.printWidth = builder.printWidth;
        this.feedBeforeCut = builder.feedBeforeCut;
        this.timeout = builder.timeout;
        this.soTimeout = builder.soTimeout;
        if (this.commander == null) {
            this.commander = new FactoryCommander();
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Context getContext() {
        return context;
    }

    public int getPrintWidth() {
        return printWidth;
    }

    public byte getFeedBeforeCut() {
        return feedBeforeCut;
    }

    @Override
    public ConnectType connectType() {
        return connection.connectType();
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void writeData(Vector<Byte> data, int off, int len) throws IOException {
        try {
            connection.writeData(data, off, len);
        } catch (IOException e) {
            // 异常中断
            disconnect();
            throw e;
        }
    }

    @Override
    public void writeData(byte[] data, int off, int len) throws IOException {
        try {
            connection.writeData(data, off, len);
        } catch (IOException e) {
            // 异常中断
            disconnect();
            throw e;
        }
    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        try {
            return connection.readData(bytes);
        } catch (IOException e) {
            // 异常中断
            disconnect();
            throw e;
        }
    }

    /**
     * 发送数据打印
     */
    public abstract PrintResult print(Paper paper);

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
        if (listener != null) {
            listener.onStatusChanged(printerStatus);
        }
    }

    public interface Listener {
        void onStatusChanged(PrinterStatus printerStatus);

        void onPrinted(Paper paper, PrinterStatus printerStatus);

        void onError(Exception e);
    }

    public abstract static class Builder<T extends Builder> {
        int id;

        String name;

        Context context;

        PrinterConnection connection;

        PrinterCommander commander;

        Listener listener;

        int printWidth = PrintWidth.WIDTH_80.getWidth();

        byte feedBeforeCut = 1;

        int timeout = 6000;

        int soTimeout = 4000;

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

        public T commander(PrinterCommander commander) {
            this.commander = commander;
            return (T) this;
        }

        public T listener(Listener listener) {
            this.listener = listener;
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

        public T timeout(int timeout) {
            this.timeout = timeout;
            return (T) this;
        }

        public T soTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
            return (T) this;
        }

        public abstract EscPosPrinter build();
    }
}
