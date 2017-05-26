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

import com.mpush.api.service.BaseService;
import com.mpush.api.service.Listener;
import com.mpush.api.service.ServiceException;
import com.mpush.api.srd.ServiceNames;
import com.mpush.tools.config.CC;
import com.mpush.tools.log.Logs;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Created by yxx on 2016/5/6.
 *
 * @author ohun@live.cn
 */
public final class AllocServer extends BaseService {

    private HttpServer httpServer;
    private AllocHandler allocHandler;
    private AllocHandler wsAllocHandler;//ws负载均衡
    private PushHandler pushHandler;

    @Override
    public void init() {
        try {
            int port = CC.mp.net.cfg.getInt("alloc-server-port");
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            this.allocHandler = new AllocHandler(ServiceNames.CONN_SERVER);
            this.wsAllocHandler = new AllocHandler(ServiceNames.WS_SERVER);
            this.pushHandler = new PushHandler();
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        httpServer.setExecutor(Executors.newCachedThreadPool());//设置线程池，由于是纯内存操作，不需要队列
        httpServer.createContext("/", allocHandler);//查询mpush机器
        httpServer.createContext("/ws", wsAllocHandler);//查询mpush机器(ws)
        httpServer.createContext("/push", pushHandler);//模拟发送push
        httpServer.createContext("/index.html", new IndexPageHandler());//查询mpush机器
    }

    @Override
    protected void doStart(Listener listener) throws Throwable {
        pushHandler.start();
        allocHandler.start();
        wsAllocHandler.start();
        httpServer.start();
        Logs.Console.info("===================================================================");
        Logs.Console.info("====================ALLOC SERVER START SUCCESS=====================");
        Logs.Console.info("===================================================================");
    }

    @Override
    protected void doStop(Listener listener) throws Throwable {
        httpServer.stop(0);//1 min
        pushHandler.stop();
        allocHandler.stop();
        wsAllocHandler.stop();
        Logs.Console.info("===================================================================");
        Logs.Console.info("====================ALLOC SERVER STOPPED SUCCESS=====================");
        Logs.Console.info("===================================================================");
    }
}
