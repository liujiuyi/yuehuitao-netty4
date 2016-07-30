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

import com.yuehuitao.common.CRC16;
import com.yuehuitao.common.Utils;

/**
 * 
 * http处理器
 *
 */

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  private HttpPostRequestDecoder decoder;

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
      System.out.println("request uri==" + uri.getPath());
      if (uri.getPath().equals("/favicon.ico")) {
        return;
      }
      // http://127.0.0.1:3001/command.action?action=01&index=12&device=yuehuitao0001
      String path = uri.getPath().substring(uri.getPath().indexOf("/") + 1);
      System.out.println("收到的链接=" + path);
      if ("command.action".equals(path)) {
        String action = ""; // 命令类型
        int index = 0;// 第几个门
        String device = "";// 机器标识
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
          device = parame.get("device").get(0);
        }

        System.out.println("action=" + action);
        System.out.println("index=" + index);
        System.out.println("device=" + device);
        if (!"".equals(action) && index > 0 && !"".equals(device)) {
          // 01 发送开门命令
          if (action.equals("01") && index > 0) {
            for (String channelKet : Utils.channelMap.keySet()) {
              // 发送给指定的客户端
              if (device.equals(channelKet)) {
                byte[] openMessage = CRC16.OPEN_DATA_ARRAY[index - 1];
                System.out.println("开始发送消息=" + openMessage);
                Utils.channelMap.get(channelKet).writeAndFlush(openMessage).addListener(new ChannelFutureListener() {
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
                System.out.println("发送的消息：" + openMessage);
              }
            }
          }
        }
      }
    }
  }

  private void writeResponse(Channel channel, HttpRequest request, ByteBuf buffer) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
    channel.writeAndFlush(response);
    System.out.println("返回结果完成");
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
