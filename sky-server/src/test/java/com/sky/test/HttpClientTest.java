package com.sky.test;

import com.google.gson.JsonObject;
import net.minidev.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HttpClientTest{
    /**
     * httpClient发送get请求
     * @throws IOException
     */
    @Test
    public void testGet() throws IOException {
        //创建HttpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //创建请求对象
        HttpGet httpGet = new HttpGet("http://localhost:8080/user/shop/status");
        //发送请求获取响应结果
        CloseableHttpResponse response = httpClient.execute(httpGet);

        //获取响应状态码
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("服务端返回的状态码是"+statusCode);

        //获取服务端返回的数据
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        System.out.println("服务端返回的数据是"+body);

        //关闭资源
        response.close();
        httpClient.close();
    }

    /**
     * httpClient发送post请求
     */
    @Test
    public void testPost() throws Exception {
        //创建httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //创建请求对象
        HttpPost httpPost = new HttpPost("http://localhost:8080/admin/employee/login");
        //设置请求体参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "admin");
        jsonObject.put("password", "123456");
        StringEntity stringEntity = new StringEntity(jsonObject.toString());

        //设置编码方式
        stringEntity.setContentEncoding("utf-8");
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);

        //发送请求
        CloseableHttpResponse response = httpClient.execute(httpPost);

        //获取请求状态码
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("请求状态码是"+statusCode);

        //获取响应数据
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        System.out.println("响应数据是"+body);
    }
}
