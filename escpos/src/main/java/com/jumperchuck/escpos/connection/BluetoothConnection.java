package com.jumperchuck.escpos.connection;

import com.gprinter.io.BluetoothPort;
import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public class BluetoothConnection implements PrinterConnection {
    private BluetoothPort portManager;

    private boolean isConnect;

    public BluetoothConnection(String macAddress) {
        portManager = new BluetoothPort(macAddress);
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.BLUETOOTH;
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
        portManager.writeDataImmediately(data);
    }

    @Override
    public void writeData(Vector<Byte> data, int offset, int len) throws IOException {
        portManager.writeDataImmediately(data, offset, len);
    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        return portManager.readData(bytes);
    }
}
