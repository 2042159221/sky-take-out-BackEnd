package com.sky.controller.notify;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.http.entity.ContentType;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.controller.user.AddressBookController;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.sky.service.impl.AddressBookServiceImpl;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 支付回调相关接口
 * 
 */
@RestController
@RequestMapping("/notify")
@Slf4j
@Tag(name = "支付回调相关接口")
public class PayNotifyController {

    private final GroupedOpenApi adminApi;

    private final AddressBookServiceImpl addressBookServiceImpl;

    private final AddressBookController addressBookController;
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    PayNotifyController(AddressBookController addressBookController, AddressBookServiceImpl addressBookServiceImpl, GroupedOpenApi adminApi) {
        this.addressBookController = addressBookController;
        this.addressBookServiceImpl = addressBookServiceImpl;
        this.adminApi = adminApi;
    }

    /**
     * 支付成功回调
     * @param request
     * 
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //读取数据
        String body = readData(request);
        log.info("支付成功回调：{}",body);

        //数据解密
        String plainText = decryptData(body);
        log.info("解密后的文本：{}", plainText);

        JSONObject jsonObject = JSON.parseObject(plainText);
        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号

        log.info("商户平台订单号：{}", outTradeNo);
        log.info("商户之父交易号", transactionId);

        //业务处理，修改订单状态、来电提醒
        orderService.paySuccess(outTradeNo);

        //给微信响应
        responseToWeixin(response);
    }
    /**
     * 读取数据
     * @param request
     * @return 
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if(result.length() > 0) {
                result.append("\n");
            }
            result.append(line);

            
        }
        return result.toString();
    }

    /**
     * 数据解密
     * @param body
     * @return 
     * @throws Exception
     * 
     */
    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        JSONObject resource = resultObject.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        //密文解密
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8), ciphertext);

        return plainText;
    }
    
    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object,Object> map = new HashMap<>();
        map.put("code","SUCCESS");
        map.put("message","SUCCESS");
        response.setHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSONUtils.toJSONString(map).getBytes(StandardCharsets.UTF_8));

        response.flushBuffer();
    }

}
