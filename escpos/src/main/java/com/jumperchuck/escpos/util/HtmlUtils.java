package com.jumperchuck.escpos.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

public class HtmlUtils {
    private static WebView webView;

    public static Bitmap toBitmap(final Context context, final String html, final int width) {
        try {
            final Map<String, Bitmap> bitmaps = new HashMap();
            Thread thread = new Thread() {
                private boolean isRun = true;

                public void run() {
                    // ui线程创建webView
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (webView == null) {
                                webView = new WebView(context);
                                WebSettings webSettings = webView.getSettings();
                                webSettings.setJavaScriptEnabled(true);
                                webSettings.setLoadWithOverviewMode(true);
                                webSettings.setUseWideViewPort(true);
                                webSettings.setBuiltInZoomControls(true);
                                webSettings.setDisplayZoomControls(false);
                                webSettings.setDefaultTextEncodingName("utf-8");
                            }

                            webView.measure(width, 400);
                            webView.layout(0, 0, width, 400);
                            webView.loadData(html, "text/html", "UTF-8");
                            webView.setWebViewClient(new WebViewClient() {
                                @Override
                                public void onPageFinished(final WebView view, String url) {
                                    super.onPageFinished(view, url);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 设置成内容高度
                                            view.measure(width, view.getMeasuredHeight());
                                            view.layout(0, 0, width, view.getMeasuredHeight());
                                            view.loadData(html, "text/html", "UTF-8");
                                            view.setWebViewClient(new WebViewClient() {
                                                @Override
                                                public void onPageFinished(final WebView view, String url) {
                                                    super.onPageFinished(view, url);
                                                    new Handler().postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            int htmlContentHeight = view.getMeasuredHeight();
                                                            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), htmlContentHeight, Bitmap.Config.RGB_565);
                                                            Canvas canvas = new Canvas(bitmap);
                                                            view.draw(canvas);
                                                            bitmaps.put("bitmap", bitmap);
                                                            isRun = false;
                                                        }
                                                    }, 100);
                                                }
                                            });
                                        }
                                    }, 100);
                                }
                            });
                        }
                    });

                    while (isRun) { }
                }
            };

            thread.start();
            thread.join();

            return bitmaps.get("bitmap");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
