package com.jumperchuck.escpos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.blankj.utilcode.util.ResourceUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jumperchuck.escpos.constant.PrintWidth;
import com.jumperchuck.escpos.constant.PrinterCommand;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.printer.Paper;
import com.jumperchuck.escpos.printer.EscPosPrinter;
import com.jumperchuck.escpos.scanner.DeviceScanner;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private EditText etIp;
    private EditText etPort;
    private Button btSelectBluetooth;
    private Button btSelectWlan;
    private Button btPrintImage;
    private Button btPrintHtml;

    private String ip = "192.168.123.100";
    private int port = 9100;

    private AlertDialog alertDialog;
    private AlertDialog.Builder alertBuilder;
    private EscPosPrinter printer;
    private DeviceScanner scanner;
    private DeviceScanner.Listener listener = new DeviceScanner.Listener() {
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

                    }
                }
            );
            if (alertDialog != null) {
                alertDialog.cancel();
            }
            alertDialog = alertBuilder.create();
            alertDialog.show();
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

        etIp = findViewById(R.id.et_printer_ip);
        etPort = findViewById(R.id.et_printer_port);
        btPrintImage = findViewById(R.id.bt_print_image);
        btPrintHtml = findViewById(R.id.bt_print_html);
        btSelectBluetooth = findViewById(R.id.bt_select_bluetooth);
        btSelectWlan = findViewById(R.id.bt_select_wlan);
        alertBuilder = new AlertDialog.Builder(this);

        etIp.setText(ip);
        etPort.setText(String.valueOf(port));

        etIp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ip = etIp.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        etPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                port = Integer.parseInt(etPort.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
        btPrintImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printImage();
            }
        });
        btPrintHtml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printHtml();
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        }, 100);
    }

    public void showSelectBluetoothPrinter() {
        scanner = PrinterManager.bluetoothScanner()
            .listener(listener)
            .build();
        scanner.startScan();
        alertBuilder.setTitle("选择蓝牙打印机");
        alertBuilder.setMessage("扫描中");
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
        if (alertDialog != null) {
            alertDialog.cancel();
        }
        alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void showSelectWlanPrinter() {
        scanner = PrinterManager.wlanScanner()
            .listener(listener)
            .build();
        scanner.startScan();
        alertBuilder.setTitle("选择局域网打印机");
        alertBuilder.setMessage("扫描中");
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
        if (alertDialog != null) {
            alertDialog.cancel();
        }
        alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void showInputTcpPrinter() {

    }

    public EscPosPrinter initPrinter() {
        if (printer != null) {
            printer.close();
        }
        return printer = PrinterManager.tcpPrinter(ip, port)
            .id(1)
            .name("name")
            .printWidth(PrintWidth.WIDTH_58.getWidth())
            .build();
    }

    public void printImage() {
        if (printer == null) return;
        new Thread() {
            public void run() {
                Paper paper = new Paper();
                paper.addImage(BitmapFactory.decodeResource(getResources(), R.drawable.printer));
                paper.addCutPaper();
                paper.setListener(new Paper.Listener() {
                    @Override
                    public void onPrintResult(EscPosPrinter printerManager, PrinterStatus printerStatus) {

                    }
                });
                printer.open();
                PrinterStatus printerStatus = printer.print(paper);
                printer.close();
                ToastUtils.showLong(printerStatus.getMessage());
            }
        }.start();
    }

    public void printHtml() {
        if (printer == null) return;
        new Thread() {
            public void run() {
                Paper paper = new Paper();
                paper.addHtml(ResourceUtils.readRaw2String(R.raw.html));
                paper.addCutPaper();
                paper.setListener(new Paper.Listener() {
                    @Override
                    public void onPrintResult(EscPosPrinter printerManager, PrinterStatus printerStatus) {

                    }
                });
                printer.open();
                PrinterStatus printerStatus = printer.print(paper);
                printer.close();
                ToastUtils.showLong(printerStatus.getMessage());
            }
        }.start();
    }
}