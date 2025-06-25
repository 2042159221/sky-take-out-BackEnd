package com.sky.controller.admin;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.result.Result;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



/**
 * 工作台
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
@Tag( name = "工作台相关接口")
public class WorkSpaceController {


    @Autowired
    private WorkSpaceService workSpaceService;


    /**
     * 工作台今日数据查询
     * @return
     */
    @GetMapping("/businessData")
    @Operation(summary = "工作台今日数据查询")
    public Result<BusinessDataVO> businessData() {
        try {
            //获取当天的开始时间
            LocalDateTime begin = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            //获取当天的结束时间
            LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            log.info("查询营业数据，开始时间：{}，结束时间：{}", begin, end);
            
            //获取当天的数据
            BusinessDataVO businessDataVO = workSpaceService.getBusinessData(begin, end);
            
            // 确保返回值不为null
            if(businessDataVO == null) {
                businessDataVO = BusinessDataVO.builder()
                    .turnover(0.0)
                    .validOrderCount(0)
                    .orderCompletionRate(0.0)
                    .unitPrice(0.0)
                    .newUsers(0)
                    .build();
            }
            
            return Result.success(businessDataVO);
        } catch (Exception e) {
            log.error("查询营业数据异常", e);
            // 返回默认值，避免前端显示NaN
            BusinessDataVO defaultData = BusinessDataVO.builder()
                .turnover(0.0)
                .validOrderCount(0)
                .orderCompletionRate(0.0)
                .unitPrice(0.0)
                .newUsers(0)
                .build();
            return Result.success(defaultData);
        }
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @GetMapping("/overviewOrders")
    @Operation(summary = "查询订单管理数据")
    public Result<OrderOverViewVO> orderOverView(){
        return Result.success(workSpaceService.getOrderOverView());
    }

    /**
     * 查询菜品总览
     * @param return
     */
    @GetMapping("/overviewDishes")
    @Operation(summary = "查询菜品总览")
    public Result<DishOverViewVO> dishOverView(){
        return Result.success(workSpaceService.getDishOverView());
    }

    @GetMapping("/overviewSetmeals")
    @Operation(summary = "查询套餐总览")
    public Result<SetmealOverViewVO> setmealOverView(){
        return Result.success(workSpaceService.getSetmealOverView());
    }


}
