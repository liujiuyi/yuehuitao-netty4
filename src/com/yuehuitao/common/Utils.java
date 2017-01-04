package com.yuehuitao.common;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

public class Utils {
  // 所有客户端列表
  public static Map<String, Channel> channelMap = new HashMap<String, Channel>();

  public static void main(String[] args) {
    String str_index = String.valueOf(4);
    int strLen = str_index.length();
    if (strLen < 3) {
      while (strLen < 3) {
        StringBuffer sb = new StringBuffer();
        sb.append("0").append(str_index);// 左补0
        str_index = sb.toString();
        strLen = str_index.length();
      }
    }
    System.out.println(str_index);
  }
}
