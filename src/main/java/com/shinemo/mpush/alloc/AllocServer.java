package com.shinemo.mpush.alloc;

import com.mpush.api.service.BaseService;
import com.mpush.api.service.Listener;
import com.mpush.api.service.ServiceException;
import com.mpush.cache.redis.RedisKey;
import com.mpush.cache.redis.manager.RedisManager;
import com.mpush.tools.config.CC;
import com.mpush.tools.log.Logs;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by yxx on 2016/5/6.
 *
 * @author ohun@live.cn
 */
public final class AllocServer extends BaseService {

    private HttpServer httpServer;

    @Override
    public void init() {
        try {
            int port = CC.mp.net.cfg.getInt("alloc-server-port");
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/push", new PushHandler());//模拟发送push
        httpServer.createContext("/", new AllocHandler());//模拟Alloc
    }

    @Override
    protected void doStart(Listener listener) throws Throwable {
        httpServer.start();
        Logs.Console.info("===================================================================");
        Logs.Console.info("====================ALLOC SERVER START SUCCESS=====================");
        Logs.Console.info("===================================================================");
    }

    @Override
    protected void doStop(Listener listener) throws Throwable {
        httpServer.stop(60);//1 min
    }
}
