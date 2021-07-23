package com.jumperchuck.escpos.printer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.jumperchuck.escpos.connection.SunmiConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.constant.PrinterCommand;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.HtmlUtils;
import com.jumperchuck.escpos.util.LogUtils;
import com.jumperchuck.escpos.util.ThreadFactoryBuilder;
import com.jumperchuck.escpos.util.ThreadPool;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通用打印机
 */
public class GeneralPrinter extends EscPosPrinter {
    private final int ESC = 1;
    private final int TSC = 2;
    private final int CPCL = 3;

    private Reader reader;

    private int queryPrinterCommandFlag;

    public GeneralPrinter(Builder builder) {
        super(builder);
    }

    @Override
    public synchronized void open() {
        if (isConnected()) {
            return;
        }
        currentPrinterStatus = null;
        listener.onOpening();
        printerConnection.connect();
        if (isConnected()) {
            // 开启读取打印机返回数据线程
            reader = new Reader();
            reader.start();
            listener.onOpened();
            if (printerCommand == null) {
                // 查询打印机使用指令
                queryPrinterCommand(); // 小票机连接不上 注释这行，添加下面那三行代码。使用ESC指令
            }
        } else {
            // 连接失败, 重连一次
            printerConnection.connect();
            if (isConnected()) {
                // 开启读取打印机返回数据线程
                reader = new Reader();
                reader.start();
                listener.onOpened();
                if (printerCommand == null) {
                    // 查询打印机使用指令
                    queryPrinterCommand(); // 小票机连接不上 注释这行，添加下面那三行代码。使用ESC指令
                }
            } else {
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        currentPrinterStatus = null;
        if (reader != null) {
            reader.cancel();
            reader = null;
        }
        if (isConnected()) {
            printerConnection.disconnect();
            listener.onClosed();
        }
    }

    @Override
    public synchronized PrinterStatus print(Paper paper) {
        if (!isConnected()) {
            open();
        }
        PrinterStatus status = getPrinterStatus();
        if (status == PrinterStatus.NORMAL) {
            try {
                switch (printerCommand) {
                    case ESC:
                        sendDataByEsc(paper);
                        break;
                    case TSC:
                        sendDataByTsc(paper);
                        break;
                    case CPCL:
                        sendDataByCpcl(paper);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                status = PrinterStatus.UNKNOWN_ERROR;
            }
        }
        if (paper.getListener() != null) {
            paper.getListener().onPrintResult(this, status);
        }
        sendStatusBroadcast(status);
        return status;
    }

    @Override
    public synchronized PrinterStatus getPrinterStatus() {
        PrinterStatus status = currentPrinterStatus;
        if (isConnected()) {
            try {
                currentPrinterStatus = null;
                if (printerCommand != null) {
                    sendByteDataImmediately(printerCommand.getCheckCommand());
                }
                long startTime = System.currentTimeMillis();
                long currentTime = System.currentTimeMillis();
                while (currentTime - startTime < 1500) {
                    if (currentPrinterStatus != null) {
                        status = currentPrinterStatus;
                        break;
                    }
                    Thread.sleep(100);
                    currentTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (status == null) {
                status = PrinterStatus.UNKNOWN_ERROR;
            }
        } else {
            status = PrinterStatus.DISCONNECTED;
        }
        sendStatusBroadcast(status);
        return status;
    }

    private int getPrinterStatusCommand(PrinterStatus printerStatus) {
        switch (printerCommand) {
            case ESC:
                return printerStatus.getEsc();
            case TSC:
                return printerStatus.getTsc();
            case CPCL:
                return printerStatus.getCpcl();
        }
        return -1;
    }

    private boolean checkPrinterStatus(byte b, PrinterStatus printerStatus) {
        switch (printerCommand) {
            case ESC:
                if (printerStatus.getEsc() <= 0) return false;
                if ((b & printerStatus.getEsc()) > 0) return true;
                break;
            case TSC:
                if (printerStatus.getTsc() <= 0) return false;
                if ((b & printerStatus.getTsc()) > 0) return true;
                break;
            case CPCL:
                if (printerStatus.getCpcl() <= 0) return false;
                if ((b & printerStatus.getCpcl()) > 0) return true;
                break;
        }
        return false;
    }

    /**
     * 判断是实时状态（10 04 02）还是查询状态（1D 72 01）
     */
    private int judgeResponseType(byte r) {
        return (byte) ((r & 0x10) >> 4);
    }

    private void sendDataByEsc(Paper paper) throws Exception {
        EscCommand escCommand = new EscCommand();
        for (Paper.Command command : paper.getCommands()) {
            switch (command.getKey()) {
                case Paper.COMMAND_INIT:
                    escCommand.addInitializePrinter();
                    break;
                case Paper.COMMAND_ALIGN:
                    Paper.ALIGN align = (Paper.ALIGN) command.getValue();
                    switch (align) {
                        case LEFT:
                            escCommand.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                            break;
                        case CENTER:
                            escCommand.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                            break;
                        case RIGHT:
                            escCommand.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
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
                    escCommand.addRastBitImage(imageBitmap, printWidth, 0);
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_HTML:
                    Bitmap htmlBitmap = HtmlUtils.toBitmap(context, (String) command.getValue(), printWidth);
                    escCommand.addRastBitImage(htmlBitmap, printWidth, 0);
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_LINE:
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_LINES:
                    escCommand.addPrintAndFeedLines((byte) command.getValue());
                    break;
                case Paper.COMMAND_CUT_PAPER:
                    escCommand.addPrintAndFeedLines((byte) 3);
                    escCommand.addCutPaper();
                    break;
                case Paper.COMMAND_TEXT:
                    escCommand.addText((String) command.getValue());
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_BARCODE:
                    String barcodeValue = (String) command.getValue();
                    escCommand.addSetBarcodeHeight((byte) command.getValue3());
                    escCommand.addSetBarcodeWidth((byte) command.getValue4());
                    switch ((Paper.HRI_POSITION) command.getValue5()) {
                        case NONE:
                            escCommand.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.NO_PRINT);
                            break;
                        case TOP:
                            escCommand.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.ABOVE);
                            break;
                        case BOTTOM:
                            escCommand.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
                            break;
                        case BOTH:
                            escCommand.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.ABOVE_AND_BELOW);
                            break;
                    }
                    switch ((Paper.SYMBOLOGY) command.getValue2()) {
                        case UPCA:
                            escCommand.addUPCA(barcodeValue);
                            break;
                        case UPCE:
                            escCommand.addUPCE(barcodeValue);
                            break;
                        case EAN13:
                            escCommand.addEAN13(barcodeValue);
                            break;
                        case EAN8:
                            escCommand.addEAN8(barcodeValue);
                            break;
                        case CODE39:
                            escCommand.addCODE39(barcodeValue);
                            break;
                        case ITF:
                            escCommand.addITF(barcodeValue);
                            break;
                        case CODABAR:
                            escCommand.addCODABAR(barcodeValue);
                            break;
                        case CODE93:
                            escCommand.addCODE93(barcodeValue);
                            break;
                        case CODE128:
                            escCommand.addCODE128(barcodeValue);
                            break;
                        case CODE128A:
                            escCommand.addCODE128(barcodeValue);
                            break;
                        case CODE128B:
                            escCommand.addCODE128(escCommand.genCodeB(barcodeValue));
                            break;
                        case CODE128C:
                            escCommand.addCODE128(escCommand.genCodeC(barcodeValue));
                            break;
                    }
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_QRCODE:
                    // TODO 转换成图片发送（部分佳博机型才支持此命令）
                    switch ((Paper.ERROR_LEVEL) command.getValue3()) {
                        case L:
                            escCommand.addSelectErrorCorrectionLevelForQRCode((byte) 0x30);
                            break;
                        case M:
                            escCommand.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                            break;
                        case Q:
                            escCommand.addSelectErrorCorrectionLevelForQRCode((byte) 0x32);
                            break;
                        case H:
                            escCommand.addSelectErrorCorrectionLevelForQRCode((byte) 0x33);
                            break;
                    }
                    escCommand.addSelectSizeOfModuleForQRCode((byte) command.getValue2());
                    escCommand.addStoreQRCodeData((String) command.getValue());
                    escCommand.addPrintQRCode();
                    escCommand.addPrintAndLineFeed();
                    break;
                default:
                    break;
            }
        }
        sendDataImmediately(escCommand.getCommand());
    }

    private void sendDataByTsc(Paper paper) throws Exception {
        LabelCommand labelCommand = new LabelCommand();
        for (Paper.Command command : paper.getCommands()) {
            switch (command.getKey()) {
                case Paper.COMMAND_IMAGE:
                    break;
                case Paper.COMMAND_LINE:
                    break;
                case Paper.COMMAND_LINES:
                    break;
                case Paper.COMMAND_CUT_PAPER:
                    break;
            }
        }
        sendDataImmediately(labelCommand.getCommand());
    }

    private void sendDataByCpcl(Paper paper) throws Exception {
        CpclCommand cpclCommand = new CpclCommand();
        for (Paper.Command command : paper.getCommands()) {
            switch (command.getKey()) {
                case Paper.COMMAND_IMAGE:
                    break;
                case Paper.COMMAND_LINE:
                    break;
                case Paper.COMMAND_LINES:
                    break;
                case Paper.COMMAND_CUT_PAPER:
                    break;
            }
        }
        sendDataImmediately(cpclCommand.getCommand());
    }

    public void sendDataImmediately(Vector<Byte> data) throws IOException {
        if (printerConnection == null) {
            return;
        }
        try {
            printerConnection.writeData(data, 0, data.size());
        } catch (Exception e) {
            // 异常中断
            throw e;
        }
    }

    public void sendByteDataImmediately(byte[] data) throws IOException {
        Vector<Byte> datas = new Vector<>();
        for (int i = 0; i < data.length; i++) {
            datas.add(Byte.valueOf(data[i]));
        }
        sendDataImmediately(datas);
    }

    public int readDataImmediately(byte[] buffer) throws IOException {
        return printerConnection.readData(buffer);
    }

    private void queryPrinterCommand() {
        queryPrinterCommandFlag = 0;
        ThreadPool.getInstance().addSerialTask(new Runnable() {
            @Override
            public void run() {
                final ThreadFactoryBuilder threadFactory = new ThreadFactoryBuilder("Timer");
                final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory);
                scheduledExecutorService.scheduleAtFixedRate(threadFactory.newThread(new Runnable() {
                    @Override
                    public void run() {
                        if (printerCommand == null && queryPrinterCommandFlag > CPCL) {
                            if (printerConnection instanceof UsbConnection) { // 三种状态查询，完毕均无返回值，默认票据（针对凯仕、盛源机器USB查询指令没有返回值，导致连不上）
                                printerCommand = PrinterCommand.ESC;
                                scheduledExecutorService.shutdown();
                            } else { // 三种状态，查询无返回值，发送连接失败广播
                                close();
                                scheduledExecutorService.shutdown();
                            }
                        }
                        if (printerCommand != null) {
                            scheduledExecutorService.shutdown();
                            return;
                        }
                        queryPrinterCommandFlag++;
                        try {
                            switch (queryPrinterCommandFlag) {
                                case ESC:
                                    sendByteDataImmediately(PrinterCommand.ESC.getCheckCommand());
                                    break;
                                case TSC:
                                    sendByteDataImmediately(PrinterCommand.TSC.getCheckCommand());
                                    break;
                                case CPCL:
                                    sendByteDataImmediately(PrinterCommand.CPCL.getCheckCommand());
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }), 1500, 1500, TimeUnit.MILLISECONDS);
            }
        });
    }

    class Reader extends Thread {
        private boolean isRun = true;

        private byte[] buffer = new byte[100];

        @Override
        public void run() {
            try {
                while (isRun) {
                    // 读取打印机返回信息,打印机没有返回值返回-1
                    int len = readDataImmediately(buffer);
                    if (printerCommand == null) {
                        switch (queryPrinterCommandFlag) {
                            case ESC:
                                printerCommand = PrinterCommand.ESC;
                                break;
                            case TSC:
                                printerCommand = PrinterCommand.TSC;
                                break;
                            case CPCL:
                                printerCommand = PrinterCommand.CPCL;
                                break;
                            default:
                                printerCommand = PrinterCommand.ESC;
                                break;
                        }
                    }
                    if (len < 0 || buffer[0] == getPrinterStatusCommand(PrinterStatus.NORMAL)) {
                        currentPrinterStatus = PrinterStatus.NORMAL;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.COVER_OPEN)) {
                        currentPrinterStatus = PrinterStatus.COVER_OPEN;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.FEEDING)) {
                        currentPrinterStatus = PrinterStatus.FEEDING;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_OUT)) {
                        currentPrinterStatus = PrinterStatus.PAPER_OUT;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_ERROR)) {
                        currentPrinterStatus = PrinterStatus.PAPER_ERROR;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.CARBON_OUT)) {
                        currentPrinterStatus = PrinterStatus.CARBON_OUT;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.ERROR)) {
                        if (printerCommand == PrinterCommand.ESC) {
                            // ESC查询错误状态
                            sendByteDataImmediately(new byte[]{0x10, 0x04, 0x03});
                            len = readDataImmediately(buffer);
                            if (len < 0) {
                                currentPrinterStatus = PrinterStatus.UNKNOWN_ERROR;
                            } else if (checkPrinterStatus(buffer[0], PrinterStatus.KNIFE_ERROR)) {
                                currentPrinterStatus = PrinterStatus.KNIFE_ERROR;
                            }
                            if (checkPrinterStatus(buffer[0], PrinterStatus.OVER_HEATING)) {
                                currentPrinterStatus = PrinterStatus.OVER_HEATING;
                            }
                            currentPrinterStatus = PrinterStatus.UNKNOWN_ERROR;
                        } else {
                            currentPrinterStatus = PrinterStatus.ERROR;
                        }
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.BLACK_LABEL_ERROR)) {
                        currentPrinterStatus = PrinterStatus.BLACK_LABEL_ERROR;
                    } else {
                        currentPrinterStatus = PrinterStatus.UNKNOWN_ERROR;
                    }
                    LogUtils.i("getPrinterStatus", currentPrinterStatus.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                close();
            }
        }

        public void cancel() {
            isRun = false;
        }
    }
}
