package com.shinemo.mpush.alloc;

import com.google.common.base.Joiner;
import com.mpush.zk.ZKClient;
import com.mpush.zk.listener.ZKServerNodeWatcher;
import com.mpush.zk.node.ZKServerNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by yxx on 2016/5/6.
 *
 * @author ohun@live.cn
 */
public class AllocServer {

    public static void main(String[] args) throws IOException {//正式环境可以用tomcat
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 9999), 0);
        httpServer.createContext("/", new AllocHandler());
        httpServer.start();
    }

    public static class AllocHandler implements HttpHandler {
        private Charset UTF_8 = Charset.forName("UTF-8");
        private final ZKServerNodeWatcher watcher;

        public AllocHandler() {
            ZKClient.I.init();//初始化ZK
            ZKClient.I.start();//启动ZK
            watcher = ZKServerNodeWatcher.buildConnect();//监听长链接服务器节点
            watcher.beginWatch();
        }

        public void handle(HttpExchange exchange) throws IOException {
            //1.从缓存中拿取可用对长链接服务器IP
            Collection<ZKServerNode> serverNodes = watcher.getCache().values();

            //2.对serverNodes可以按某种规则排序,以便实现负载均衡,比如:随机,轮询,链接数量等

            //3.拼接返回值结构ip:port,ip:port
            StringBuilder sb = new StringBuilder();
            Iterator<ZKServerNode> it = serverNodes.iterator();
            if (it.hasNext()) {
                ZKServerNode node = it.next();
                sb.append(node.getExtranetIp()).append(':').append(node.getPort());
            }

            while (it.hasNext()) {
                ZKServerNode node = it.next();
                sb.append(',').append(node.getExtranetIp()).append(':').append(node.getPort());
            }

            byte[] data = sb.toString().getBytes(UTF_8);
            exchange.sendResponseHeaders(200, data.length);//200, content-length
            OutputStream out = exchange.getResponseBody();
            out.write(data);
            out.close();
        }
    }
}
