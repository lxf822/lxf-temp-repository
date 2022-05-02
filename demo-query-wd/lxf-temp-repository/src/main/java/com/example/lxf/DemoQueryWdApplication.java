package com.example.lxf;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DemoQueryWdApplication {

    private static PoolingHttpClientConnectionManager cm;

    private static long httpClientTimeout = 60000;
    private static int httpClientMaxTotal = 20;
    private static int httpClientMaxPerRoute = 50;
    private static int count = 1;

    public static void initHttpClientCM(){
        // 创建http连接池，可以同时指定连接超时时间
        cm = new PoolingHttpClientConnectionManager(httpClientTimeout, TimeUnit.MILLISECONDS);
        // 最多同时连接20个请求
        cm.setMaxTotal(httpClientMaxTotal);
        // 每个路由最大连接数，路由指IP+PORT或者域名
        cm.setDefaultMaxPerRoute(httpClientMaxPerRoute);
    }

    public static CloseableHttpClient getHttpClient(PoolingHttpClientConnectionManager cm){
        //设置请求参数配置，创建连接时间、从连接池获取连接时间、数据传输时间、是否测试连接可用、构建配置对象
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .setSocketTimeout(10 * 1000)
                .setStaleConnectionCheckEnabled(true)
                .build();

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
    public static void doGetRequest(CloseableHttpClient httpClient, String url)  {
        //创建http请求类型
        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
            if (200 == httpResponse.getStatusLine().getStatusCode()) {
                System.out.println(count++ + " 请求返回数据内容：200 " + EntityUtils.toString(httpResponse.getEntity()));
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

    public static void queryWds() {
        String url = "http://www.baidu.com/s";
        int reqsNum = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for(int i = 0; i < reqsNum; i++){
            // 连接池中获取httpClient
            CloseableHttpClient httpClient = getHttpClient(cm);
            //多线程执行请求
            executorService.execute(() -> {
                doGetRequest(httpClient, url + "?wd=" + Math.random());
            });
        }
        executorService.shutdown();
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoQueryWdApplication.class, args);
        initHttpClientCM();
        queryWds();
    }

}
