package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.content.Intent;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.constant.PrintWidth;
import com.jumperchuck.escpos.constant.PrinterCommand;
import com.jumperchuck.escpos.constant.PrinterStatus;

public abstract class EscPosPrinter {
    private int id;

    private String name;

    private Context context;

    protected PrinterConnection printerConnection;

    protected PrinterCommand printerCommand;

    protected int printWidth;

    protected Listener listener;

    public EscPosPrinter(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.context = builder.context;
        this.printerConnection = builder.printerConnection;
        this.printerCommand = builder.printerCommand;
        this.printWidth = builder.printWidth;
        this.listener = builder.listener;
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
        intent.putExtra("connectType", printerConnection.connectType());
        intent.putExtra("code", printerStatus.getCode());
        intent.putExtra("message", printerStatus.getMessage());
        getContext().sendBroadcast(intent);
    }

    public interface Listener {
        void onPrinted(Paper paper, PrinterStatus printerStatus);

        void onError(PrinterStatus printerStatus);
    }

    public static final class Builder {

        int id;

        String name;

        Context context;

        PrinterConnection printerConnection;

        PrinterCommand printerCommand;

        int printWidth = PrintWidth.WIDTH_80.getWidth();

        EscPosPrinter.Listener listener;

        public EscPosPrinter.Builder id(int id) {
            this.id = id;
            return this;
        }

        public EscPosPrinter.Builder name(String name) {
            this.name = name;
            return this;
        }

        public EscPosPrinter.Builder context(Context context) {
            this.context = context;
            return this;
        }

        public EscPosPrinter.Builder printerConnection(PrinterConnection printerConnection) {
            this.printerConnection = printerConnection;
            return this;
        }

        public EscPosPrinter.Builder printerCommand(PrinterCommand printerCommand) {
            this.printerCommand = printerCommand;
            return this;
        }

        public EscPosPrinter.Builder printWidth(int printWidth) {
            this.printWidth = printWidth;
            return this;
        }

        public EscPosPrinter.Builder listener(EscPosPrinter.Listener listener) {
            this.listener = listener;
            return this;
        }

        public EscPosPrinter build() {
            if (printerConnection instanceof SunmiConnection) {
                return new SunmiPrinter(this);
            }
            return new GeneralPrinter(this);
        }
    }
}
