package com.jumperchuck.escpos.connection;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.gprinter.io.UsbPort;
import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public class UsbConnection implements PrinterConnection {
    private UsbPort portManager;

    private boolean isConnect;

    public UsbConnection(Context context, UsbDevice usbDevice) {
        this.portManager = new UsbPort(context, usbDevice);
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.USB;
    }

    @Override
    public void connect() {
        if (isConnect) return;
        isConnect = portManager.openPort();
    }

    @Override
    public void disconnect() {
        portManager.closePort();
        isConnect = false;
    }

    @Override
    public boolean isConnected() {
        return isConnect;
    }

    @Override
    public void writeData(Vector<Byte> data) throws IOException {
        this.portManager.writeDataImmediately(data);
    }

    @Override
    public void writeData(Vector<Byte> data, int offset, int len) throws IOException {
        this.portManager.writeDataImmediately(data, offset, len);
    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        return this.portManager.readData(bytes);
    }
}
