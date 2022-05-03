package com.example.lxf;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import sun.plugin.util.PluginSysUtil;

import java.io.*;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class DemoQueryWdApplicationTests {
    private static PoolingHttpClientConnectionManager cm;
    private static long httpClientTimeout = 60000;
    private static int httpClientMaxTotal = 20;
    private static int httpClientMaxPerRoute = 50;
    private static AtomicInteger count = new AtomicInteger(0);
    private static CloseableHttpClient httpClient = null;
    private static RequestConfig requestConfig = null;

    @BeforeAll
    public static void initHttpClientCM(){
        // 创建http连接池，可以同时指定连接超时时间
        cm = new PoolingHttpClientConnectionManager(httpClientTimeout, TimeUnit.MILLISECONDS);
        // 最多同时连接20个请求
        cm.setMaxTotal(httpClientMaxTotal);
        // 每个路由最大连接数，路由指IP+PORT或者域名
        cm.setDefaultMaxPerRoute(httpClientMaxPerRoute);

        requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .setSocketTimeout(10 * 1000)
                .setStaleConnectionCheckEnabled(true)
                .build();
        System.out.println("初始化");
    }

    public  CloseableHttpClient getHttpClient( ){
        //创建httpClient时从连接池中获取，并设置连接失败时自动重试（也可以自定义重试策略：setRetryHandler()）
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(cm)
                .disableAutomaticRetries()
                .build();
        return httpClient;
    }

    /**
     * 执行请求
     */
    void doGetRequest( String url)  {
        //创建http请求类型
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse httpResponse = null;

        try {
            httpResponse = getHttpClient().execute(httpGet);
            if (200 == httpResponse.getStatusLine().getStatusCode()) {
                synchronized (this) {
                    System.out.println("第{"+count.incrementAndGet()+"}次请求：" + " 请求响应状态码：200 ");//+ EntityUtils.toString(httpResponse.getEntity()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                //执行httpResponse.close关闭对象会关闭连接池，
                //如果需要将连接释放到连接池，可以使用EntityUtils.consume()方法
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public  void queryWds() {
        String url = "http://www.baidu.com/s";
        ExecutorService executorService = Executors.newCachedThreadPool();
        for(int i = 0; i < 20; i++){
            //多线程执行请求
            executorService.execute(() -> {
                doGetRequest(url + "?wd=" + Math.random());
            });
        }
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(20,Integer.valueOf(count.toString()));
        executorService.shutdown();
    }

}
