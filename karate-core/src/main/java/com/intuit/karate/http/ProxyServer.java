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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class ProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final Channel channel;
    private final int port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public int getPort() {
        return port;
    }

    public void waitSync() {
        try {
            channel.closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        logger.info("stop: shutting down");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        logger.info("stop: shutdown complete");
    }

    public ProxyServer(int requestedPort, RequestFilter requestFilter, ResponseFilter responseFilter) {
        // Use Epoll on Linux for better performance, fallback to NIO on other platforms
        boolean useEpoll = Epoll.isAvailable();
        if (useEpoll) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(8);
            logger.info("using Epoll event loop for optimal performance");
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(8);
            logger.info("using NIO event loop");
            if (logger.isDebugEnabled() && Epoll.unavailabilityCause() != null) {
                logger.debug("Epoll not available: {}", Epoll.unavailabilityCause().getMessage());
            }
        }
        try {
            Class<? extends ServerChannel> channelClass = useEpoll
                    ? EpollServerSocketChannel.class
                    : NioServerSocketChannel.class;
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(channelClass)
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel c) {
                            ChannelPipeline p = c.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(1048576));
                            p.addLast(new ProxyClientHandler(requestFilter, responseFilter, workerGroup));
                        }
                    });
            channel = b.bind(requestedPort).sync().channel();
            InetSocketAddress isa = (InetSocketAddress) channel.localAddress();
            String host = "127.0.0.1"; //isa.getHostString();
            port = isa.getPort();
            logger.info("proxy server started - http://{}:{}", host, port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
