package com.sky.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import com.sky.result.Result;
import com.sky.result.PageResult;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 订单管理
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Tag(name = "订单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @PostMapping("/conditionSearch")
    @Operation(summary = "订单搜索")
    public Result<PageResult> conditionSearch(@RequestBody OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单搜索：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }
    
    /**
     * 订单条件查询 - GET请求专用接口
     */
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索(GET)")
    public Result<PageResult> conditionSearchGet(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "number", required = false) String orderNumber,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "beginTime", required = false) String beginTimeStr,
            @RequestParam(value = "endTime", required = false) String endTimeStr) {
        
        // 创建查询DTO
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setPage(page);
        dto.setPageSize(pageSize);
        
        // 处理订单号参数
        if (orderNumber != null && !orderNumber.isEmpty()) {
            dto.setNumber(orderNumber);
            log.info("设置订单号查询参数：{}", orderNumber);
        }
        
        // 设置其他基本参数
        dto.setStatus(status);
        dto.setPhone(phone);
        
        // 处理日期参数
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            if (beginTimeStr != null && !beginTimeStr.isEmpty()) {
                // 解码URL编码并解析日期
                String decodedBeginTime = URLDecoder.decode(beginTimeStr, StandardCharsets.UTF_8.name());
                // 如果只有日期部分，添加时间部分
                if (decodedBeginTime.length() <= 10) {
                    decodedBeginTime += " 00:00:00";
                }
                LocalDateTime beginTime = LocalDateTime.parse(decodedBeginTime, formatter);
                dto.setBeginTime(beginTime);
            }
            
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                // 解码URL编码并解析日期
                String decodedEndTime = URLDecoder.decode(endTimeStr, StandardCharsets.UTF_8.name());
                // 如果只有日期部分，添加结束时间部分
                if (decodedEndTime.length() <= 10) {
                    decodedEndTime += " 23:59:59";
                }
                LocalDateTime endTime = LocalDateTime.parse(decodedEndTime, formatter);
                dto.setEndTime(endTime);
            }
        } catch (Exception e) {
            log.error("日期参数解析错误", e);
            return Result.error("日期格式不正确");
        }
        
        log.info("订单GET搜索参数：{}", dto);
        PageResult pageResult = orderService.conditionSearch(dto);
        return Result.success(pageResult);
    }
    
    /**
     * 各个状态的订单数量的统计
     * @return
     */
    @GetMapping("/statusCount")
    @Operation(summary = "各个状态的订单数量的统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 订单详情
     * @param id 
     * @return
     */
    @GetMapping("/details/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * 
     * @return
     */
    @PutMapping("/confirm")
    @Operation(summary = "接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @return
     * 
     */
    @PutMapping("/rejection")
    @Operation(summary = "拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO)throws Exception{
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @return
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO)throws Exception{
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @return
     */
    @PutMapping("/delivery/{id}")
    @Operation(summary = "派送订单")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @return
     */
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);

        return Result.success();
    }

}
