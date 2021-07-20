package com.jumperchuck.escpos.scanner;

import android.content.Context;

import java.util.List;

public abstract class DeviceScanner {
    protected Context context;

    protected int timeout;

    protected Listener listener;

    DeviceScanner(Builder builder) {
        this.context = builder.context;
        this.timeout = builder.timeout;
        this.listener = builder.listener;
    }

    public abstract void startScan();

    public abstract void stopScan();

    public abstract boolean isScanning();

    public interface Listener {
        void onStarted();

        void onDiscovery(Object device);

        void onScanned(List<Object> devices);

        void onStopped();

        void onError(Exception e);
    }

    protected abstract static class Builder<T extends Builder> {
        Context context;

        int timeout;

        Listener listener;

        Builder() { }

        public T context(Context context) {
            this.context = context;
            return (T) this;
        }

        public T timeout(int timeout) {
            this.timeout = timeout;
            return (T) this;
        }

        public T listener(Listener listener) {
            this.listener = listener;
            return (T) this;
        }

        public abstract DeviceScanner build();
    }
}
