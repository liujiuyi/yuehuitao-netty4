package com.yuehuitao.common;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

public class Utils {
  // 所有客户端列表
  public static Map<String, Channel> channelMap = new HashMap<String, Channel>();
  // md5加密校验位
  private static String MD5_CHECK = "yuehuitao";

  public static void main(String[] args) {
    System.out.println(getMD5("01,12,1389999999"));
    for (String channelKey : Utils.channelMap.keySet()) {
      System.out.println("已经连接的key= " + channelKey);
    }
  }

  // 转换字节到十六进制编码
  public static String charToHex(char c) {
    String hex = Integer.toHexString(c & 0xFF);
    if (hex.length() == 1) {
      hex = '0' + hex;
    }
    return hex;
  }

  // 转换字符串到十六进制编码
  public static String str2HexStr(String str) {
    char[] chars = "0123456789abcdef".toCharArray();
    StringBuilder sb = new StringBuilder("");
    byte[] bs = str.getBytes();
    int bit;
    for (int i = 0; i < bs.length; i++) {
      bit = (bs[i] & 0x0f0) >> 4;
      sb.append(chars[bit]);
      bit = bs[i] & 0x0f;
      sb.append(chars[bit]);
    }
    return sb.toString();
  }

  // 转换十六进制编码到字符串
  public static String hexStr2Str(String s) {
    if ("0x".equals(s.substring(0, 2))) {
      s = s.substring(2);
    }
    byte[] baKeyword = new byte[s.length() / 2];
    for (int i = 0; i < baKeyword.length; i++) {
      try {
        baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      s = new String(baKeyword, "utf-8");
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return s;
  }

  public static String getMD5(String content) {
    String s = null;
    char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
      md.update(content.getBytes());
      byte tmp[] = md.digest();
      char str[] = new char[16 * 2];
      int k = 0;
      for (int i = 0; i < 16; i++) {
        byte byte0 = tmp[i];
        str[k++] = hexDigits[byte0 >>> 4 & 0xf];
        str[k++] = hexDigits[byte0 & 0xf];
      }
      s = new String(str);

    } catch (Exception e) {
    }
    return s;
  }

  // 验证命令的正确性
  public static boolean checkCommand(String message, String md5) {
    System.out.println("得出的串=" + getMD5(message + MD5_CHECK));
    System.out.println("验证的串=" + md5);
    if (getMD5(message + MD5_CHECK).equals(md5)) {
      return true;
    }
    return false;
  }
}
