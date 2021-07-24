package com.jumperchuck.escpos.connection;

import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class PrinterConnection {
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean isConnect;

    public abstract ConnectType connectType();

    public abstract void connect();

    public abstract void disconnect();

    public boolean isConnected() {
        return isConnect;
    }

    public abstract void writeData(byte[] data, int off, int len) throws IOException;

    public abstract int readData(byte[] bytes) throws IOException;

    public void writeData(byte[] data) throws IOException {
        writeData(data, 0, data.length);
    }

    public void writeData(Vector<Byte> data) throws IOException {
        writeData(data, 0, data.size());
    }

    public void writeData(Vector<Byte> data, int off, int len) throws IOException {
        writeData(vectorByteToBytes(data), off, len);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public static byte[] vectorByteToBytes(Vector<Byte> data) {
        byte[] bytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            bytes[i] = data.get(i);
        }
        return bytes;
    }

    public static Vector<Byte> bytes2VectorByte(byte[] data) {
        Vector<Byte> vectorByte = new Vector<>();
        for (int i = 0; i < data.length; i++) {
            vectorByte.add(Byte.valueOf(data[i]));
        }
        return vectorByte;
    }
}
