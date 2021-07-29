package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;

import com.gprinter.command.LabelCommand;
import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.CommandType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.ReadUtils;

import java.io.IOException;

public class TscCommander implements PrinterCommander {
    private PrinterStatus currentStatus;

    @Override
    public Reader createReader(PrinterConnection connection) {
        return new Reader(connection);
    }

    @Override
    public Sender createSender(PrinterConnection connection) {
        return new Sender(connection);
    }

    public class Reader extends Thread implements PrinterCommander.Reader {
        private PrinterConnection connection;
        private byte[] buffer = new byte[100];

        public Reader(PrinterConnection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    int len = connection.readData(buffer);
                    if (len < 0) {
                        currentStatus = PrinterStatus.NORMAL;
                    } else if (buffer[0] == 0) {
                        currentStatus = PrinterStatus.NORMAL;
                    } else if ((buffer[0] & 0x01) > 0) {
                        currentStatus = PrinterStatus.COVER_OPEN;
                    } else if ((buffer[0] & 0x02) > 0) {
                        currentStatus = PrinterStatus.PAPER_ERROR;
                    } else if ((buffer[0] & 0x04) > 0) {
                        currentStatus = PrinterStatus.PAPER_OUT;
                    } else if ((buffer[0] & 0x08) > 0) {
                        currentStatus = PrinterStatus.CARBON_OUT;
                    } else if ((buffer[0] & 0x10) > 0) {
                        currentStatus = PrinterStatus.FEEDING;
                    } else if ((buffer[0] & 0x20) > 0) {
                        currentStatus = PrinterStatus.FEEDING;
                    } else {
                        currentStatus = PrinterStatus.UNKNOWN_ERROR;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void startRead() {
            start();
        }

        @Override
        public void cancelRead() {
            interrupt();
        }

        @Override
        public PrinterStatus updateStatus(int soTimeout) throws IOException {
            if (!connection.isConnected()) {
                return PrinterStatus.DISCONNECTED;
            }
            connection.writeData(CommandType.TSC.getCheckCommand());
            currentStatus = null;
            ReadUtils.readSync(() -> currentStatus, soTimeout);
            if (currentStatus == null) {
                return PrinterStatus.READ_TIMEOUT;
            } else {
                return currentStatus;
            }
        }
    }

    public class Sender implements PrinterCommander.Sender {
        private PrinterConnection connection;
        private LabelCommand command;

        public Sender(PrinterConnection connection) {
            this.connection = connection;
            this.command = new LabelCommand();
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
