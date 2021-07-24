package com.jumperchuck.escpos.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanner extends DeviceScanner {
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    listener.onDiscovery(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    listener.onStarted();
                    devices.clear();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    listener.onScanned(devices);
                    break;
            }
        }
    };

    private List<Object> devices = new ArrayList<>();

    private boolean isScanning = false;

    private BluetoothScanner(Builder builder) {
        super(builder);
    }

    @Override
    public void startScan() {
        if (isScanning) {
            return;
        }
        isScanning = true;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, intent);
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void stopScan() {
        if (isScanning) {
            isScanning = false;
            listener.onStopped();
            context.unregisterReceiver(bluetoothReceiver);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    public static class Builder extends DeviceScanner.Builder<Builder> {
        public Builder(Context context) {
            super(context);
        }

        @Override
        public BluetoothScanner build() {
            return new BluetoothScanner(this);
        }
    }
}
