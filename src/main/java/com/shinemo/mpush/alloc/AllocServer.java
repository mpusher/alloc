package com.shinemo.mpush.alloc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Created by yxx on 2016/5/6.
 *
 * @author ohun@live.cn
 */
public class AllocServer {

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9999), 0);//设置HttpServer的端口为80
        httpServer.createContext("/", new AllocHandler());
        httpServer.start();
    }

    public static class AllocHandler implements HttpHandler {
        private String mpushServers = "127.0.0.1:3000";//ip和端口从zk获取，并根据redis中的链接数排序
        private Charset UTF_8 = Charset.forName("UTF-8");

        public void handle(HttpExchange exchange) throws IOException {
            byte[] data = mpushServers.getBytes(UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            OutputStream out = exchange.getResponseBody();
            out.write(data);
            out.close();
        }
    }
}
