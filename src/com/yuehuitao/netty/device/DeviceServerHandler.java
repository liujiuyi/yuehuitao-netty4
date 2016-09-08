package com.yuehuitao.netty.device;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import com.yuehuitao.common.Utils;
import com.yuehuitao.netty.http.HttpServerHandler;

/**
 * 服务端 处理器
 * 
 */
public class DeviceServerHandler extends ChannelInboundHandlerAdapter {
  private static Logger logger = Logger.getLogger(DeviceServerHandler.class);

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Channel incoming = ctx.channel();
    logger.info("[SERVER] - " + incoming.remoteAddress() + " 加入\n");
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    Channel incoming = ctx.channel();
    for (String channelKey : Utils.channelMap.keySet()) {
      if (Utils.channelMap.get(channelKey) == incoming) {
        Utils.channelMap.remove(channelKey);
      }
    }
    logger.info("[SERVER] - " + incoming.remoteAddress() + " 离开\n");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    String message = "";
    // 读取客户端16进制码
    ByteBuf in = (ByteBuf) msg;
    try {
      while (in.isReadable()) {
        char c = (char) in.readByte();
        message += Utils.charToHex(c);
      }
      logger.info("收到的16进制码=" + message + "/" + Utils.str2HexStr("yuehuitao"));
      // DTU设备端包头yuehuitao
      if (message.startsWith(Utils.str2HexStr("yuehuitao"))) {
        message = message.split("20")[0];
        String key = Utils.hexStr2Str(message);
        Utils.channelMap.put(key, ctx.channel());
        // 打印所有的key
        for (String channelKey : Utils.channelMap.keySet()) {
          logger.info("已经连接的key= " + channelKey);
        }
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }
}