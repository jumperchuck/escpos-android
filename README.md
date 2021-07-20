# escpos-android

> ESC/POS热敏打印机安卓库，支持（蓝牙，TCP/IP，USB，商米打印），支持打印html

## 安装

1. 添加 JitPack 到项目根目录的 `build.gradle` 下
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. 添加依赖到项目中
```
dependencies {
    ...
    implementation 'com.github.jumperchuck:escpos-android:0.0.1'
}
```

3. 在 `App` 入口注册
```java
import com.jumperchuck.escpos.PrinterManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PrinterManager.init(this);
    }
}
```

4. 在 `AndroidManifest.xml` 内添加需要的权限
```xml
<!--蓝牙-->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<!--TCP/IP-->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!--USB-->
<uses-feature android:name="android.hardware.usb.host" />
```

## 创建一个EscPosPrinter

### TCP/IP
```java
EscPosPrinter printer = PrinterManager.tcpPrinter("192.168.123.100", 9100)
    .id(1) // 标示打印机id
    .name("name") // 标示打印机名称
    .printWidth(PrintWidth.WIDTH_58.getWidth()) // 设置打印宽度，默认是58mm打印机
    .printerCommand(PrinterCommand.ESC) // 设置指令类型
    .listener(new EscPosPrinter.Listener() {
        @Override
        public void onPrinted(Paper paper, PrinterStatus printerStatus) {

        }

        @Override
        public void onError(PrinterStatus printerStatus) {

        }
    })
    .build();
```

### 商米
```java
EscPosPrinter printer = PrinterManager.sunmiPrinter()
    ...
    .build();
```
            
### 蓝牙
```java
EscPosPrinter printer = PrinterManager.bluetoothPrinter(macAddress)
    ...
    .build();
```

### USB
```java
EscPosPrinter printer = PrinterManager.usbPrinter(usbDevice)
    ...
    .build();
```

## 构造打印数据
                
```java
Paper paper = new Paper();
paper.addText("123"); // 打印文字
paper.addBarcode(""); // 打印条形码
paper.addQrcode(""); // 打印二维码
paper.addLine(); // 走纸1行
paper.addLines(2); // 走纸2行
paper.addImage(bitmap); // 打印图片
paper.addCutPaper(); // 切刀
paper.addHtml("<html>...</html>"); // 打印html
paper.addCutPaper(); // 切刀
paper.setListener(new Paper.Listener() {
    @Override
    public void onPrintResult(EscPosPrinter printerManager, PrinterStatus printerStatus) {
    
    }
});
```

## 连接打印机打印

`open` / `print` / `getPrinterStatus` 会堵塞线程，需要放置其他线程中处理

```java
new Thread(new Runnable() {
    public void run() {
        printer.open();
        PrinterStatus result = printer.print(paper);
    }
}).start();
```

## 获取打印机状态
```java
new Thread(new Runnable() {
    public void run() {
        PrinterStatus result = printer.getPrinterStatus(paper);
    }
}).start();
```

## 断开打印机

```java
printer.close();
```

## 创建一个DeviceScanner

查询可用的蓝牙/wifi设备列表

### 蓝牙
```java
PrinterManager.bluetoothScanner();
```

### WLAN
```java
PrinterManager.wlanScanner();
```

## 问题

### 打印乱码
某些热敏打印机不支持多线程，同一个时间内有2个以上的连接同时发送打印指令，打印机就有可能会乱码
1. 进入打印机设置页面，最大线程数修改为1（不建议修改）
2. 单例模式，每个设备创建的打印机对象全局只存在一个。open / print / getPrinterStatus / close方法已添加synchronized，不用担心多线程的问题

### 打印队列

### 中止打印任务/调整打印顺序
