package com.jumperchuck.escpos;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.jumperchuck.escpos.connection.BluetoothConnection;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.connection.TcpConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.printer.EscPosPrinter;

public class PrinterManager {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
        SunmiConnection.init(context);
    }

    public static EscPosPrinter.Builder bluetoothPrinter(String macAddress) {
        return new EscPosPrinter.Builder()
            .printerConnection(new BluetoothConnection(macAddress))
            .context(context);
    }

    public static EscPosPrinter.Builder tcpPrinter(String ip, int port) {
        return new EscPosPrinter.Builder()
            .printerConnection(new TcpConnection(ip, port))
            .context(context);
    }

    public static EscPosPrinter.Builder usbPrinter(UsbDevice usbDevice) {
        return new EscPosPrinter.Builder()
            .printerConnection(new UsbConnection(context, usbDevice))
            .context(context);
    }

    public static EscPosPrinter.Builder sunmiPrinter() {
        return new EscPosPrinter.Builder()
            .printerConnection(new SunmiConnection(context))
            .context(context);
    }

    public static void bluetoothScanner() {

    }

    public static void wlanScanner() {

    }
}
