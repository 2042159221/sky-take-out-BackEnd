package com.sky.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    @Operation(summary = "获取店铺营业状态")
    public Result<Integer> getStatus(){
        String value = redisTemplate.opsForValue().get(KEY);
        Integer status = 0;
        
        if (value != null) {
            status = Integer.parseInt(value);
        }
        
        log.info("获取到店铺的营业状态为：{}",status == 1 ? "营业中":"打烊中");
        return Result.success(status);
    }
}
