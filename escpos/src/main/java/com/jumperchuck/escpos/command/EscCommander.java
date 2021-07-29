package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;

import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.CommandType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.LogUtils;
import com.jumperchuck.escpos.util.ReadUtils;

import java.io.IOException;
import java.util.Vector;

public class EscCommander implements PrinterCommander {
    private PrinterStatus currentStatus;

    @Override
    public Reader createReader(PrinterConnection connection) {
        return new Reader(connection);
    }

    @Override
    public Sender createSender(PrinterConnection connection) {
        return new Sender(connection);
    }

    /**
     * 判断是实时状态（10 04 02）还是查询状态（1D 72 01）
     */
    private int judgeResponseType(byte r) {
        return (byte) ((r & 0x10) >> 4);
    }

    public class Reader extends Thread implements PrinterCommander.Reader {
        private PrinterConnection connection;
        private byte[] buffer = new byte[100];

        private Reader(PrinterConnection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    // 读取打印机返回信息,打印机没有返回值返回-1
                    int len = connection.readData(buffer);
                    LogUtils.d("READ", len + " / " + buffer[0]);
                    int result = judgeResponseType(buffer[0]);
                    if (result == 0) {
                        // 打印机缓冲区打印完成
                        if (len < 0) {
                            currentStatus = PrinterStatus.NORMAL;
                        } else if ((buffer[0] & 0x03) > 0) {
                            // 纸将尽
                            currentStatus = PrinterStatus.NORMAL;
                        } else if ((buffer[0] & 0x0C) > 0) {
                            // 缺纸
                            currentStatus = PrinterStatus.PAPER_OUT;
                        } else {
                            currentStatus = PrinterStatus.ERROR;
                        }
                    } else {
                        // 实时状态
                        if (len < 0) {
                            currentStatus = PrinterStatus.NORMAL;
                        } else if ((buffer[0] & 0x04) > 0) {
                            currentStatus = PrinterStatus.COVER_OPEN;
                        } else if ((buffer[0] & 0x08) > 0) {
                            currentStatus = PrinterStatus.FEEDING;
                        } else if ((buffer[0] & 0x20) > 0) {
                            currentStatus = PrinterStatus.PAPER_OUT;
                        } else if ((buffer[0] & 0x40) > 0) {
                            currentStatus = PrinterStatus.ERROR;
                            // ESC查询错误状态
                            // connection.writeData(new byte[]{0x10, 0x04, 0x03});
                            // len = connection.readData(buffer);
                            // if (len < 0) {
                            //     currentStatus = PrinterStatus.ERROR;
                            // } else if ((buffer[0] & 0x08) > 0) {
                            //     currentStatus = PrinterStatus.KNIFE_ERROR;
                            // } else if ((buffer[0] & 0x40) > 0) {
                            //     currentStatus = PrinterStatus.OVER_HEATING;
                            // } else {
                            //     currentStatus = PrinterStatus.ERROR;
                            // }
                        } else {
                            currentStatus = PrinterStatus.NORMAL;
                        }
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
            connection.writeData(CommandType.ESC.getCheckCommand());
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
        private EscCommand command;

        private Sender(PrinterConnection connection) {
            this.connection = connection;
            this.command = new EscCommand();
        }

        @Override
        public void addInit() {
            this.command.addInitializePrinter();
        }

        @Override
        public void addAlign(AlignType type) {
            switch (type) {
                case LEFT:
                    command.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    break;
                case CENTER:
                    command.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    break;
                case RIGHT:
                    command.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    break;
            }
        }

        @Override
        public void addBitmap(Bitmap bitmap, int width) {
            command.addRastBitImage(bitmap, width, 0);
        }

        @Override
        public void addPrint() {
            command.addPrintAndLineFeed();
        }

        @Override
        public void addLine() {
            command.addPrintAndLineFeed();
        }

        @Override
        public void addLines(byte n) {
            command.addPrintAndFeedLines(n);
        }

        @Override
        public void addCutPaper() {
            command.addCutPaper();
        }

        @Override
        public void addText(String text) {
            command.addText(text);
        }

        @Override
        public void addBarcode(String content, BarcodeType type, byte height, byte width, HriPosition position) {
            command.addSetBarcodeHeight(height);
            command.addSetBarcodeWidth(width);
            switch (position) {
                case NONE:
                    command.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.NO_PRINT);
                    break;
                case TOP:
                    command.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.ABOVE);
                    break;
                case BOTTOM:
                    command.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
                    break;
                case BOTH:
                    command.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.ABOVE_AND_BELOW);
                    break;
            }
            switch (type) {
                case UPCA:
                    command.addUPCA(content);
                    break;
                case UPCE:
                    command.addUPCE(content);
                    break;
                case EAN13:
                    command.addEAN13(content);
                    break;
                case EAN8:
                    command.addEAN8(content);
                    break;
                case CODE39:
                    command.addCODE39(content);
                    break;
                case ITF:
                    command.addITF(content);
                    break;
                case CODABAR:
                    command.addCODABAR(content);
                    break;
                case CODE93:
                    command.addCODE93(content);
                    break;
                case CODE128:
                    command.addCODE128(content);
                    break;
                case CODE128A:
                    command.addCODE128(content);
                    break;
                case CODE128B:
                    command.addCODE128(command.genCodeB(content));
                    break;
                case CODE128C:
                    command.addCODE128(command.genCodeC(content));
                    break;
            }
        }

        @Override
        public void addQRCode(String content, byte moduleSize, ErrorLevel errorLevel) {
            // TODO 转换成图片发送（部分佳博机型才支持此命令）
            switch (errorLevel) {
                case L:
                    command.addSelectErrorCorrectionLevelForQRCode((byte) 0x30);
                    break;
                case M:
                    command.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                    break;
                case Q:
                    command.addSelectErrorCorrectionLevelForQRCode((byte) 0x32);
                    break;
                case H:
                    command.addSelectErrorCorrectionLevelForQRCode((byte) 0x33);
                    break;
            }
            command.addSelectSizeOfModuleForQRCode(moduleSize);
            command.addStoreQRCodeData(content);
            command.addPrintQRCode();
        }

        @Override
        public void addBeep(byte n, byte time) {
            command.addSound(n, time);
        }

        @Override
        public void addOpenDrawer() {
            command.addGeneratePlus(LabelCommand.FOOT.F2, (byte) 0x40, (byte) 0x50);
        }

        @Override
        public int startSend() throws IOException {
            Vector<Byte> data = command.getCommand();
            connection.writeData(data);
            // 一票一控，发送缓冲区打印完成查询指令
            connection.writeData(new byte[]{0x1D, 0x72, 0x01});
            currentStatus = null;
            // 设置等待时间
            ReadUtils.readSync(() -> currentStatus, 4000);
            return data.size();
        }
    }
}
