package com.example.charging.util;

import java.util.Map;

public class WechatPayUtil {

    // 模拟微信支付密钥，实际应存放在安全配置中
    public static final String WECHAT_PAY_KEY = "your_wechat_pay_key";

    /**
     * 模拟生成签名（实际需要按照微信支付文档实现）
     */
    public static String generateSign(Map<String, String> params, String key) {
        // 此处返回伪签名，实际需要根据参数排序、拼接和MD5/SM3加密生成
        return "MOCK_SIGN";
    }

    /**
     * 模拟验证签名
     */
    public static boolean validateSign(Map<String, String> params, String key) {
        // 此处仅返回true，实际需要验证签名
        return true;
    }
}
