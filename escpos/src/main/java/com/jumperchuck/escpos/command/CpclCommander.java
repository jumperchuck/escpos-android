package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;

import java.io.IOException;

public class CpclCommander implements PrinterCommander {
    @Override
    public Reader createReader(PrinterConnection connection) {
        return new Reader(connection);
    }

    @Override
    public Sender createSender(PrinterConnection connection) {
        return new Sender(connection);
    }

    public static class Reader implements PrinterCommander.Reader {
        private Reader(PrinterConnection connection) {

        }

        @Override
        public void startRead() {

        }

        @Override
        public void cancelRead() {

        }

        @Override
        public PrinterStatus updateStatus(int soTimeout) throws IOException {
            return PrinterStatus.NORMAL;
        }
    }

    public static class Sender implements PrinterCommander.Sender {
        private Sender(PrinterConnection connection) {

        }

        @Override
        public void addInit() {

        }

        @Override
        public void addAlign(AlignType type) {

        }

        @Override
        public void addBitmap(Bitmap bitmap, int width) {

        }

        @Override
        public void addPrint() {

        }

        @Override
        public void addLine() {

        }

        @Override
        public void addLines(byte n) {

        }

        @Override
        public void addCutPaper() {

        }

        @Override
        public void addText(String text) {

        }

        @Override
        public void addBarcode(String content, BarcodeType type, byte height, byte width, HriPosition hriPosition) {

        }

        @Override
        public void addQRCode(String content, byte moduleSize, ErrorLevel errorLevel) {

        }

        @Override
        public void addBeep(byte n, byte time) {

        }

        @Override
        public void addOpenDrawer() {

        }

        @Override
        public int startSend() throws IOException {
            return 0;
        }
    }
}