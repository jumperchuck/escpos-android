package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SunmiCommander implements PrinterCommander {
    @Override
    public Reader createReader(PrinterConnection connection) {
        return new Reader();
    }

    @Override
    public Sender createSender(PrinterConnection connection) {
        return new Sender();
    }

    public static class Reader implements PrinterCommander.Reader {
        @Override
        public void startRead() {

        }

        @Override
        public void cancelRead() {

        }

        @Override
        public PrinterStatus updateStatus(int soTimeout) throws IOException {
            SunmiPrinterService service = SunmiConnection.getService();
            if (service == null) {
                return PrinterStatus.DISCONNECTED;
            }
            try {
                int status = service.updatePrinterState();
                switch (status) {
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
                throw new IOException();
            }
        }
    }

    public static class Sender implements PrinterCommander.Sender {
        private List<Task> tasks;

        public Sender() {
            this.tasks = new ArrayList<>();
        }

        @Override
        public void addInit() {
            this.tasks.add(service -> {
                service.printerInit(null);
            });
        }

        @Override
        public void addAlign(AlignType type) {
            this.tasks.add(service -> {
                switch (type) {
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
            });
        }

        @Override
        public void addBitmap(Bitmap bitmap, int width) {
            this.tasks.add(service -> {
                service.printBitmap(bitmap, null);
            });
        }

        @Override
        public void addPrint() {

        }

        @Override
        public void addLine() {
            this.tasks.add(service -> {
                service.lineWrap(1, null);
            });
        }

        @Override
        public void addLines(byte n) {
            this.tasks.add(service -> {
                service.lineWrap(n, null);
            });
        }

        @Override
        public void addCutPaper() {
            this.tasks.add(service -> {
                service.cutPaper(null);
            });
        }

        @Override
        public void addText(String text) {
            this.tasks.add(service -> {
                service.printText(text, null);
            });
        }

        @Override
        public void addBarcode(final String content, BarcodeType type, byte height, byte width, HriPosition hriPosition) {
            this.tasks.add(service -> {
                String value = content;
                int symbology = 0;
                int textPosition = 0;
                switch (type) {
                    case UPCA:
                        symbology = 0;
                        break;
                    case UPCE:
                        symbology = 1;
                        break;
                    case EAN13:
                        symbology = 2;
                        break;
                    case EAN8:
                        symbology = 3;
                        break;
                    case CODE39:
                        symbology = 4;
                        break;
                    case ITF:
                        symbology = 5;
                        break;
                    case CODABAR:
                        symbology = 6;
                        break;
                    case CODE93:
                        symbology = 7;
                        break;
                    case CODE128:
                        symbology = 8;
                        break;
                    case CODE128A:
                        symbology = 8;
                        value = String.format("{A%s", content);
                        break;
                    case CODE128B:
                        symbology = 8;
                        break;
                    case CODE128C:
                        symbology = 8;
                        value = String.format("{C%s", content);
                        break;
                }
                switch (hriPosition) {
                    case NONE:
                        textPosition = 0;
                        break;
                    case TOP:
                        textPosition = 1;
                        break;
                    case BOTTOM:
                        textPosition = 2;
                        break;
                    case BOTH:
                        textPosition = 3;
                        break;
                }
                service.printBarCode(value, symbology, height, width, textPosition, null);
            });
        }

        @Override
        public void addQRCode(String content, byte moduleSize, ErrorLevel errorLevel) {
            this.tasks.add(service -> {
                int level = 0;
                switch (errorLevel) {
                    case L:
                        level = 0;
                        break;
                    case M:
                        level = 1;
                        break;
                    case Q:
                        level = 2;
                        break;
                    case H:
                        level = 3;
                        break;
                }
                service.printQRCode(content, moduleSize, level, null);
            });
        }

        @Override
        public void addBeep(byte n, byte time) {

        }

        @Override
        public void addOpenDrawer() {
            this.tasks.add(service -> {
                service.openDrawer(null);
            });
        }

        @Override
        public int startSend() throws IOException {
            SunmiPrinterService service = SunmiConnection.getService();
            if (service == null) {
                return 0;
            }
            try {
                service.enterPrinterBuffer(true);
                for (Task task : tasks) {
                    try {
                        task.invoke(service);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                service.exitPrinterBuffer(true);
                return 0;
            } catch (RemoteException e) {
                e.printStackTrace();
                throw new IOException();
            }
        }
    }

    private interface Task {
        void invoke(SunmiPrinterService service) throws RemoteException;
    }
}
