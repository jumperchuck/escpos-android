package com.jumperchuck.escpos.scanner;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class WlanScanner extends DeviceScanner {
    private AsyncTask<WlanScanner, Object, List<Object>> task;

    private WlanScanner(Builder builder) {
        super(builder);
    }

    @Override
    public void startScan() {
        if (task != null) {
            task.cancel(true);
        }
        task = new CatArpTask(this);
        task.execute();
    }

    @Override
    public void stopScan() {
        if (task != null) {
            listener.onStopped();
            task.cancel(true);
            task = null;
        }
    }

    @Override
    public boolean isScanning() {
        return task != null;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                // To prevent phone of xiaomi return "10.0.2.15"
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress add = addresses.nextElement();
                    if (!add.isLoopbackAddress()) {
                        String hostAddress = add.getHostAddress();
                        boolean isIPv4 = hostAddress.indexOf(":") < 0;
                        if (useIPv4) {
                            if (isIPv4) return hostAddress;
                        } else {
                            if (!isIPv4) {
                                int index = hostAddress.indexOf('%');
                                return index < 0
                                    ? hostAddress.toUpperCase()
                                    : hostAddress.substring(0, index).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static class PingIPTask extends AsyncTask<WlanScanner, Object, List<Object>> {
        private final WlanScanner scanner;

        private PingIPTask(WlanScanner scanner) {
            this.scanner = scanner;
        }

        @Override
        protected void onPreExecute() {
            scanner.listener.onStarted();
        }

        @Override
        protected List<Object> doInBackground(WlanScanner... wlanScanners) {
            List<Object> addresses = new ArrayList<>();
            try {
                String ipAddress = getIPAddress(true);
                String prefix = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
                for (int i = 0; i < 255; i++) {
                    String testIp = prefix + String.valueOf(i);

                    InetAddress address = InetAddress.getByName(testIp);
                    boolean reachable = address.isReachable(500);

                    if (reachable) {
                        addresses.add(address);
                        scanner.listener.onDiscovery(address);
                    }
                }
            } catch (Throwable t){
                t.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Object> addresses) {
            scanner.listener.onScanned(addresses);
        }

        @Override
        protected void onCancelled() {

        }
    }

    public static class CatArpTask extends AsyncTask<WlanScanner, Object, List<Object>> {
        private final WlanScanner scanner;

        private CatArpTask(WlanScanner scanner) {
            this.scanner = scanner;
        }

        @Override
        protected void onPreExecute() {
            scanner.listener.onStarted();
        }

        @Override
        protected List<Object> doInBackground(WlanScanner... wlanScanners) {
            List<Object> addresses = new ArrayList<>();
            try {
                String ipAddress = getIPAddress(true);
                String prefix = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
                DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                DatagramSocket socket = new DatagramSocket();
                int position = 0;
                while (position < 255) {
                    dp.setAddress(InetAddress.getByName(prefix + String.valueOf(position)));
                    socket.send(dp);
                    position++;
                    if (position == 125) { // 分两段掉包，一次性发的话，达到236左右，会耗时3秒左右再往下发
                        socket.close();
                        socket = new DatagramSocket();
                    }
                }
                socket.close();

                Process exec = Runtime.getRuntime().exec("cat proc/net/arp");
                InputStream is = exec.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("00:00:00:00:00:00") && !line.contains("IP")) {
                        String[] split = line.split("\\s+");
                        String ip = split[0];
                        String mac = split[3];
                        InetAddress address = InetAddress.getByName(ip);
                        // boolean reachable = address.isReachable(1000);
                        address.getHostName();
                        address.getHostAddress();
                        addresses.add(address);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Object> addresses) {
            scanner.listener.onScanned(addresses);
        }

        @Override
        protected void onCancelled() {

        }
    }

    public final static class Builder extends DeviceScanner.Builder<Builder> {
        public Builder() {

        }

        @Override
        public WlanScanner build() {
            return new WlanScanner(this);
        }
    }
}
