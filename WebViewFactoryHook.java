package com.demo;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class WebViewFactoryHook {

    public static void hookWebViewFactory() {
        try {
            // 1. 获取 WebViewFactory 的 Class 对象
            Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
            Class<?> providerClass = Class.forName("android.webkit.WebViewFactoryProvider");

            // 2. 获取 sProviderInstance 字段
            Field sProviderInstanceField = webViewFactoryClass.getDeclaredField("sProviderInstance");

            // 3. 设置字段为可访问
            sProviderInstanceField.setAccessible(true);
            // 获取原始provider
            Object rawProvider = sProviderInstanceField.get(null);

            // 4. 创建自定义的 CookieManager
            CookieManager rawCookieManager = CookieManager.getInstance();
            CustomCookieManager customCookieManager = new CustomCookieManager(rawCookieManager);

            // 4. 创建 WebViewFactoryProvider 的动态代理对象
            Object providerProxy = Proxy.newProxyInstance(
                    providerClass.getClassLoader(),
                    new Class<?>[]{providerClass},
                    new WebViewFactoryProviderHandler(customCookieManager, rawProvider)
            );

            // 5. 用代理对象替换现有的 sProviderInstance
            sProviderInstanceField.set(null, providerProxy);

            System.out.println("Hook completed! sProviderInstance replaced with proxy.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CustomCookieManager extends CookieManager {
        private final CookieManager mCookieManager;

        public CustomCookieManager(CookieManager cookieManager) {
            this.mCookieManager = cookieManager;
        }

        public static void printStackTrace(String url, String value) {
            if (value.contains("=;")) {
                Log.d("cookie", "---clear cookie:" + url + ",\t value:" + value);
            }
            Log.d("cookie", "+++set cookie:" + url + ",\t value:" + value);
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                Log.d("cookieStack", element.toString());
            }
        }

        @Override
        public void setAcceptCookie(boolean accept) {
            mCookieManager.setAcceptCookie(accept);
        }

        @Override
        public boolean acceptCookie() {
            return mCookieManager.acceptCookie();
        }

        @Override
        public void setAcceptThirdPartyCookies(WebView webview, boolean accept) {
            mCookieManager.setAcceptThirdPartyCookies(webview, accept);
        }

        @Override
        public boolean acceptThirdPartyCookies(WebView webview) {
            return mCookieManager.acceptThirdPartyCookies(webview);
        }

        @Override
        public void setCookie(String url, String value) {

            printStackTrace(url, value);
            mCookieManager.setCookie(url, value);
        }

        @Override
        public void setCookie(String url, String value, @Nullable ValueCallback<Boolean> callback) {
            printStackTrace(url, value);
            mCookieManager.setCookie(url, value, callback);
        }

        @Override
        public String getCookie(String url) {
            return mCookieManager.getCookie(url);
        }

        @Override
        public void removeSessionCookie() {
            mCookieManager.removeSessionCookie();
        }

        @Override
        public void removeSessionCookies(@Nullable ValueCallback<Boolean> callback) {
            mCookieManager.removeSessionCookies(callback);
        }

        @Override
        public void removeAllCookie() {
            mCookieManager.removeAllCookie();
        }

        @Override
        public void removeAllCookies(@Nullable ValueCallback<Boolean> callback) {
            mCookieManager.removeAllCookies(callback);
        }

        @Override
        public boolean hasCookies() {
            return mCookieManager.hasCookies();
        }

        @Override
        public void removeExpiredCookie() {
            mCookieManager.removeExpiredCookie();
        }

        @Override
        public void flush() {
            mCookieManager.flush();
        }
    }

    // 示例用法


    // 动态代理处理器，实现 InvocationHandler 接口
    private static class WebViewFactoryProviderHandler implements InvocationHandler {

        private final CookieManager cookieManager;
        private final Object rawProvider;


        public WebViewFactoryProviderHandler(CustomCookieManager customCookieManager, Object rawProvider) {
            this.cookieManager = customCookieManager;
            this.rawProvider = rawProvider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 检查方法名，如果是 getCookieManager，则返回我们的自定义 CookieManager
            if ("getCookieManager".equals(method.getName())) {
                return cookieManager;
            } else {
                return method.invoke(rawProvider, args);
            }
        }
    }
}
