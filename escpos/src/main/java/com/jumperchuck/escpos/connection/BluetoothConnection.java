
package com.jumperchuck.escpos.connection;

import com.gprinter.io.BluetoothPort;
import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public class BluetoothConnection extends PrinterConnection {
    private BluetoothPort portManager;

    public BluetoothConnection(String macAddress) {
        this.portManager = new BluetoothPort(macAddress);
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.BLUETOOTH;
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