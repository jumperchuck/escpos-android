package com.jumperchuck.escpos.command;

import android.graphics.Bitmap;

import com.jumperchuck.escpos.connection.PrinterConnection;
import com.jumperchuck.escpos.connection.UsbConnection;
import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.CommandType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;
import com.jumperchuck.escpos.constant.PrinterStatus;
import com.jumperchuck.escpos.util.ReadUtils;
import com.jumperchuck.escpos.util.ThreadFactoryBuilder;
import com.jumperchuck.escpos.util.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FactoryCommander implements PrinterCommander {
    private static final int ESC = 1;
    private static final int TSC = 2;
    private static final int CPCL = 3;

    private PrinterCommander commander;
    private int queryCommandFlag;
    private byte[] buffer = new byte[100];

    @Override
    public PrinterCommander.Reader createReader(PrinterConnection connection) {
        if (commander != null) {
            return commander.createReader(connection);
        }
        return new Reader(connection);
    }

    @Override
    public PrinterCommander.Sender createSender(PrinterConnection connection) {
        if (commander != null) {
            return commander.createSender(connection);
        }
        return new Sender(connection);
    }

    public class Reader extends Thread implements PrinterCommander.Reader {
        private PrinterConnection connection;
        private PrinterCommander.Reader reader;

        public Reader(PrinterConnection connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                // 读取打印机返回信息,打印机没有返回值返回-1
                connection.readData(buffer);
                if (commander == null) {
                    switch (queryCommandFlag) {
                        case ESC:
                            commander = new EscCommander();
                            break;
                        case TSC:
                            commander = new TscCommander();
                            break;
                        case CPCL:
                            commander = new CpclCommander();
                            break;
                        default:
                            commander = new EscCommander();
                            break;
                    }
                }
                if (!isInterrupted()) {
                    reader = commander.createReader(connection);
                    reader.startRead();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void startRead() {
            start();
            ThreadPool.getInstance().addSerialTask(() -> {
                final ThreadFactoryBuilder threadFactory = new ThreadFactoryBuilder("Timer");
                final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory);
                scheduledExecutorService.scheduleAtFixedRate(threadFactory.newThread(() -> {
                    if (queryCommandFlag > CPCL) {
                        if (commander == null) {
                            if (connection instanceof UsbConnection) {
                                // 三种状态查询，完毕均无返回值，默认票据（针对凯仕、盛源机器USB查询指令没有返回值，导致连不上）
                                commander = new EscCommander();
                            } else {
                                // 三种状态，查询无返回值，发送连接失败广播
                                connection.disconnect();
                                cancelRead();
                            }
                        }
                        scheduledExecutorService.shutdown();
                    }
                    if (commander != null || isInterrupted()) {
                        if (!scheduledExecutorService.isShutdown()) {
                            scheduledExecutorService.shutdown();
                        }
                        return;
                    }
                    queryCommandFlag++;
                    try {
                        switch (queryCommandFlag) {
                            case ESC:
                                connection.writeData(CommandType.ESC.getCheckCommand());
                                break;
                            case TSC:
                                connection.writeData(CommandType.TSC.getCheckCommand());
                                break;
                            case CPCL:
                                connection.writeData(CommandType.CPCL.getCheckCommand());
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }), 1500, 1500, TimeUnit.MILLISECONDS);
            });
        }

        @Override
        public void cancelRead() {
            interrupt();
            if (reader != null) {
                reader.cancelRead();
            }
        }

        @Override
        public PrinterStatus updateStatus(int soTimeout) throws IOException {
            ReadUtils.readSync(() -> reader, 4000);
            if (reader == null) {
                return PrinterStatus.READ_TIMEOUT;
            } else {
                return reader.updateStatus(soTimeout);
            }
        }
    }

    public class Sender implements PrinterCommander.Sender {
        private PrinterConnection connection;
        private List<Task> tasks;

        private Sender(PrinterConnection connection) {
            this.connection = connection;
            this.tasks = new ArrayList<>();
        }

        @Override
        public void addInit() {
            this.tasks.add(sender -> sender.addInit());
        }

        @Override
        public void addAlign(AlignType type) {
            this.tasks.add(sender -> sender.addAlign(type));
        }

        @Override
        public void addText(String text) {
            this.tasks.add(sender -> sender.addText(text));
        }

        @Override
        public void addBitmap(Bitmap bitmap, int width) {
            this.tasks.add(sender -> sender.addBitmap(bitmap, width));
        }

        @Override
        public void addPrint() {
            this.tasks.add(sender -> sender.addPrint());
        }

        @Override
        public void addLine() {
            this.tasks.add(sender -> sender.addLine());
        }

        @Override
        public void addLines(byte n) {
            this.tasks.add(sender -> sender.addLines(n));
        }

        @Override
        public void addCutPaper() {
            this.tasks.add(sender -> sender.addCutPaper());
        }

        @Override
        public void addBarcode(String content, BarcodeType type, byte height, byte width, HriPosition hriPosition) {
            this.tasks.add(sender -> sender.addBarcode(content, type, height, width, hriPosition));
        }

        @Override
        public void addQRCode(String content, byte moduleSize, ErrorLevel errorLevel) {
            this.tasks.add(sender -> sender.addQRCode(content, moduleSize, errorLevel));
        }

        @Override
        public int startSend() throws IOException {
            ReadUtils.readSync(() -> commander, 5000);
            if (commander == null) {
                throw new IOException();
            }
            PrinterCommander.Sender sender = commander.createSender(connection);
            for (Task task : tasks) {
                task.invoke(sender);
            }
            return sender.startSend();
        }
    }

    private interface Task {
        void invoke(PrinterCommander.Sender sender);
    }
}
