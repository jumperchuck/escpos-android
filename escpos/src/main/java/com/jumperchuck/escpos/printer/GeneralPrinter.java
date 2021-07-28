package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.webkit.WebView;

import com.jumperchuck.escpos.command.PrinterCommander;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.connection.TcpConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.HtmlUtils;

import java.io.IOException;

/**
 * 通用打印机
 */
public class GeneralPrinter extends EscPosPrinter {
    private static String TAG = GeneralPrinter.class.getSimpleName();

    private PrinterCommander.Reader reader;

    private GeneralPrinter(Builder builder) {
        super(builder);
    }

    @Override
    public synchronized void connect() {
        if (isConnected()) {
            return;
        }
        if (connection instanceof TcpConnection) {
            ((TcpConnection) connection).setTimeout(timeout);
        }
        sendStatusBroadcast(PrinterStatus.CONNECTING);
        connection.connect();
        if (isConnected()) {
            // 开启读取打印机返回数据线程
            reader = commander.createReader(this);
            reader.startRead();
            sendStatusBroadcast(PrinterStatus.CONNECTED);
        } else {
            // 连接失败, 重连一次
            connection.connect();
            if (isConnected()) {
                // 开启读取打印机返回数据线程
                reader = commander.createReader(this);
                reader.startRead();
                sendStatusBroadcast(PrinterStatus.CONNECTED);
            } else {
                sendStatusBroadcast(PrinterStatus.CONNECT_TIMEOUT);
            }
        }
    }

    @Override
    public synchronized void disconnect() {
        if (reader != null) {
            reader.cancelRead();
            reader = null;
        }
        connection.disconnect();
        sendStatusBroadcast(PrinterStatus.DISCONNECTED);
    }

    @Override
    public synchronized PrintResult print(Paper paper) {
        if (!isConnected()) connect();
        boolean success = false;
        int size = 0;
        if (!isConnected()) return new PrintResult(success, size);
        try {
            PrinterCommander.Sender sender = commander.createSender(this);
            for (Paper.Command command : paper.getCommands()) {
                switch (command.getKey()) {
                    case Paper.COMMAND_INIT:
                        sender.addInit();
                        break;
                    case Paper.COMMAND_ALIGN:
                        sender.addAlign((AlignType) command.getValue());
                        break;
                    case Paper.COMMAND_IMAGE:
                        Bitmap imageBitmap = null;
                        Object imageValue = command.getValue();
                        if (imageValue instanceof String) {
                            byte[] base64Bytes = Base64.decode((String) imageValue, Base64.DEFAULT);
                            imageBitmap = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.length);
                        } else if (imageValue instanceof Bitmap) {
                            imageBitmap = (Bitmap) imageValue;
                        }
                        sender.addBitmap(imageBitmap, printWidth);
                        sender.addPrint();
                        break;
                    case Paper.COMMAND_HTML:
                        Bitmap htmlBitmap = HtmlUtils.toBitmap(context, (String) command.getValue(), printWidth);
                        sender.addBitmap(htmlBitmap, printWidth);
                        sender.addPrint();
                        break;
                    case Paper.COMMAND_LINE:
                        sender.addLine();
                        break;
                    case Paper.COMMAND_LINES:
                        sender.addLines((byte) command.getValue());
                        break;
                    case Paper.COMMAND_CUT_PAPER:
                        if (feedBeforeCut > 0) {
                            sender.addLines(feedBeforeCut);
                        }
                        sender.addCutPaper();
                        break;
                    case Paper.COMMAND_TEXT:
                        sender.addText((String) command.getValue());
                        break;
                    case Paper.COMMAND_BARCODE:
                        sender.addBarcode(
                            (String) command.getValue(),
                            (BarcodeType) command.getValue2(),
                            (byte) command.getValue3(),
                            (byte) command.getValue4(),
                            (HriPosition) command.getValue5()
                        );
                        sender.addPrint();
                        break;
                    case Paper.COMMAND_QRCODE:
                        sender.addQRCode(
                            (String) command.getValue(),
                            (byte) command.getValue2(),
                            (ErrorLevel) command.getValue3()
                        );
                        sender.addPrint();
                        break;
                    default:
                        break;
                }
            }
            size = sender.startSend();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return new PrintResult(success, size);
    }

    @Override
    public synchronized PrinterStatus getPrinterStatus() {
        if (!isConnected()) {
            return PrinterStatus.DISCONNECTED;
        }
        PrinterStatus status = PrinterStatus.UNKNOWN_ERROR;
        try {
            status = reader.updateStatus(soTimeout);
        } catch (IOException e) {
            status = PrinterStatus.DISCONNECTED;
        }
        sendStatusBroadcast(status);
        return status;
    }

    public static class Builder extends EscPosPrinter.Builder<Builder> {
        public Builder(Context context) {
            super(context);
        }

        @Override
        public GeneralPrinter build() {
            return new GeneralPrinter(this);
        }
    }
}
