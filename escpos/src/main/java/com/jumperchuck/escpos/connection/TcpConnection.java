package com.jumperchuck.escpos.connection;

import com.jumperchuck.escpos.constant.ConnectType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpConnection extends PrinterConnection {
    private static final String TAG = TcpConnection.class.getSimpleName();

    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;
    private String ip;
    private int port;
    private int timeout = 4000;
    private int soTimeout = 0;

    public TcpConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    @Override
    public ConnectType connectType() {
        return ConnectType.TCP;
    }

    @Override
    public void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.setSoTimeout(soTimeout);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            isConnect = true;
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    @Override
    public void disconnect() {
        isConnect = false;
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeData(byte[] data, int off, int len) throws IOException {
        if (outputStream != null && data.length > 0) {
            outputStream.write(data, off, len);
            outputStream.flush();
        }
    }

    @Override
    public int readData(byte[] bytes) throws IOException {
        return inputStream.read(bytes);
    }
}
