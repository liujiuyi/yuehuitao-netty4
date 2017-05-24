package com.yuehuitao.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.log4j.Logger;

import com.yuehuitao.netty.device.DeviceServerInitializer;
import com.yuehuitao.netty.http.HttpServerInitializer;

/**
 * 
 * 启动主函数
 *
 */
public class MainStartServer {
  private static String HOST = "vending.tttalk.org";
  private static Logger logger = Logger.getLogger(MainStartServer.class);
  private static int DEVICE_PORT = 9527;
  private static int HTTP_PORT = 3004;

  public static void main(String[] args) throws Exception {
    new MainStartServer().run();
  }

  public void run() throws Exception {
    EventLoopGroup deviceBossGroup = new NioEventLoopGroup();
    EventLoopGroup deviceWorkerGroup = new NioEventLoopGroup();

    EventLoopGroup httpBossGroup = new NioEventLoopGroup();
    EventLoopGroup httpWorkerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap deviceBoot = new ServerBootstrap();
      ServerBootstrap httpBoot = new ServerBootstrap();

      deviceBoot.group(deviceBossGroup, deviceWorkerGroup).channel(NioServerSocketChannel.class)
          .childHandler(new DeviceServerInitializer()).option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.SO_KEEPALIVE, true);

      httpBoot.group(httpBossGroup, httpWorkerGroup).channel(NioServerSocketChannel.class)
          .childHandler(new HttpServerInitializer());

      // 绑定Host,Port，开始接收进来的连接
      logger.info("Server 启动了");
      Channel deviceCh = deviceBoot.bind(HOST, DEVICE_PORT).sync().channel();

      logger.info("HttpServer 启动了");
      Channel htttpCh = httpBoot.bind(HOST, HTTP_PORT).sync().channel();

      // 等待服务器 socket 关闭 。
      deviceCh.closeFuture().sync();
      htttpCh.closeFuture().sync();
    } finally {
      deviceBossGroup.shutdownGracefully();
      deviceWorkerGroup.shutdownGracefully();

      httpBossGroup.shutdownGracefully();
      httpWorkerGroup.shutdownGracefully();
      logger.info("Server 关闭了");
    }
  }
}
