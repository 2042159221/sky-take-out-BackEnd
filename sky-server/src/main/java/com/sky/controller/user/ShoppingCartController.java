package com.sky.controller.user;

import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 购物车
 */
@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
@Tag(name = "C端-购物车接口")

public class ShoppingCartController {

    private final GroupedOpenApi adminApi;
    @Autowired
    private ShoppingCartService shoppingCartService;

    ShoppingCartController(GroupedOpenApi adminApi) {
        this.adminApi = adminApi;
    }

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "添加购物车")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车：{}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        
        return Result.success();
    }

    /**
     * 查看购物车
     * @return
     */
     @GetMapping("/list")
     @Operation(summary = "查看购物车")
     public Result<List<ShoppingCart>> list(){
        return Result.success(shoppingCartService.showShoppingCart());
     }

     /**
      * 清空购物车信息
      @return
      */
      @DeleteMapping("/clean")
      @Operation(summary = "清空购物车信息")
      public Result<String> clean(){
        shoppingCartService.cleanShoppingCart();
        return Result.success();
      }
}
