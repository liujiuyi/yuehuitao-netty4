package com.yuehuitao.netty.device;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import com.yuehuitao.common.Utils;

/**
 * 服务端 处理器
 * 
 */
public class DeviceServerHandler extends ChannelInboundHandlerAdapter {
  private static Logger logger = Logger.getLogger(DeviceServerHandler.class);
  // 心跳丢失计数器
  private int counter;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Channel incoming = ctx.channel();
    logger.info("[SERVER] - " + incoming.remoteAddress() + " 加入\n");
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    Channel incoming = ctx.channel();
    String removeKey = "";
    for (String channelKey : Utils.channelMap.keySet()) {
      if (Utils.channelMap.get(channelKey) == incoming) {
        removeKey = channelKey;
        break;
      }
    }
    Utils.channelMap.remove(removeKey);
    logger.info("[SERVER] - " + incoming.remoteAddress() + " 离开\n");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 将心跳丢失计数器置为0
    counter = 0;

    String message = "";
    // 读取客户端连接消息,消息格式：{START,ID:A01611BKK000001,END}
    ByteBuf in = (ByteBuf) msg;
    try {
      while (in.isReadable()) {
        char c = (char) in.readByte();
        message += c;
      }
      logger.info("收到心跳包=" + message);
      // 检查字符串是否符合通讯格式
      if (message.startsWith("{START") && message.endsWith("END}") && message.split(",").length == 3) {
        String key = message.split(",")[1];
        if (key.startsWith("ID")) {
          Utils.channelMap.put(key, ctx.channel());
          // 打印所有的key
          for (String channelKey : Utils.channelMap.keySet()) {
            logger.info("已经连接的key= " + channelKey);
          }
          ctx.channel().writeAndFlush("{START,ID:OK,END}".getBytes());
        }
      } else {
        logger.info(message.startsWith("{START"));
        logger.info(message.endsWith("END}"));
        logger.info(message.split(",").length);
        ctx.channel().writeAndFlush("{START,ID:FAIL,END}".getBytes());
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      // 空闲6s之后触发 (心跳包丢失)
      if (counter >= 3) {
        // 连续丢失3个心跳包 (断开连接)
        ctx.channel().close().sync();
        logger.info("已与Client断开连接");
      } else {
        counter++;
        logger.info("丢失了第 " + counter + " 个心跳包");
      }
    }
  }
}