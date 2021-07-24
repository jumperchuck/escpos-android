package com.jumperchuck.escpos.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.jumperchuck.escpos.connection.TcpConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.constant.CommandType;
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
    private static String TAG = GeneralPrinter.class.getSimpleName();
    private final int ESC = 1;
    private final int TSC = 2;
    private final int CPCL = 3;

    private CommandType commandType;

    private int timeout;

    private int soTimeout;

    private Reader reader;

    private PrinterStatus currentStatus;

    private int queryPrinterCommandFlag;

    private GeneralPrinter(Builder builder) {
        super(builder);
        this.commandType = builder.commandType;
        this.timeout = builder.timeout;
        this.soTimeout = builder.soTimeout;
    }

    @Override
    public synchronized void open() {
        if (isConnected()) {
            return;
        }
        if (connection instanceof TcpConnection) {
            ((TcpConnection) connection).setTimeout(timeout);
        }
        currentStatus = null;
        listener.onOpening();
        connection.connect();
        if (isConnected()) {
            // 开启读取打印机返回数据线程
            reader = new Reader();
            reader.start();
            listener.onOpened();
            if (commandType == null) {
                // 查询打印机使用指令
                queryPrinterCommand();
            }
        } else {
            // 连接失败, 重连一次
            connection.connect();
            if (isConnected()) {
                // 开启读取打印机返回数据线程
                reader = new Reader();
                reader.start();
                listener.onOpened();
                if (commandType == null) {
                    // 查询打印机使用指令
                    queryPrinterCommand();
                }
            } else {
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        currentStatus = PrinterStatus.DISCONNECTED;
        if (reader != null) {
            reader.cancel();
            reader = null;
        }
        connection.disconnect();
        listener.onClosed();
        sendStatusBroadcast(currentStatus);
    }

    @Override
    public synchronized PrinterStatus print(Paper paper) {
        // 连接打印机
        if (!isConnected()) open();
        // 检测打印机状态
        PrinterStatus status = getPrinterStatus();

        if (status == PrinterStatus.NORMAL) {
            try {
                switch (commandType) {
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
            } catch (IOException e) {
                e.printStackTrace();
                close();
                status = PrinterStatus.UNKNOWN_ERROR;
            }
        }
        if (paper.getListener() != null) {
            paper.getListener().onPrintResult(this, status);
        }
        return status;
    }

    @Override
    public synchronized PrinterStatus getPrinterStatus() {
        PrinterStatus status = currentStatus;
        if (isConnected()) {
            try {
                currentStatus = null;
                if (commandType != null) {
                    sendData(commandType.getCheckCommand());
                }
                long startTime = System.currentTimeMillis();
                long currentTime = System.currentTimeMillis();
                while (currentTime - startTime < soTimeout) {
                    if (currentStatus != null) {
                        status = currentStatus;
                        break;
                    }
                    Thread.sleep(100);
                    currentTime = System.currentTimeMillis();
                }
            } catch (IOException e) {
                e.printStackTrace();
                close();
                status = PrinterStatus.DISCONNECTED;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            status = PrinterStatus.DISCONNECTED;
        }
        if (status == null) {
            status = PrinterStatus.UNKNOWN_ERROR;
        }
        sendStatusBroadcast(status);
        return status;
    }

    private int getPrinterStatusCommand(PrinterStatus printerStatus) {
        switch (commandType) {
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
        switch (commandType) {
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

    private void sendDataByEsc(Paper paper) throws IOException {
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
                    if (feedBeforeCut > 0) {
                        escCommand.addPrintAndFeedLines(feedBeforeCut);
                    }
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
                    switch ((Paper.BARCODE_TYPE) command.getValue2()) {
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
        sendData(escCommand.getCommand());
    }

    private void sendDataByTsc(Paper paper) throws IOException {
        LabelCommand labelCommand = new LabelCommand();
        sendData(labelCommand.getCommand());
    }

    private void sendDataByCpcl(Paper paper) throws IOException {
        CpclCommand cpclCommand = new CpclCommand();
        sendData(cpclCommand.getCommand());
    }

    public void sendData(Vector<Byte> data) throws IOException {
        try {
            connection.writeData(data);
        } catch (Exception e) {
            throw e;
        }
    }

    public void sendData(byte[] data) throws IOException {
        try {
            connection.writeData(data);
        } catch (IOException e) {
            throw e;
        }
    }

    public int readData(byte[] buffer) throws IOException {
        try {
            return connection.readData(buffer);
        } catch (IOException e) {
            throw e;
        }
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
                        if (commandType == null && queryPrinterCommandFlag > CPCL) {
                            if (connection instanceof UsbConnection) { // 三种状态查询，完毕均无返回值，默认票据（针对凯仕、盛源机器USB查询指令没有返回值，导致连不上）
                                commandType = CommandType.ESC;
                                scheduledExecutorService.shutdown();
                            } else if (reader != null) { // 三种状态，查询无返回值，发送连接失败广播
                                close();
                                scheduledExecutorService.shutdown();
                            }
                        }
                        if (commandType != null) {
                            if (!scheduledExecutorService.isShutdown()) {
                                scheduledExecutorService.shutdown();
                            }
                            return;
                        }
                        queryPrinterCommandFlag++;
                        try {
                            switch (queryPrinterCommandFlag) {
                                case ESC:
                                    sendData(CommandType.ESC.getCheckCommand());
                                    break;
                                case TSC:
                                    sendData(CommandType.TSC.getCheckCommand());
                                    break;
                                case CPCL:
                                    sendData(CommandType.CPCL.getCheckCommand());
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
                    int len = readData(buffer);
                    if (commandType == null) {
                        switch (queryPrinterCommandFlag) {
                            case ESC:
                                commandType = CommandType.ESC;
                                break;
                            case TSC:
                                commandType = CommandType.TSC;
                                break;
                            case CPCL:
                                commandType = CommandType.CPCL;
                                break;
                            default:
                                commandType = CommandType.ESC;
                                break;
                        }
                    }
                    LogUtils.d(TAG, "read: " + len + " buffer: " + buffer[0]);
                    if (len < 0 || buffer[0] == getPrinterStatusCommand(PrinterStatus.NORMAL)) {
                        currentStatus = PrinterStatus.NORMAL;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.COVER_OPEN)) {
                        currentStatus = PrinterStatus.COVER_OPEN;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.FEEDING)) {
                        currentStatus = PrinterStatus.FEEDING;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_OUT)) {
                        currentStatus = PrinterStatus.PAPER_OUT;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_ERROR)) {
                        currentStatus = PrinterStatus.PAPER_ERROR;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.CARBON_OUT)) {
                        currentStatus = PrinterStatus.CARBON_OUT;
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.ERROR)) {
                        if (commandType == CommandType.ESC) {
                            // ESC查询错误状态
                            sendData(new byte[]{0x10, 0x04, 0x03});
                            len = readData(buffer);
                            if (len < 0) {
                                currentStatus = PrinterStatus.UNKNOWN_ERROR;
                            } else if (checkPrinterStatus(buffer[0], PrinterStatus.KNIFE_ERROR)) {
                                currentStatus = PrinterStatus.KNIFE_ERROR;
                            } else if (checkPrinterStatus(buffer[0], PrinterStatus.OVER_HEATING)) {
                                currentStatus = PrinterStatus.OVER_HEATING;
                            } else {
                                currentStatus = PrinterStatus.UNKNOWN_ERROR;
                            }
                        } else {
                            currentStatus = PrinterStatus.ERROR;
                        }
                    } else if (checkPrinterStatus(buffer[0], PrinterStatus.BLACK_LABEL_ERROR)) {
                        currentStatus = PrinterStatus.BLACK_LABEL_ERROR;
                    } else {
                        currentStatus = PrinterStatus.UNKNOWN_ERROR;
                    }
                    LogUtils.d(TAG, "readPrinterStatus: " + currentStatus.getCode() + " / " + currentStatus.getMessage());
                    sendStatusBroadcast(currentStatus);
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

    public static class Builder extends EscPosPrinter.Builder<Builder> {

        CommandType commandType;

        int timeout = 6000;

        int soTimeout = 2000;

        public Builder(Context context) {
            super(context);
        }

        public Builder commandType(CommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder soTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
            return this;
        }

        @Override
        public GeneralPrinter build() {
            return new GeneralPrinter(this);
        }
    }
}
