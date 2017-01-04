package com.yuehuitao.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.yuehuitao.common.Utils;

/**
 * 
 * http处理器
 *
 */

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  private HttpPostRequestDecoder decoder;
  private static Logger logger = Logger.getLogger(HttpServerHandler.class);

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (decoder != null) {
      decoder.cleanFiles();
    }
  }

  public void messageReceived(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    final Channel channel = ctx.channel();
    if (msg instanceof HttpRequest) {
      final HttpRequest request = (HttpRequest) msg;
      URI uri = new URI(request.getUri());
      logger.info("request uri==" + uri.getPath());
      if (uri.getPath().equals("/favicon.ico")) {
        return;
      }
      // http://211.149.218.190:3004/command.action?action=01&index=12&device=A01611BKK000001
      String path = uri.getPath().substring(uri.getPath().indexOf("/") + 1);
      logger.info("收到的链接=" + path);
      if ("command.action".equals(path)) {
        String action = ""; // 命令类型
        int index = 0;// 第几个门
        String device = "";// 机器标识
        String order_id = "";// 订单id
        // 解析get请求参数
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> parame = decoder.parameters();
        if (parame.containsKey("action")) {
          action = parame.get("action").get(0);
        }
        if (parame.containsKey("index")) {
          index = Integer.valueOf(parame.get("index").get(0));
        }
        if (parame.containsKey("device")) {
          device = "ID:" + parame.get("device").get(0);
        }
        if (parame.containsKey("order_id")) {
          order_id = parame.get("order_id").get(0);
        }

        logger.info("action=" + action);
        logger.info("index=" + index);
        logger.info("device=" + device);
        logger.info("order_id=" + order_id);

        if (!"".equals(action) && !"".equals(device)) {
          // 01 发送开门命令
          if (action.equals("01") && index > 0) {
            if (Utils.channelMap.containsKey(device)) {
              for (String channelKet : Utils.channelMap.keySet()) {
                // 发送给指定的客户端
                if (device.equals(channelKet)) {
                  // 左侧补0，补足三位
                  String str_index = String.valueOf(index);
                  int strLen = str_index.length();
                  if (strLen < 3) {
                    while (strLen < 3) {
                      StringBuffer sb = new StringBuffer();
                      sb.append("0").append(str_index);// 左补0
                      str_index = sb.toString();
                      strLen = str_index.length();
                    }
                  }

                  String openMessage = "{START,OPEN:" + str_index + ",END}";// 格式{START,OPEN:023,END}
                  logger.info("开始发送消息=" + openMessage);
                  Utils.channelMap.get(channelKet).writeAndFlush(openMessage.getBytes())
                      .addListener(new ChannelFutureListener() {
                        // 监听发送的结果
                        public void operationComplete(ChannelFuture future) throws Exception {
                          if (future.isSuccess()) {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送消息开门成功\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          } else {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送消息开门失败\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          }
                        }
                      });
                  logger.info("发送的消息：" + openMessage);
                }
              }
            } else {
              StringBuilder buf = new StringBuilder();
              buf.append("没有在线的设备\r\n");
              ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
              writeResponse(channel, request, buffer);
            }
          } else if (action.equals("02")) {
            // 02 检查设备是否在线
            StringBuilder buf = new StringBuilder();
            if (Utils.channelMap.containsKey(device)) {
              buf.append("true");
            } else {
              buf.append("false");
            }
            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
            writeResponse(channel, request, buffer);
          } else if (action.equals("03")) {
            // 03 LED开控制
            if (Utils.channelMap.containsKey(device)) {
              for (String channelKet : Utils.channelMap.keySet()) {
                // 发送给指定的客户端
                if (device.equals(channelKet)) {
                  String onMessage = "{START,LED:ON,END}";
                  logger.info("开始发送消息");
                  Utils.channelMap.get(channelKet).writeAndFlush(onMessage.getBytes())
                      .addListener(new ChannelFutureListener() {
                        // 监听发送的结果
                        public void operationComplete(ChannelFuture future) throws Exception {
                          if (future.isSuccess()) {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送LED命令成功\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          } else {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送LED命令失败\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          }
                        }
                      });
                  logger.info("发送的消息：" + onMessage);
                }
              }
            } else {
              StringBuilder buf = new StringBuilder();
              buf.append("没有在线的设备\r\n");
              ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
              writeResponse(channel, request, buffer);
            }
          } else if (action.equals("04")) {
            // 04 LED关控制
            if (Utils.channelMap.containsKey(device)) {
              for (String channelKet : Utils.channelMap.keySet()) {
                // 发送给指定的客户端
                if (device.equals(channelKet)) {
                  String offMessage = "{START,LED:OFF,END}";
                  logger.info("开始发送消息");
                  Utils.channelMap.get(channelKet).writeAndFlush(offMessage.getBytes())
                      .addListener(new ChannelFutureListener() {
                        // 监听发送的结果
                        public void operationComplete(ChannelFuture future) throws Exception {
                          if (future.isSuccess()) {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送LED命令成功\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          } else {
                            StringBuilder buf = new StringBuilder();
                            buf.append("发送LED命令失败\r\n");
                            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
                            writeResponse(channel, request, buffer);
                          }
                        }
                      });
                  logger.info("发送的消息：" + offMessage);
                }
              }
            } else {
              StringBuilder buf = new StringBuilder();
              buf.append("没有在线的设备\r\n");
              ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
              writeResponse(channel, request, buffer);
            }
          } else {
            StringBuilder buf = new StringBuilder();
            buf.append("错误的命令标识\r\n");
            ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
            writeResponse(channel, request, buffer);
          }
        } else {
          StringBuilder buf = new StringBuilder();
          buf.append("错误的参数\r\n");
          ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
          writeResponse(channel, request, buffer);
        }
      }
    }
  }

  private void writeResponse(Channel channel, HttpRequest request, ByteBuf buffer) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
    channel.writeAndFlush(response);
    logger.info("返回结果完成");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.channel().close();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    messageReceived(ctx, msg);
  }
}
