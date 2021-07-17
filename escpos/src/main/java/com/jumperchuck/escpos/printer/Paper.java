package com.jumperchuck.escpos.printer;

import android.graphics.Bitmap;

import com.jumperchuck.escpos.constant.PrinterStatus;

import java.util.ArrayList;
import java.util.List;

public class Paper {
    public static final String COMMAND_TEXT = "command_text";
    public static final String COMMAND_IMAGE = "command_image";
    public static final String COMMAND_HTML = "command_html";
    public static final String COMMAND_LINE = "command_line";
    public static final String COMMAND_LINES = "command_lines";
    public static final String COMMAND_BARCODE = "command_barcode";
    public static final String COMMAND_QRCODE = "command_qrcode";
    public static final String COMMAND_CUT_PAPER = "command_cut_paper";

    public static final String ALIGN_LEFT = "left";
    public static final String ALIGN_CENTER = "center";
    public static final String ALIGN_RIGHT = "right";

    public static final String SIZE_DEFAULT = "default";
    public static final String SIZE_LARGE = "large";
    public static final String SIZE_BIG = "big";

    private List<Command> commands = new ArrayList<>();

    private Listener listener;

    public List<Command> getCommands() {
        return commands;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return listener;
    }

    public Paper addText(String text) {
        this.commands.add(new Command(COMMAND_TEXT, text));
        return this;
    }

    public Paper addImage(String base64) {
        this.commands.add(new Command(COMMAND_IMAGE, base64));
        return this;
    }

    public Paper addImage(Bitmap bitmap) {
        this.commands.add(new Command(COMMAND_IMAGE, bitmap));
        return this;
    }

    public Paper addHtml(String html) {
        this.commands.add(new Command(COMMAND_HTML, html));
        return this;
    }

    public Paper addLine() {
        this.commands.add(new Command(COMMAND_LINE));
        return this;
    }

    public Paper addLines(int n) {
        this.commands.add(new Command(COMMAND_LINES, n));
        return this;
    }

    public Paper addBarcode(String value) {
        this.commands.add(new Command(COMMAND_BARCODE, value));
        return this;
    }

    public Paper addQrcode(String value) {
        this.commands.add(new Command(COMMAND_QRCODE, value));
        return this;
    }

    public Paper addCutPaper() {
        this.commands.add(new Command(COMMAND_CUT_PAPER));
        return this;
    }

    public interface Listener {
        void onPrintResult(EscPosPrinter printerManager, PrinterStatus printerStatus);
    }

    public static class Command {
        private String key;
        private Object value;

        public Command(String key) {
            this.key = key;
        }

        public Command(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
