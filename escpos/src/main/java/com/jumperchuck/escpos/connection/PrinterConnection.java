package com.jumperchuck.escpos.connection;

import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public interface PrinterConnection {
    ConnectType connectType();

    void connect();

    void disconnect();

    boolean isConnected();

    void writeData(Vector<Byte> data) throws IOException;

    void writeData(Vector<Byte> data, int offset, int len) throws IOException;

    int readData(byte[] bytes) throws IOException;
}
