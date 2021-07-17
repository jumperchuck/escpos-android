package com.jumperchuck.escpos.scanner;

public interface DeviceScanner {
    void startScan();

    void stopScan();

    interface Listener {
        void onScanStart();

        void onScanStop();

        void onScanDiscovery();
    }
}
