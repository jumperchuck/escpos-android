package com.jumperchuck.escpos.connection;

import com.gprinter.io.EthernetPort;
import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public class TcpConnection implements PrinterConnection {
    private EthernetPort portManager;

    private boolean isConnect;

    public TcpConnection(String ip, int port) {
        this.portManager = new EthernetPort(ip, port);
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.TCP;
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
