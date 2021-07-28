package com.jumperchuck.escpos.connection;

import android.content.Context;

import com.jumperchuck.escpos.constant.ConnectType;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.io.IOException;
import java.util.Vector;

public class SunmiConnection extends PrinterConnection {
    private static SunmiPrinterService service;

    private static InnerPrinterCallback callback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            SunmiConnection.service = service;
        }

        @Override
        protected void onDisconnected() {
            SunmiConnection.service = null;
        }
    };

    public static void init(Context context) {
        try {
            InnerPrinterManager.getInstance().bindService(context, callback);
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
    }

    public static SunmiPrinterService getService() {
        return service;
    }

    public SunmiConnection(Context context) {

    }

    @Override
    public ConnectType connectType() {
        return ConnectType.SUNMI;
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return service != null;
    }

    @Override
    public void writeData(byte[] data, int off, int len) throws IOException {

    }

    @Override
    public void writeData(Vector<Byte> data, int off, int len) throws IOException {

    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        return -1;
    }
}
