package com.shinemo.mpush.alloc;

import com.mpush.cache.redis.RedisKey;
import com.mpush.cache.redis.manager.RedisManager;
import com.mpush.zk.ZKClient;
import com.mpush.zk.listener.ZKServerNodeWatcher;
import com.mpush.zk.node.ZKServerNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by yxx on 2016/5/6.
 *
 * @author ohun@live.cn
 */
public class AllocServer implements HttpHandler {

    private Charset UTF_8 = Charset.forName("UTF-8");
    private final ZKServerNodeWatcher watcher;
    private List<ServerNode> serverNodes = Collections.emptyList();
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public AllocServer() {
        //ZKClient.I.start();//启动ZK
        watcher = ZKServerNodeWatcher.buildConnect();//监听长链接服务器节点
        watcher.beginWatch();
        RedisManager.I.init();
        scheduledExecutor.scheduleAtFixedRate(this::refresh, 0, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        ZKClient.I.stop();
        scheduledExecutor.shutdown();
    }

    public void handle(HttpExchange exchange) throws IOException {
        //3.格式组装 ip:port,ip:port
        StringBuilder sb = new StringBuilder();
        Iterator<ServerNode> it = serverNodes.iterator();
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

    /**
     * 从zk中获取可提供服务的机器,并以在线用户量排序
     */
    private void refresh() {
        //1.从缓存中拿取可用对长链接服务器IP
        Collection<ZKServerNode> nodes = watcher.getCache().values();
        if (nodes.size() > 0) {
            //2.对serverNodes可以按某种规则排序,以便实现负载均衡,比如:随机,轮询,链接数量等
            this.serverNodes = nodes.stream().map(this::convert).sorted(ServerNode::compareTo).collect(Collectors.toList());
        }
    }


    private long getOnlineUserNum(String publicIP) {
        String online_key = RedisKey.getUserOnlineKey(publicIP);
        Long value = RedisManager.I.zCard(online_key);
        return value == null ? 0 : value;
    }

    private ServerNode convert(ZKServerNode node) {
        ServerNode serverNode = new ServerNode();
        serverNode.setExtranetIp(node.getExtranetIp());
        serverNode.setIp(node.getIp());
        serverNode.setPort(node.getPort());
        serverNode.setOnlineUserNum(getOnlineUserNum(node.getExtranetIp()));
        return serverNode;
    }

    public static class ServerNode extends ZKServerNode implements Comparable<ServerNode> {
        long onlineUserNum = 0;

        public void setOnlineUserNum(long onlineUserNum) {
            this.onlineUserNum = onlineUserNum;
        }

        @Override
        public int compareTo(ServerNode o) {
            return Long.compare(onlineUserNum, o.onlineUserNum);
        }
    }
}
