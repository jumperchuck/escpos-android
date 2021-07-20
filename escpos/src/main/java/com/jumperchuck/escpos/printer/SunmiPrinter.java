package com.jumperchuck.escpos.printer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.HtmlUtils;
import com.sunmi.peripheral.printer.SunmiPrinterService;

/**
 * 商米内置打印机
 */
public class SunmiPrinter extends EscPosPrinter {
    public SunmiPrinter(Builder builder) {
        super(builder);
    }

    @Override
    public synchronized void open() {
        printerConnection.connect();
    }

    @Override
    public synchronized void close() {
        printerConnection.disconnect();
    }

    @Override
    public synchronized PrinterStatus print(Paper paper) {
        SunmiPrinterService service = SunmiConnection.getService();
        if (service == null) return PrinterStatus.DISCONNECTED;
        try {
            service.enterPrinterBuffer(true);
            for (Paper.Command command : paper.getCommands()) {
                switch (command.getKey()) {
                    case Paper.COMMAND_IMAGE:
                        byte[] base64Bytes = Base64.decode((String) command.getValue(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.length);
                        service.printBitmap(bitmap, null);
                        break;
                    case Paper.COMMAND_HTML:
                        Bitmap htmlBitmap = HtmlUtils.toBitmap(getContext(), (String) command.getValue(), getPrintWidth());
                        if (htmlBitmap != null) {
                            service.printBitmap(htmlBitmap, null);
                        }
                        break;
                    case Paper.COMMAND_LINE:
                        service.lineWrap(1, null);
                        break;
                    case Paper.COMMAND_LINES:
                        service.lineWrap((int) command.getValue(), null);
                        break;
                    case Paper.COMMAND_CUT_PAPER:
                        service.cutPaper(null);
                        break;
                    case Paper.COMMAND_TEXT:
                        service.printText((String) command.getValue(), null);
                        break;
                    case Paper.COMMAND_BARCODE:
                        // service.printBarCode((String) command.getValue());
                        break;
                    case Paper.COMMAND_QRCODE:
                        // service.printQRCode((String) command.getValue());
                        break;
                }
            }
            service.exitPrinterBuffer(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PrinterStatus.UNKNOWN_ERROR;
    }

    @Override
    public PrinterStatus getPrinterStatus() {
        SunmiPrinterService service = SunmiConnection.getService();
        if (service == null) return PrinterStatus.DISCONNECTED;
        try {
            int printerStatus = service.updatePrinterState();
            switch (printerStatus) {
                case 1: // 打印机工作正常
                    return PrinterStatus.NORMAL;
                case 2: // 打印机准备中
                    return PrinterStatus.CONNECTING;
                case 3: // 通讯异常
                    return PrinterStatus.ERROR;
                case 4: // 缺纸
                    return PrinterStatus.PAPER_OUT;
                case 5: // 过热
                    return PrinterStatus.OVER_HEATING;
                case 6: // 开盖
                    return PrinterStatus.COVER_OPEN;
                case 7: // 切刀异常
                    return PrinterStatus.KNIFE_ERROR;
                case 8: // 切刀恢复
                    return PrinterStatus.NORMAL;
                case 9: // 未检测到黑标
                    return PrinterStatus.BLACK_LABEL_ERROR;
                case 505: // 未检测到打印机
                    return PrinterStatus.DISCONNECTED;
                case 507: // 打印机固件升级失败
                    return PrinterStatus.NORMAL;
                default:
                    return PrinterStatus.UNKNOWN_ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PrinterStatus.UNKNOWN_ERROR;
    }
}
