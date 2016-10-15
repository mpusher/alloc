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

import java.io.IOException;

/**
 * Created by ohun on 16/9/7.
 *
 * @author ohun@live.cn (夜色)
 */
public final class Main {

    public static void main(String[] args) {
        Logs.init();
        Logs.Console.info("launch alloc server...");
        AllocServer server = new AllocServer();
        server.start();
        addHook(server);
    }

    private static void addHook(AllocServer server) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        Logs.Console.error("alloc server stop ex", e);
                    }
                    Logs.Console.info("jvm exit, all service stopped...");

                }, "mpush-shutdown-hook-thread")
        );
    }
}
