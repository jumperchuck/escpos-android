package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.util.Base64;

import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.HtmlUtils;
import com.sunmi.peripheral.printer.SunmiPrinterService;

/**
 * 商米内置打印机
 */
public class SunmiPrinter extends EscPosPrinter {
    private SunmiPrinter(Builder builder) {
        super(builder);
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public PrintResult print(Paper paper) {
        return null;
    }

    @Override
    public PrinterStatus getPrinterStatus() {
        return null;
    }

    public static class Builder extends EscPosPrinter.Builder<Builder> {
        public Builder(Context context) {
            super(context);
        }

        @Override
        public SunmiPrinter build() {
            return new SunmiPrinter(this);
        }
    }
}
