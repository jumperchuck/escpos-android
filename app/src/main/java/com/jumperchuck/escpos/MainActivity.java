package com.jumperchuck.escpos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.ResourceUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrintWidth;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.printer.Paper;
import com.jumperchuck.escpos.printer.EscPosPrinter;
import com.jumperchuck.escpos.printer.PrintResult;
import com.jumperchuck.escpos.scanner.DeviceScanner;

import java.net.InetAddress;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvPrinterInfo;
    private TextView tvPrinterStatus;
    private TextView tvPrinterConnected;
    private Button btSelectBluetooth;
    private Button btSelectWlan;
    private Button btSelectSunmi;
    private Button btStatus;
    private Button btPrint;

    private AlertDialog alertDialog;
    private AlertDialog.Builder alertBuilder;
    private EscPosPrinter printer;
    private DeviceScanner scanner;
    private DeviceScanner.Listener scannerListener = new DeviceScanner.Listener() {
        @Override
        public void onStarted() {
            ToastUtils.showLong("设备扫描中...");
        }

        @Override
        public void onDiscovery(Object device) {
            if (device instanceof InetAddress) {
                ToastUtils.showLong("发现局域网设备: " +
                    ((InetAddress) device).getHostName() +
                    ((InetAddress) device).getHostAddress());
            } else if (device instanceof BluetoothDevice) {
                ToastUtils.showLong("发现蓝牙设备: " +
                    ((BluetoothDevice) device).getType() +
                    ((BluetoothDevice) device).getName() +
                    ((BluetoothDevice) device).getAddress());
            }
        }

        @Override
        public void onScanned(List<Object> devices) {
            alertBuilder.setMessage(null);
            alertBuilder.setSingleChoiceItems(
                devices.stream()
                    .map(item -> {
                        if (item instanceof InetAddress) {
                            return ((InetAddress) item).getHostName() + " / " +
                                ((InetAddress) item).getHostAddress();
                        } else if (item instanceof BluetoothDevice) {
                            return ((BluetoothDevice) item).getType() + " / " +
                                ((BluetoothDevice) item).getName() + " / " +
                                ((BluetoothDevice) item).getAddress();
                        }
                        return "";
                    })
                    .toArray(String[]::new),
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Object item = devices.get(i);
                        if (item instanceof InetAddress) {
                            showInputTcpPrinter(((InetAddress) item).getHostAddress(), "9100");
                        } else if (item instanceof BluetoothDevice) {
                            String name = ((BluetoothDevice) item).getName();
                            String address = ((BluetoothDevice) item).getAddress();
                            initPrinter(PrinterManager.bluetoothPrinter(address).name(name + " / " + address));
                        }
                        alertDialog.cancel();
                    }
                }
            );
            if (alertDialog != null) {
                alertDialog.cancel();
            }
            alertDialog = alertBuilder.show();
        }

        @Override
        public void onStopped() {
            ToastUtils.showLong("停止设备扫描!");
        }

        @Override
        public void onError(Exception e) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPrinterInfo = findViewById(R.id.tv_printer_info);
        tvPrinterStatus = findViewById(R.id.tv_printer_status);
        tvPrinterConnected = findViewById(R.id.tv_printer_connected);
        btStatus = findViewById(R.id.bt_status);
        btPrint = findViewById(R.id.bt_print);
        btSelectBluetooth = findViewById(R.id.bt_select_bluetooth);
        btSelectWlan = findViewById(R.id.bt_select_wlan);
        btSelectSunmi = findViewById(R.id.bt_select_sunmi);

        btSelectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectBluetoothPrinter();
            }
        });
        btSelectWlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectWlanPrinter();
            }
        });
        btSelectSunmi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initPrinter(PrinterManager
                    .sunmiPrinter()
                    .name("SUNMI")
                    .printWidth(PrintWidth.WIDTH_58.getWidth())
                );
            }
        });
        btStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkStatus();
            }
        });
        btPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                print();
            }
        });

        alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                scanner.stopScan();
            }
        });
        alertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                scanner.stopScan();
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        }, 100);
    }

    public void showSelectBluetoothPrinter() {
        scanner = PrinterManager.bluetoothScanner()
            .listener(scannerListener)
            .build();
        scanner.startScan();
        alertBuilder.setTitle("选择蓝牙打印机");
        alertBuilder.setMessage("扫描中...");
        if (alertDialog != null) {
            alertDialog.cancel();
        }
        alertDialog = alertBuilder.show();
    }

    public void showSelectWlanPrinter() {
        scanner = PrinterManager.wlanScanner()
            .listener(scannerListener)
            .build();
        scanner.startScan();
        alertBuilder.setTitle("选择局域网打印机");
        alertBuilder.setMessage("扫描中...");
        if (alertDialog != null) {
            alertDialog.cancel();
        }
        alertDialog = alertBuilder.show();
    }

    public void showInputTcpPrinter(String defaultIp, String defaultPort) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.dialog_input, null);
        EditText editIP = view.findViewById(R.id.et_printer_ip);
        EditText editPort = view.findViewById(R.id.et_printer_port);
        editIP.setText(defaultIp);
        editPort.setText(defaultPort);
        builder.setTitle("输入打印机地址");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String ip = editIP.getText().toString();
                int port = Integer.parseInt(editPort.getText().toString());
                initPrinter(PrinterManager.tcpPrinter(ip, port).name(ip + ":" + port));
            }
        });
        builder.show();
    }

    public void initPrinter(EscPosPrinter.Builder builder) {
        if (printer != null) {
            EscPosPrinter currentPrinter = printer;
            new Thread() {
                @Override
                public void run() {
                    currentPrinter.disconnect();
                }
            }.start();
        }
        printer = builder
            .printWidth(PrintWidth.WIDTH_80.getWidth())
            .feedBeforeCut((byte) 3)
            .listener(new EscPosPrinter.Listener() {
                @Override
                public void onStatusChanged(PrinterStatus printerStatus) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (printerStatus) {
                                case CONNECTING:
                                case CONNECTED:
                                case CONNECT_TIMEOUT:
                                case DISCONNECTED:
                                    tvPrinterConnected.setText(printerStatus.getMessage());
                                    break;
                                default:
                                    tvPrinterStatus.setText(printerStatus.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onPrinted(Paper paper, PrinterStatus printerStatus) {

                }

                @Override
                public void onError(Exception e) {

                }
            })
            .build();
        tvPrinterInfo.setText(printer.connectType() + " / " + printer.getName());
        tvPrinterStatus.setText("");
        tvPrinterConnected.setText("");
    }

    public void checkStatus() {
        new Thread() {
            @Override
            public void run() {
                if (printer == null) return;
                PrinterStatus status = printer.getPrinterStatus();
                ToastUtils.showLong(status.getMessage());
            }
        }.start();
    }

    public void print() {
        new Thread() {
            public void run() {
                if (printer == null) return;
                Paper paper = new Paper();
                paper.addAlign(AlignType.LEFT);
                paper.addText("打印图片");
                paper.addImage(BitmapFactory.decodeResource(getResources(), R.drawable.printer));
                paper.addCutPaper();
                paper.addAlign(AlignType.CENTER);
                paper.addText("打印HTML");
                paper.addHtml(ResourceUtils.readRaw2String(R.raw.html));
                paper.addCutPaper();
                paper.addAlign(AlignType.RIGHT);
                paper.addText("打印二维码");
                paper.addQRCode("http://www.baidu.com", (byte) 3, ErrorLevel.H);
                paper.addAlign(AlignType.CENTER);
                paper.addText("打印一维码");
                paper.addBarcode("3123040", BarcodeType.CODABAR, (byte) 40, (byte) 2, HriPosition.NONE);
                paper.addCutPaper();
                paper.addBeep((byte) 3, (byte) 1000);
                paper.addOpenDrawer();
                PrintResult result = printer.print(paper);
                // printer.disconnect();
                ToastUtils.showLong("已发送: " + result.isSent() + " 数据大小: " + result.getTotalBytes());
            }
        }.start();
    }
}