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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by ohun on 2016/12/5.
 *
 * @author ohun@live.cn (夜色)
 */
public final class IndexPageHandler implements HttpHandler {

    private byte[] data;

    public IndexPageHandler() {
        try (InputStream in = this.getClass().getResourceAsStream("/index.html")) {
            this.data = new byte[in.available()];
            in.read(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (data != null && data.length > 0) {
            httpExchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            httpExchange.sendResponseHeaders(200, data.length);//200, content-length
            OutputStream out = httpExchange.getResponseBody();
            out.write(data);
            out.close();
            httpExchange.close();
        } else {
            byte[] data = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            httpExchange.sendResponseHeaders(404, data.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(data);
            out.close();
            httpExchange.close();
        }
    }
}
