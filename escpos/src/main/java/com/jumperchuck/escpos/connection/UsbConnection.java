package com.jumperchuck.escpos.connection;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.gprinter.io.UsbPort;
import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public class UsbConnection extends PrinterConnection {
    private UsbPort portManager;

    public UsbConnection(Context context, UsbDevice usbDevice) {
        this.portManager = new UsbPort(context, usbDevice);
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.USB;
    }

    @Override
    public void connect() {
        isConnect = portManager.openPort();
    }

    @Override
    public void disconnect() {
        isConnect = false;
        portManager.closePort();
    }

    @Override
    public void writeData(byte[] data, int off, int len) throws IOException {
        portManager.writeDataImmediately(bytes2VectorByte(data), off, len);
    }

    @Override
    public void writeData(Vector<Byte> data, int off, int len) throws IOException {
        portManager.writeDataImmediately(data, off, len);
    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        return portManager.readData(bytes);
    }
}
