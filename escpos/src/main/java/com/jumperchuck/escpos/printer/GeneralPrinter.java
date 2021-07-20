package com.jumperchuck.escpos.printer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
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

    private PrinterStatus currentPrinterStatus;

    public GeneralPrinter(Builder builder) {
        super(builder);
    }

    @Override
    public synchronized void open() {
        if (isConnected()) {
            return;
        }
        currentPrinterStatus = null;
        printerConnection.connect();
        if (isConnected()) {
            // 开启读取打印机返回数据线程
            reader = new Reader();
            reader.start();
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
        printerConnection.disconnect();
    }

    @Override
    public synchronized PrinterStatus print(Paper paper) {
        if (!isConnected()) {
            open();
        }
        PrinterStatus printerStatus = getPrinterStatus();
        if (printerStatus == PrinterStatus.NORMAL) {
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
                printerStatus = PrinterStatus.UNKNOWN_ERROR;
            }
        }
        if (paper.getListener() != null) {
            paper.getListener().onPrintResult(this, printerStatus);
        }
        return printerStatus;
    }

    @Override
    public synchronized PrinterStatus getPrinterStatus() {
        if (!isConnected()) {
            return PrinterStatus.DISCONNECTED;
        }
        try {
            currentPrinterStatus = null;
            if (printerCommand != null) {
                sendByteDataImmediately(printerCommand.getCheckCommand());
            }
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            while (currentTime - startTime < 1500) {
                if (currentPrinterStatus != null) {
                    return currentPrinterStatus;
                }
                Thread.sleep(100);
                currentTime = System.currentTimeMillis();
            }
            if (currentPrinterStatus != null) {
                return currentPrinterStatus;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PrinterStatus.UNKNOWN_ERROR;

       // try {
       //     if (printerCommand != null) {
       //         sendByteDataImmediately(printerCommand.getCheckCommand());
       //     }
       //     byte[] buffer = new byte[100];
       //     int len = readDataImmediately(buffer);
       //     if (printerCommand == null) {
       //         switch (queryPrinterCommandFlag) {
       //             case ESC:
       //                 printerCommand = PrinterCommand.ESC;
       //                 break;
       //             case TSC:
       //                 printerCommand = PrinterCommand.TSC;
       //                 break;
       //             case CPCL:
       //                 printerCommand = PrinterCommand.CPCL;
       //                 break;
       //             default:
       //                 printerCommand = PrinterCommand.ESC;
       //                 break;
       //         }
       //     }
       //     LogUtils.i("getPrinterStatus", "" + buffer[0] + " " + buffer[len - 1]);
       //     if (len < 0 || buffer[0] == getPrinterStatusCommand(PrinterStatus.NORMAL)) {
       //         return PrinterStatus.NORMAL;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.COVER_OPEN)) {
       //         return PrinterStatus.COVER_OPEN;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.FEEDING)) {
       //         return PrinterStatus.FEEDING;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_OUT)) {
       //         return PrinterStatus.PAPER_OUT;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.PAPER_ERROR)) {
       //         return PrinterStatus.PAPER_ERROR;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.CARBON_OUT)) {
       //         return PrinterStatus.CARBON_OUT;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.ERROR)) {
       //         if (printerCommand == PrinterCommand.ESC) {
       //             // ESC查询错误状态
       //             sendByteDataImmediately(new byte[]{0x10, 0x04, 0x03});
       //             len = readDataImmediately(buffer);
       //             if (len < 0) return PrinterStatus.UNKNOWN_ERROR;
       //             if (checkPrinterStatus(buffer[0], PrinterStatus.KNIFE_ERROR)) {
       //                 return PrinterStatus.KNIFE_ERROR;
       //             }
       //             if (checkPrinterStatus(buffer[0], PrinterStatus.OVER_HEATING)) {
       //                 return PrinterStatus.OVER_HEATING;
       //             }
       //             return PrinterStatus.UNKNOWN_ERROR;
       //         }
       //         return PrinterStatus.ERROR;
       //     }
       //     if (checkPrinterStatus(buffer[0], PrinterStatus.BLACK_LABEL_ERROR)) {
       //         return PrinterStatus.BLACK_LABEL_ERROR;
       //     }
       // } catch (Exception e) {
       //     e.printStackTrace();
       //     close();
       // }
       // return PrinterStatus.UNKNOWN_ERROR;
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
        escCommand.addInitializePrinter();
        for (Paper.Command command : paper.getCommands()) {
            switch (command.getKey()) {
                case Paper.COMMAND_IMAGE:
                    Bitmap bitmap = null;
                    Object value = command.getValue();
                    if (value instanceof String) {
                        // base64
                        byte[] base64Bytes = Base64.decode((String) command.getValue(), Base64.DEFAULT);
                        bitmap = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.length);
                    } else if (value instanceof Bitmap) {
                        bitmap = (Bitmap) value;
                    }
                    escCommand.addRastBitImage(bitmap, getPrintWidth(), 0);
                    escCommand.addPrintAndFeedLines((byte) 4);
                    break;
                case Paper.COMMAND_HTML:
                    Bitmap htmlBitmap = HtmlUtils.toBitmap(getContext(), (String) command.getValue(), getPrintWidth());
                    if (htmlBitmap != null) {
                        escCommand.addRastBitImage(htmlBitmap, getPrintWidth(), 0);
                        escCommand.addPrintAndFeedLines((byte) 4);
                    }
                    break;
                case Paper.COMMAND_LINE:
                    escCommand.addPrintAndLineFeed();
                    break;
                case Paper.COMMAND_LINES:
                    escCommand.addPrintAndFeedLines((byte) command.getValue());
                    break;
                case Paper.COMMAND_CUT_PAPER:
                    escCommand.addCutPaper();
                    break;
                case Paper.COMMAND_TEXT:
                    escCommand.addText((String) command.getValue());
                    break;
                case Paper.COMMAND_BARCODE:
                    escCommand.addCODABAR((String) command.getValue());
                    break;
                case Paper.COMMAND_QRCODE:
                    escCommand.addStoreQRCodeData((String) command.getValue());
                    escCommand.addPrintQRCode();
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
        queryPrinterCommandFlag = ESC;
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
                        queryPrinterCommandFlag++;
                    }
                }), 1500, 1500, TimeUnit.MILLISECONDS);
            }
        });
    }

    class Reader extends Thread {
        private boolean isRun = true;

        private byte[] buffer = new byte[1];

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
                    if (len > 0) {
                        LogUtils.i("getPrinterStatus", "" + buffer[0] + " " + buffer[len - 1]);
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
