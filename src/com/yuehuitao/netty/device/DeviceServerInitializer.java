package com.yuehuitao.netty.device;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

/**
 * 服务端初始化 接受客户端连接
 * 
 */
public class DeviceServerInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("encoder", new ByteArrayEncoder());
    pipeline.addLast("handler", new DeviceServerHandler());
    System.out.println("Client:" + ch.remoteAddress() + "连接上");
  }
}
