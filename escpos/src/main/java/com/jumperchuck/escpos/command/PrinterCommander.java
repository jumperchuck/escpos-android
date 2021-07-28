package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;

import java.io.IOException;

public interface PrinterCommander {
    PrinterCommander.Reader createReader(PrinterConnection connection);

    PrinterCommander.Sender createSender(PrinterConnection connection);

    interface Reader {
        void startRead();

        void cancelRead();

        PrinterStatus updateStatus(int soTimeout) throws IOException;
    }

    interface Sender {
        void addInit();

        void addAlign(AlignType type);

        void addBitmap(Bitmap bitmap, int width);

        void addPrint();

        void addLine();

        void addLines(byte n);

        void addCutPaper();

        void addText(String text);

        void addBarcode(String content, BarcodeType type, byte height, byte width, HriPosition hriPosition);

        void addQRCode(String content, byte moduleSize, ErrorLevel errorLevel);

        int startSend() throws IOException;
    }
}
