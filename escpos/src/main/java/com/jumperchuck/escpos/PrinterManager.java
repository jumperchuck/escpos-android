package com.jumperchuck.escpos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.jumperchuck.escpos.connection.BluetoothConnection;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.connection.TcpConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.printer.GeneralPrinter;
import com.jumperchuck.escpos.printer.SunmiPrinter;
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

    public static GeneralPrinter.Builder bluetoothPrinter(String macAddress) {
        return new GeneralPrinter.Builder(context)
            .connection(new BluetoothConnection(macAddress));
    }

    public static GeneralPrinter.Builder tcpPrinter(String ip, int port) {
        return new GeneralPrinter.Builder(context)
            .connection(new TcpConnection(ip, port));
    }

    public static GeneralPrinter.Builder usbPrinter(UsbDevice usbDevice) {
        return new GeneralPrinter.Builder(context)
            .connection(new UsbConnection(context, usbDevice));
    }

    public static SunmiPrinter.Builder sunmiPrinter() {
        return new SunmiPrinter.Builder(context)
            .connection(new SunmiConnection(context));
    }

    public static SunmiPrinterService sunmiService() {
        return SunmiConnection.getService();
    }

    public static BluetoothScanner.Builder bluetoothScanner() {
        return new BluetoothScanner.Builder(context);
    }

    public static WlanScanner.Builder wlanScanner() {
        return new WlanScanner.Builder(context);
    }
}
