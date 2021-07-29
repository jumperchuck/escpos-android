package com.jumperchuck.escpos.connection;

import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.util.Vector;

public abstract class PrinterConnection {
    public abstract ConnectType connectType();

    public abstract void connect();

    public abstract void disconnect();

    public abstract boolean isConnected();

    public abstract void writeData(byte[] data, int off, int len) throws IOException;

    public abstract void writeData(Vector<Byte> data, int off, int len) throws IOException;

    public void writeData(byte[] data) throws IOException {
        writeData(data, 0, data.length);
    }

    public void writeData(Vector<Byte> data) throws IOException {
        writeData(vectorByteToBytes(data), 0, data.size());
    }

    public abstract int readData(byte[] bytes) throws IOException;

    public static byte[] vectorByteToBytes(Vector<Byte> data) {
        byte[] bytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            bytes[i] = data.get(i);
        }
        return bytes;
    }

    public static Vector<Byte> bytesToVectorByte(byte[] data) {
        Vector<Byte> vectorByte = new Vector<>();
        for (int i = 0; i < data.length; i++) {
            vectorByte.add(Byte.valueOf(data[i]));
        }
        return vectorByte;
    }
}
