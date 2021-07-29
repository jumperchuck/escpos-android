# escpos-android

> ESC/POS热敏打印机安卓库，封装佳博SDK和商米SDK，支持（蓝牙，TCP/IP，USB，商米打印），支持打印html

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
    implementation 'com.github.jumperchuck:escpos-android:0.1.0'
}
```

3. 在 `AndroidManifest.xml` 内添加需要的权限
```xml
<!--蓝牙-->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<!--TCP/IP-->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!--USB-->
<uses-feature android:name="android.hardware.usb.host" />
```

4. 在 `App` 入口注册
```java
import com.jumperchuck.escpos.App;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PrinterManager.init(this);
    }
}
```

## 创建一个Printer

### TCP/IP
```java
EscPosPrinter printer = PrinterManager.tcpPrinter("192.168.123.100", 9100)
    .id(1) // 标示打印机id
    .name("name") // 标示打印机名称
    .printWidth(PrintWidth.WIDTH_80.getWidth()) // 设置打印宽度，默认是80mm打印机
    .feedBeforeCut((byte) 3) // 设置每次切刀前走纸
    .timeout(6000) // 设置连接超时时间
    .soTimeout(4000) // 设置读取打印机数据超时时间
    .commander(new FactoryCommander()) // 设置打印指令，默认是FactoryCommander，如果确定打印机指令类型，可以传入Esc/Tsc/Cpcl/SunmiCommander以提升首次打印速度
    .listener(new EscPosPrinter.Listener() {
        @Override
        public void onStatusChanged(PrinterStatus printerStatus) {
            
        }
        
        @Override
        public void onPrinted(Paper paper, PrinterStatus printerStatus) {
        
        }
        
        @Override
        public void onError(Exception e) {
        
        }
    })
    .build();
```

> 默认是FactoryCommander，此指令器初始化时会按照ESC/TSC/CPCL顺序依此发送检测指令给打印机来判断打印机指令，因此首次打印会有耗时

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

### 构造打印数据
                
```java
Paper paper = new Paper();
paper.addAlign(AlignType.LEFT); // 设置对齐方式
paper.addText("123"); // 打印文字
paper.addImage(bitmap); // 打印图片
paper.addHtml("<html>...</html>"); // 打印html
paper.addQrcode("", (byte) 3, ErrorLevel.H); // 打印二维码
paper.addBarcode("123", BarcodeType.CODABAR, (byte) 40, (byte) 2, HriPosition.NONE); // 打印条形码
paper.addLine(); // 走纸1行
paper.addLines(2); // 走纸2行
paper.addCutPaper(); // 切刀
paper.addBeep((byte) 3, (byte) 5); // 蜂鸣，(次数, 时间)
paper.addOpenDrawer(); // 打开钱箱
```

### 连接打印机打印

`connect` / `disconnect` / `print` / `getPrinterStatus` 等IO操作会堵塞线程，需要放置其他线程中处理

```java
new Thread(new Runnable() {
    public void run() {
        printer.connect();
        PrintResult result = printer.print(paper);
        boolean sent = result.isSent(); // 是否发送成功
        int size = result.getTotalBytes(); // 数据大小
    }
}).start();
```

### 获取打印机状态
```java
new Thread(new Runnable() {
    public void run() {
        PrinterStatus result = printer.getPrinterStatus();
    }
}).start();
```

### 断开打印机

```java
new Thread(new Runnable() {
    public void run() {
        printer.disconnect();
    }
}).start();
```

## 创建一个Scanner

查询可用的蓝牙/wifi设备列表

### 蓝牙
```java
DeviceScanner scanner = PrinterManager.bluetoothScanner()
    .listener(new DeviceScanner.Listener() {
        @Override
        public void onStarted() {
            
        }

        @Override
        public void onDiscovery(Object device) {
            (BluetoothDevice) device;
        }

        @Override
        public void onScanned(List<Object> devices) {
            List<BluetoothDevice> deviceList = devices.stream()
                .map(item -> ((BluetoothDevice) item))
                .collect(Collectors.toList());
        }

        @Override
        public void onStopped() {
            
        }

        @Override
        public void onError(Exception e) {

        }
    })
    .build();
```

### WLAN
```java
DeviceScanner scanner = PrinterManager.wlanScanner()
    .listener(new DeviceScanner.Listener() {
        @Override
        public void onStarted() {
            
        }

        @Override
        public void onDiscovery(Object device) {
            (InetAddress) device;
        }

        @Override
        public void onScanned(List<Object> devices) {
            List<BluetoothDevice> deviceList = devices.stream()
                .map(item -> ((InetAddress) item))
                .collect(Collectors.toList());
        }

        @Override
        public void onStopped() {
            
        }

        @Override
        public void onError(Exception e) {

        }
    })
    .build();
```

### 开始/停止查询设备
```java
scanner.startScan();
scanner.stopScan();
```

## 自定义连接器

连接器负责连接设备以及读写数据

> 目前仅有Tcp/Bluetooth/Usb/SunmiConnection

```java
public class MyConnection extends PrinterConnection {
    @Override
    public void connect() {}
    
    @Override
    public void disconnect() {}
    
    @Override
    public boolean isConnected() {}
    
    @Override
    public void writeData(byte[] data, int off, int len) throws IOException {}
    
    @Override
    public void readData(byte[] bytes) throws IOException {}
}
```

使用

```java
EscPosPrinter printer = new GeneralPrinter.Builder(context)
    .connection(new MyConnection())
    .build();
```

## 自定义指令器

指令器负责通过连接器解析设备返回的数据以及构造发送打印数据

> 目前仅有Esc/Tsc/Cpcl/Factory/SunmiCommander，EscCommander为防止打印乱码默认启用一票一控模式，只处理了佳博打印机，其他打印机需自行处理
> 不支持一票一控的打印机可通过添加流控来等待打印机打印完成，数据总大小 / 打印机每秒处理数据大小

```java
public class MyCommander implements PrinterCommander {
    @Override
    public Reader createReader(PrinterConnection connection) {
        return new Reader();
    }
    
    @Override
    public Sender createSender(PrinterConnection connection) {
        return new Sender();
    }
    
    public class Reader implements PrinterCommander.Reader {
        private Reader(PrinterConnection connection) {}
        
        @Override
        public void startRead() {
            // 开始读取打印机返回数据
        }
        
        @Override
        public void cancelRead() {
            // 取消读取打印机返回数据
        }
        
        @Override
        public PrinterStatus updateStatus(int soTimeout) throws IOException {
            // 更新打印机实时状态
        }
    }
    
    public class Sender implements PrinterCommander.Sender {
        private Sender(PrinterConnection connection) {}
        
        @Override
        public void addInit() {}
        
        ...
        
        @Override
        public int startSend() throws IOException {
            // 构造打印机打印数据发送，返回总数据大小
        }
    }
}
```

```使用
EscPosPrinter printer = new GeneralPrinter.Builder(context)
    .commander(new MyCommander())
    .build();
```