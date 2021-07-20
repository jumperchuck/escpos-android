package com.jumperchuck.escpos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.jumperchuck.escpos.connection.BluetoothConnection;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.connection.TcpConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.printer.EscPosPrinter;
import com.jumperchuck.escpos.scanner.BluetoothScanner;
import com.jumperchuck.escpos.scanner.WlanScanner;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class PrinterManager {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static void init(Context ctx) {
        PrinterManager.context = ctx.getApplicationContext();
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

    public static SunmiPrinterService sunmiPrinterService() {
        return SunmiConnection.getService();
    }

    public static BluetoothScanner.Builder bluetoothScanner() {
        return new BluetoothScanner.Builder()
            .context(context);
    }

    public static WlanScanner.Builder wlanScanner() {
        return new WlanScanner.Builder()
            .context(context);
    }
}
