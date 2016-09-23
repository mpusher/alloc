/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     ohun@live.cn (夜色)
 */

package com.shinemo.mpush.alloc;

import com.mpush.api.Constants;
import com.mpush.cache.redis.manager.RedisManager;
import com.mpush.common.user.UserManager;
import com.mpush.zk.ZKClient;
import com.mpush.zk.ZKPath;
import com.mpush.zk.cache.ZKServerNodeCache;
import com.mpush.zk.listener.ZKServerNodeWatcher;
import com.mpush.zk.node.ZKServerNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ohun on 16/9/22.
 *
 * @author ohun@live.cn (夜色)
 */
/*package*/ final class AllocHandler implements HttpHandler {

    private ScheduledExecutorService scheduledExecutor;
    private ZKServerNodeWatcher watcher;
    private List<ServerNode> serverNodes = Collections.emptyList();

    public void start() {
        ZKClient.I.start();//启动ZK
        watcher = new ZKServerNodeWatcher(ZKPath.CONNECT_SERVER, new ConnectServerZKNodeCache());//监听长链接服务器节点
        watcher.beginWatch();
        RedisManager.I.init();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
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

        byte[] data = sb.toString().getBytes(Constants.UTF_8);
        exchange.sendResponseHeaders(200, data.length);//200, content-length
        OutputStream out = exchange.getResponseBody();
        out.write(data);
        out.close();
    }

    /**
     * 从zk中获取可提供服务的机器,并以在线用户量排序
     */
    private void refresh() {
        //1.从缓存中拿取可用的长链接服务器节点
        Collection<ZKServerNode> nodes = watcher.getCache().values();
        if (nodes.size() > 0) {
            //2.对serverNodes可以按某种规则排序,以便实现负载均衡,比如:随机,轮询,链接数量等
            this.serverNodes = nodes
                    .stream()
                    .map(this::convert)
                    .sorted(ServerNode::compareTo)
                    .collect(Collectors.toList());
        }
    }

    private long getOnlineUserNum(String publicIP) {
        return UserManager.I.getOnlineUserNum(publicIP);
    }

    private ServerNode convert(ZKServerNode node) {
        ServerNode serverNode = new ServerNode();
        serverNode.setExtranetIp(node.getExtranetIp());
        serverNode.setIp(node.getIp());
        serverNode.setPort(node.getPort());
        serverNode.setOnlineUserNum(getOnlineUserNum(node.getExtranetIp()));
        return serverNode;
    }

    private class ConnectServerZKNodeCache extends ZKServerNodeCache {

        @Override
        public void put(String fullPath, ZKServerNode node) {
            super.put(fullPath, node);
            refresh();
        }

        @Override
        public ZKServerNode remove(String fullPath) {
            ZKServerNode node = super.remove(fullPath);
            refresh();
            return node;
        }

        @Override
        public void clear() {
            super.clear();
            refresh();
        }
    }

    private static class ServerNode extends ZKServerNode implements Comparable<ServerNode> {
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
