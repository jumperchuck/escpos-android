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
    public synchronized void open() {
        connection.connect();
    }

    @Override
    public synchronized void close() {
        connection.disconnect();
    }

    @Override
    public synchronized PrinterStatus print(Paper paper) {
        if (!isConnected()) {
            open();
        }
        PrinterStatus status = getPrinterStatus();
        if (isConnected()) {
            try {
                SunmiPrinterService service = SunmiConnection.getService();
                service.enterPrinterBuffer(true);
                for (Paper.Command command : paper.getCommands()) {
                    switch (command.getKey()) {
                        case Paper.COMMAND_INIT:
                            service.printerInit(null);
                            break;
                        case Paper.COMMAND_ALIGN:
                            Paper.ALIGN align = (Paper.ALIGN) command.getValue();
                            switch (align) {
                                case LEFT:
                                    service.setAlignment(0, null);
                                    break;
                                case CENTER:
                                    service.setAlignment(1, null);
                                    break;
                                case RIGHT:
                                    service.setAlignment(2, null);
                                    break;
                            }
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
                            service.printBitmap(imageBitmap, null);
                            break;
                        case Paper.COMMAND_HTML:
                            Bitmap htmlBitmap = HtmlUtils.toBitmap(context, (String) command.getValue(), printWidth);
                            service.printBitmap(htmlBitmap, null);
                            break;
                        case Paper.COMMAND_LINE:
                            service.lineWrap(1, null);
                            break;
                        case Paper.COMMAND_LINES:
                            service.lineWrap((int) command.getValue(), null);
                            break;
                        case Paper.COMMAND_CUT_PAPER:
                            if (feedBeforeCut > 0) {
                                service.lineWrap(feedBeforeCut, null);
                            }
                            service.cutPaper(null);
                            break;
                        case Paper.COMMAND_TEXT:
                            service.printText((String) command.getValue(), null);
                            break;
                        case Paper.COMMAND_BARCODE:
                            String barcodeValue = (String) command.getValue();
                            int barcodeSymbology = 0;
                            int barcodeTextPosition = 0;
                            switch ((Paper.BARCODE_TYPE) command.getValue2()) {
                                case UPCA:
                                    barcodeSymbology = 0;
                                    break;
                                case UPCE:
                                    barcodeSymbology = 1;
                                    break;
                                case EAN13:
                                    barcodeSymbology = 2;
                                    break;
                                case EAN8:
                                    barcodeSymbology = 3;
                                    break;
                                case CODE39:
                                    barcodeSymbology = 4;
                                    break;
                                case ITF:
                                    barcodeSymbology = 5;
                                    break;
                                case CODABAR:
                                    barcodeSymbology = 6;
                                    break;
                                case CODE93:
                                    barcodeSymbology = 7;
                                    break;
                                case CODE128:
                                    barcodeSymbology = 8;
                                    break;
                                case CODE128A:
                                    barcodeSymbology = 8;
                                    barcodeValue = String.format("{A%s", barcodeValue);
                                    break;
                                case CODE128B:
                                    barcodeSymbology = 8;
                                    break;
                                case CODE128C:
                                    barcodeSymbology = 8;
                                    barcodeValue = String.format("{C%s", barcodeValue);
                                    break;
                            }
                            switch ((Paper.HRI_POSITION) command.getValue5()) {
                                case NONE:
                                    barcodeTextPosition = 0;
                                    break;
                                case TOP:
                                    barcodeTextPosition = 1;
                                    break;
                                case BOTTOM:
                                    barcodeTextPosition = 2;
                                    break;
                                case BOTH:
                                    barcodeTextPosition = 3;
                                    break;
                            }
                            service.printBarCode(
                                barcodeValue,
                                barcodeSymbology,
                                (int) command.getValue3(),
                                (int) command.getValue4(),
                                barcodeTextPosition,
                                null
                            );
                            break;
                        case Paper.COMMAND_QRCODE:
                            int qrcodeErrorLevel = 0;
                            switch ((Paper.ERROR_LEVEL) command.getValue3()) {
                                case L:
                                    qrcodeErrorLevel = 0;
                                    break;
                                case M:
                                    qrcodeErrorLevel = 1;
                                    break;
                                case Q:
                                    qrcodeErrorLevel = 2;
                                    break;
                                case H:
                                    qrcodeErrorLevel = 3;
                                    break;
                            }
                            service.printQRCode((String) command.getValue(),
                                (int) command.getValue2(),
                                qrcodeErrorLevel,
                                null);
                            break;
                    }
                }
                service.exitPrinterBuffer(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            status = PrinterStatus.DISCONNECTED;
        }
        sendStatusBroadcast(status);
        return status;
    }

    @Override
    public PrinterStatus getPrinterStatus() {
        PrinterStatus status = PrinterStatus.NORMAL;
        if (isConnected()) {
            SunmiPrinterService service = SunmiConnection.getService();
            try {
                int printerStatus = service.updatePrinterState();
                switch (printerStatus) {
                    case 1: // 打印机工作正常
                        status = PrinterStatus.NORMAL;
                        break;
                    case 2: // 打印机准备中
                        status = PrinterStatus.CONNECTING;
                        break;
                    case 3: // 通讯异常
                        status = PrinterStatus.ERROR;
                        break;
                    case 4: // 缺纸
                        status = PrinterStatus.PAPER_OUT;
                        break;
                    case 5: // 过热
                        status = PrinterStatus.OVER_HEATING;
                        break;
                    case 6: // 开盖
                        status = PrinterStatus.COVER_OPEN;
                        break;
                    case 7: // 切刀异常
                        status = PrinterStatus.KNIFE_ERROR;
                        break;
                    case 8: // 切刀恢复
                        status = PrinterStatus.NORMAL;
                        break;
                    case 9: // 未检测到黑标
                        status = PrinterStatus.BLACK_LABEL_ERROR;
                        break;
                    case 505: // 未检测到打印机
                        status = PrinterStatus.DISCONNECTED;
                        break;
                    case 507: // 打印机固件升级失败
                        status = PrinterStatus.NORMAL;
                        break;
                    default:
                        status = PrinterStatus.UNKNOWN_ERROR;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                status = PrinterStatus.UNKNOWN_ERROR;
            }
        } else {
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
        public SunmiPrinter build() {
            return new SunmiPrinter(this);
        }
    }
}
