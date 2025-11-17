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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class ProxyClientHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyClientHandler.class);

    // Shared connection pool across all proxy client handlers
    private static final Map<String, ProxyRemoteHandler> REMOTE_HANDLERS = new ConcurrentHashMap<>();

    // Cached Bootstrap per EventLoopGroup for efficiency
    private static final Map<EventLoopGroup, Bootstrap> BOOTSTRAP_CACHE = new ConcurrentHashMap<>();

    protected final RequestFilter requestFilter;
    protected final ResponseFilter responseFilter;
    protected final EventLoopGroup eventLoopGroup;
    protected final ProxyConfig config;
    private final Bootstrap bootstrap;

    private ProxyRemoteHandler remoteHandler;
    protected Channel clientChannel;

    /**
     * Creates a new proxy client handler.
     *
     * @param requestFilter optional filter to intercept/modify requests
     * @param responseFilter optional filter to intercept/modify responses
     * @param eventLoopGroup shared event loop group for async I/O
     * @param config proxy configuration (timeouts, limits, etc.)
     */
    public ProxyClientHandler(RequestFilter requestFilter, ResponseFilter responseFilter,
                             EventLoopGroup eventLoopGroup, ProxyConfig config) {
        this.requestFilter = requestFilter;
        this.responseFilter = responseFilter;
        this.eventLoopGroup = eventLoopGroup;
        this.config = config;
        // Get or create cached Bootstrap for this EventLoopGroup
        this.bootstrap = BOOTSTRAP_CACHE.computeIfAbsent(eventLoopGroup, this::createBootstrap);
    }

    /**
     * Creates a Bootstrap configured for outbound connections.
     * Bootstrap is cached per EventLoopGroup to avoid recreation overhead.
     *
     * @param group the EventLoopGroup to use
     * @return configured Bootstrap instance
     */
    private Bootstrap createBootstrap(EventLoopGroup group) {
        // Determine channel class based on EventLoopGroup type
        Class<? extends SocketChannel> channelClass = group instanceof EpollEventLoopGroup
                ? EpollSocketChannel.class
                : NioSocketChannel.class;

        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(channelClass)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMillis())
         .option(ChannelOption.SO_KEEPALIVE, true)
         .option(ChannelOption.TCP_NODELAY, true);

        if (logger.isDebugEnabled()) {
            logger.debug("created cached Bootstrap for {} with channel type {}",
                group.getClass().getSimpleName(), channelClass.getSimpleName());
        }

        return b;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clientChannel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        boolean isConnect = HttpMethod.CONNECT.equals(request.method());
        ProxyContext pc = new ProxyContext(request, isConnect);
        // if ssl CONNECT, always create new remote pipeline
        if (remoteHandler == null && !isConnect) {
            remoteHandler = REMOTE_HANDLERS.get(pc.hostColonPort);
            // Validate connection is still active
            if (remoteHandler != null && (remoteHandler.remoteChannel == null || !remoteHandler.remoteChannel.isActive())) {
                if (logger.isTraceEnabled()) {
                    logger.trace("** removing stale connection: {}", pc.hostColonPort);
                }
                REMOTE_HANDLERS.remove(pc.hostColonPort);
                remoteHandler = null;
            }
        }
        if (remoteHandler != null) {
            remoteHandler.send(request);
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(">> init: {} - {}", pc, request);
        }
        // Use cached Bootstrap with configured timeouts
        Bootstrap b = bootstrap.clone();
        b.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel remoteChannel) throws Exception {
                ChannelPipeline p = remoteChannel.pipeline();
                if (isConnect) {
                    SSLContext sslContext = HttpUtils.getSslContext(null);
                    SSLEngine remoteSslEngine = sslContext.createSSLEngine(pc.host, pc.port);
                    remoteSslEngine.setUseClientMode(true);
                    remoteSslEngine.setNeedClientAuth(false);
                    SslHandler remoteSslHandler = new SslHandler(remoteSslEngine);
                    p.addLast(remoteSslHandler);
                    remoteSslHandler.handshakeFuture().addListener(rhf -> {
                        if (logger.isTraceEnabled()) {
                            logger.trace("** ssl: server handshake done: {}", remoteChannel);
                        }
                        SSLEngine clientSslEngine = sslContext.createSSLEngine();
                        clientSslEngine.setUseClientMode(false);
                        clientSslEngine.setNeedClientAuth(false);
                        SslHandler clientSslHandler = new SslHandler(clientSslEngine);
                        HttpResponse response = HttpUtils.connectionEstablished();
                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                        clientChannel.eventLoop().execute(() -> {
                            clientChannel.writeAndFlush(response);
                            clientChannel.pipeline().addFirst(clientSslHandler);
                        });
                        if (logger.isTraceEnabled()) {
                            clientSslHandler.handshakeFuture().addListener(chf -> {
                                logger.trace("** ssl: client handshake done: {}", clientChannel);
                            });
                        }
                        // SSL handshake is now fully async - no blocking!
                    });
                }
                // Add timeout handlers for robustness
                p.addLast("readTimeout", new ReadTimeoutHandler(config.getReadTimeoutSeconds(), TimeUnit.SECONDS));
                p.addLast("writeTimeout", new WriteTimeoutHandler(config.getWriteTimeoutSeconds(), TimeUnit.SECONDS));
                p.addLast("idleStateHandler", new IdleStateHandler(config.getIdleTimeoutSeconds(), config.getIdleTimeoutSeconds(), 0, TimeUnit.SECONDS));
                // HTTP codec and handlers
                p.addLast(new HttpClientCodec());
                p.addLast(new HttpContentDecompressor());
                p.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                // Create and register proxy handler
                remoteHandler = new ProxyRemoteHandler(pc, ProxyClientHandler.this, isConnect ? null : request);
                REMOTE_HANDLERS.put(pc.hostColonPort, remoteHandler);
                p.addLast(remoteHandler);
                if (logger.isTraceEnabled()) {
                    logger.trace("updated remote handlers: {}", REMOTE_HANDLERS);
                }
            }
        });
        ChannelFuture cf = b.connect(pc.host, pc.port);
        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("** ready: {} - {}", pc, cf.channel());
                }
            } else {
                HttpUtils.flushAndClose(clientChannel);
            }
        });
        // Connection establishment is now fully async - no blocking!
        // ProxyRemoteHandler.channelActive() will send the request when ready
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause.getMessage() == null) {
            cause.printStackTrace();
        } else {
            logger.error("closing proxy inbound connection: {}", cause.getMessage());
        }
        ctx.close();
        if (remoteHandler != null && remoteHandler.remoteChannel != null) {
            HttpUtils.flushAndClose(remoteHandler.remoteChannel);
        }
    }

    // Remove handler from shared pool when connection closes
    protected static void removeHandler(String hostColonPort, ProxyRemoteHandler handler) {
        REMOTE_HANDLERS.remove(hostColonPort, handler);
        if (logger.isTraceEnabled()) {
            logger.trace("** removed handler from pool: {}", hostColonPort);
        }
    }

}
