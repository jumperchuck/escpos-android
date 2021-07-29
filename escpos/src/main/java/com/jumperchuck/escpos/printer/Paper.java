package com.jumperchuck.escpos.printer;

import android.graphics.Bitmap;

import com.jumperchuck.escpos.constant.AlignType;
import com.jumperchuck.escpos.constant.BarcodeType;
import com.jumperchuck.escpos.constant.ErrorLevel;
import com.jumperchuck.escpos.constant.HriPosition;

import java.util.ArrayList;
import java.util.List;

public class Paper {
    public static final String COMMAND_INIT = "command_init";
    public static final String COMMAND_ALIGN= "command_align";
    public static final String COMMAND_TEXT = "command_text";
    public static final String COMMAND_IMAGE = "command_image";
    public static final String COMMAND_HTML = "command_html";
    public static final String COMMAND_LINE = "command_line";
    public static final String COMMAND_LINES = "command_lines";
    public static final String COMMAND_BARCODE = "command_barcode";
    public static final String COMMAND_QRCODE = "command_qrcode";
    public static final String COMMAND_CUT_PAPER = "command_cut_paper";
    public static final String COMMAND_BEEP = "command_beep";
    public static final String COMMAND_OPEN_DRAWER = "command_open_drawer";

    private List<Command> commands = new ArrayList<>();

    public List<Command> getCommands() {
        return commands;
    }

    public Paper addInit() {
        this.commands.add(new Command(COMMAND_INIT));
        return this;
    }

    public Paper addAlign(AlignType align) {
        this.commands.add(new Command(COMMAND_ALIGN, align));
        return this;
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

    public Paper addLines(byte n) {
        this.commands.add(new Command(COMMAND_LINES, n));
        return this;
    }

    public Paper addBarcode(String value, BarcodeType type, byte height, byte width, HriPosition hriPosition) {
        this.commands.add(new Command(COMMAND_BARCODE, value, type, height, width, hriPosition));
        return this;
    }

    public Paper addQRCode(String value, byte moduleSize, ErrorLevel errorLevel) {
        this.commands.add(new Command(COMMAND_QRCODE, value, moduleSize, errorLevel));
        return this;
    }

    public Paper addCutPaper() {
        this.commands.add(new Command(COMMAND_CUT_PAPER));
        return this;
    }

    public Paper addBeep(byte n, byte time) {
        this.commands.add(new Command(COMMAND_BEEP, n, time));
        return this;
    }

    public Paper addOpenDrawer() {
        this.commands.add(new Command(COMMAND_OPEN_DRAWER));
        return this;
    }

    public static class Command {
        private String key;
        private Object value;
        private Object value2;
        private Object value3;
        private Object value4;
        private Object value5;

        public Command(String key) {
            this.key = key;
        }

        public Command(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Command(String key, Object value, Object value2) {
            this.key = key;
            this.value = value;
            this.value2 = value2;
        }

        public Command(String key, Object value, Object value2, Object value3) {
            this.key = key;
            this.value = value;
            this.value2 = value2;
            this.value3 = value3;
        }

        public Command(String key, Object value, Object value2, Object value3, Object value4) {
            this.key = key;
            this.value = value;
            this.value2 = value2;
            this.value3 = value3;
            this.value4 = value4;
        }

        public Command(String key, Object value, Object value2, Object value3, Object value4, Object value5) {
            this.key = key;
            this.value = value;
            this.value2 = value2;
            this.value3 = value3;
            this.value4 = value4;
            this.value5 = value5;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object getValue2() {
            return value2;
        }

        public Object getValue3() {
            return value3;
        }

        public Object getValue4() {
            return value4;
        }

        public Object getValue5() {
            return value5;
        }
    }
}
