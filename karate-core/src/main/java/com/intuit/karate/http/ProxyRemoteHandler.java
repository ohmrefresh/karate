/*
 * The MIT License
 *
 * Copyright 2022 Karate Labs Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class ProxyRemoteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyRemoteHandler.class);

    private final ProxyContext proxyContext;
    private final ProxyClientHandler clientHandler;
    private final RequestFilter requestFilter;
    private final ResponseFilter responseFilter;
    private final Channel clientChannel;
    private final FullHttpRequest initialRequest;
    private final boolean isConnect;

    protected Channel remoteChannel;
    protected FullHttpRequest currentRequest;

    public ProxyRemoteHandler(ProxyContext proxyContext, ProxyClientHandler clientHandler,
                             FullHttpRequest initialRequest, boolean isConnect) {
        this.proxyContext = proxyContext;
        this.clientHandler = clientHandler;
        this.clientChannel = clientHandler.clientChannel;
        this.requestFilter = clientHandler.requestFilter;
        this.responseFilter = clientHandler.responseFilter;
        this.initialRequest = initialRequest;
        this.isConnect = isConnect;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.debug("<< {}", response);
        }
        ProxyResponse filtered = responseFilter == null ? null : responseFilter.apply(proxyContext, currentRequest, response);
        if (filtered == null || filtered.response == null) {
            ReferenceCountUtil.retain(response);
        } else {
            response = filtered.response;
            if (logger.isTraceEnabled()) {
                logger.debug("<<<< {}", response);
            }
        }

        // HTTP keep-alive support: only close if requested
        boolean shouldClose = shouldCloseConnection(currentRequest, response);

        if (shouldClose) {
            if (logger.isTraceEnabled()) {
                logger.trace("** closing connection (keep-alive=false)");
            }
            clientChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("** keeping connection alive");
            }
            clientChannel.writeAndFlush(response);
            // Connection stays open for next request
        }
    }

    private boolean shouldCloseConnection(FullHttpRequest request, FullHttpResponse response) {
        if (request == null) {
            return true; // No request context, close to be safe
        }

        // Check explicit Connection: close header from either side
        String reqConnection = request.headers().get(HttpHeaderNames.CONNECTION);
        String resConnection = response.headers().get(HttpHeaderNames.CONNECTION);

        if ("close".equalsIgnoreCase(reqConnection) || "close".equalsIgnoreCase(resConnection)) {
            return true;
        }

        // HTTP/1.0 defaults to close unless keep-alive is explicitly requested
        if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            return !"keep-alive".equalsIgnoreCase(reqConnection);
        }

        // HTTP/1.1 defaults to keep-alive unless close is requested
        return false;
    }

    protected void send(FullHttpRequest request) {
        currentRequest = request;

        // Validate channel is ready before sending
        if (remoteChannel == null || !remoteChannel.isActive()) {
            logger.error("** cannot send - channel not active: {}", proxyContext.hostColonPort);
            HttpUtils.flushAndClose(clientChannel);
            return;
        }

        if (!remoteChannel.isWritable()) {
            logger.warn("** channel not writable, write may be queued: {}", proxyContext.hostColonPort);
        }

        FullHttpRequest filtered;
        if (requestFilter != null) {
            ProxyResponse pr = requestFilter.apply(proxyContext, request);
            if (pr != null && pr.response != null) { // short circuit
                clientChannel.writeAndFlush(pr.response);
                return;
            }
            filtered = pr == null ? null : pr.request; // if not null, is transformed
        } else {
            filtered = null;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(">> before: {}", request);
        }
        if (filtered == null) {
            ReferenceCountUtil.retain(request);
            filtered = request;
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(">>>> after: {}", filtered);
            }
        }
        HttpUtils.fixHeadersForProxy(filtered);

        // Write with failure handling
        remoteChannel.writeAndFlush(filtered).addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("** write failed for {}: {}", proxyContext.hostColonPort, future.cause().getMessage());
                // Remove from pool on write failure
                ProxyClientHandler.removeHandler(proxyContext.hostColonPort, this);
                HttpUtils.flushAndClose(clientChannel);
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        remoteChannel = ctx.channel();

        // Only add to connection pool for non-SSL connections when channel is ready
        // SSL/CONNECT connections create new pipelines each time
        if (!isConnect && remoteChannel.isActive() && remoteChannel.isWritable()) {
            ProxyClientHandler.addToPool(proxyContext.hostColonPort, this);
            if (logger.isTraceEnabled()) {
                logger.trace("** added handler to pool: {} (total connections: {})",
                    proxyContext.hostColonPort, ProxyClientHandler.getPoolSize());
            }
        }

        if (initialRequest != null) { // only if not ssl
            send(initialRequest);
            // No need to unlock - we're now fully async!
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (logger.isTraceEnabled()) {
                logger.trace("** idle timeout, closing connection: {}", proxyContext.hostColonPort);
            }
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Clean up from shared connection pool when connection closes
        ProxyClientHandler.removeHandler(proxyContext.hostColonPort, this);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause.getMessage() == null) {
            cause.printStackTrace();
        } else {
            logger.error("closing proxy outbound connection: {}", cause.getMessage());
        }
        ctx.close();
        HttpUtils.flushAndClose(clientChannel);
    }

    @Override
    public String toString() {
        return remoteChannel + "";
    }

}
