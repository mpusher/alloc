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
import com.mpush.api.spi.common.CacheManagerFactory;
import com.mpush.api.spi.common.ServiceDiscoveryFactory;
import com.mpush.api.srd.ServiceDiscovery;
import com.mpush.api.srd.ServiceListener;
import com.mpush.api.srd.ServiceNames;
import com.mpush.api.srd.ServiceNode;
import com.mpush.common.user.UserManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
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

    private List<ServerNode> serverNodes = Collections.emptyList();
    private ScheduledExecutorService scheduledExecutor;
    private final ServiceDiscovery discovery = ServiceDiscoveryFactory.create();
    private final UserManager userManager = new UserManager(null);

    public void start() {
        CacheManagerFactory.create().init(); //启动缓冲服务

        ServiceDiscovery discovery = ServiceDiscoveryFactory.create();// 启动发现服务
        discovery.syncStart();
        discovery.subscribe(ServiceNames.CONN_SERVER, new ConnServerNodeListener());


        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(this::refresh, 0, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        discovery.syncStop();
        CacheManagerFactory.create().destroy();
        scheduledExecutor.shutdown();
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        //3.格式组装 ip:port,ip:port
        StringBuilder sb = new StringBuilder();
        Iterator<ServerNode> it = serverNodes.iterator();
        if (it.hasNext()) {
            ServerNode node = it.next();
            sb.append(node.host).append(':').append(node.port);
        }

        while (it.hasNext()) {
            ServerNode node = it.next();
            sb.append(',').append(node.host).append(':').append(node.port);
        }

        byte[] data = sb.toString().getBytes(Constants.UTF_8);
        httpExchange.sendResponseHeaders(200, data.length);//200, content-length
        OutputStream out = httpExchange.getResponseBody();
        out.write(data);
        out.close();
        httpExchange.close();
    }

    /**
     * 从zk中获取可提供服务的机器,并以在线用户量排序
     */
    private void refresh() {
        //1.从缓存中拿取可用的长链接服务器节点
        List<ServiceNode> nodes = discovery.lookup(ServiceNames.CONN_SERVER);
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
        return userManager.getOnlineUserNum(publicIP);
    }

    private ServerNode convert(ServiceNode node) {
        String public_ip = node.getAttr(ServiceNames.ATTR_PUBLIC_IP);
        if (public_ip == null) {
            public_ip = node.getHost();
        }
        long onlineUserNum = getOnlineUserNum(public_ip);
        return new ServerNode(public_ip, node.getPort(), onlineUserNum);
    }

    private class ConnServerNodeListener implements ServiceListener {

        @Override
        public void onServiceAdded(String s, ServiceNode serviceNode) {
            refresh();
        }

        @Override
        public void onServiceUpdated(String s, ServiceNode serviceNode) {
            refresh();
        }

        @Override
        public void onServiceRemoved(String s, ServiceNode serviceNode) {
            refresh();
        }
    }

    private static class ServerNode implements Comparable<ServerNode> {
        long onlineUserNum = 0;
        String host;
        int port;

        public ServerNode(String host, int port, long onlineUserNum) {
            this.onlineUserNum = onlineUserNum;
            this.host = host;
            this.port = port;
        }

        @Override
        public int compareTo(ServerNode o) {
            return Long.compare(onlineUserNum, o.onlineUserNum);
        }
    }
}
