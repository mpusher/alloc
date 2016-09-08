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

import com.mpush.tools.log.Logs;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by ohun on 16/9/7.
 *
 * @author ohun@live.cn (夜色)
 */
public class Main {
    public static void main(String[] args) throws IOException {//正式环境可以用tomcat
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(9999), 0);
        httpServer.createContext("/push", new PushHandler());//模拟发送push
        httpServer.createContext("/", new AllocServer());//模拟Alloc
        httpServer.start();
        Logs.Console.info("===================================================================");
        Logs.Console.info("====================ALLOC SERVER START SUCCESS=====================");
        Logs.Console.info("===================================================================");
    }
}
