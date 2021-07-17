package com.jumperchuck.escpos;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class MainActivity extends AppCompatActivity {

    private EditText etIp;
    private EditText etPort;
    private Button btPrintImage;
    private Button btPrintHtml;

    private String ip = "192.168.123.100";
    private int port = 9100;

    private EscPosPrinter printer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIp = findViewById(R.id.et_printer_ip);
        etPort = findViewById(R.id.et_printer_port);
        btPrintImage = findViewById(R.id.bt_print_image);
        btPrintHtml = findViewById(R.id.bt_print_html);

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
    }

    public void printImage() {
        if (printer == null) {
            printer = PrinterManager.tcpPrinter(ip, port)
                .id(1)
                .name("name")
                .printWidth(PrintWidth.WIDTH_58.getWidth())
                .printerCommand(PrinterCommand.ESC)
                .build();
        }
        new Thread() {
            public void run() {
                Paper paper = new Paper();
                paper.addImage(BitmapFactory.decodeResource(getResources(), R.drawable.printer));
                paper.addCutPaper();
                paper.addHtml(ResourceUtils.readRaw2String(R.raw.html));
                paper.setListener(new Paper.Listener() {
                    @Override
                    public void onPrintResult(EscPosPrinter printerManager, PrinterStatus printerStatus) {

                    }
                });

                printer.open();
                PrinterStatus printerStatus = printer.print(paper);
                // printer.close();
                ToastUtils.showLong(printerStatus.getMessage());
            }
        }.start();
    }

    public void printHtml() {

    }
}