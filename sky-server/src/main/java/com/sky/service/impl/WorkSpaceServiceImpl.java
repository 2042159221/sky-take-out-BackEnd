package com.sky.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {



    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;




    /**
     * 根据时间段统计营业额数据
     * @param begin 
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin ,LocalDateTime end){
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率: 有效订单/总订单数
         * 平均客单价：营业额/ 有效订单数
         * 新增用户 ：当日新增用户的书量
         * 
         */
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("begin",begin);
            map.put("end",end);
            
            //查询总订单数
            Integer totalOrderCount = orderMapper.countByMap(map);
            totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;
            
            //查询有效订单数
            map.put("status",Orders.COMPLETED);

            //营业额
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null? 0.0 : turnover;

            //有效订单数
            Integer validOrderCount = orderMapper.countByMap(map);
            validOrderCount = validOrderCount == null ? 0 : validOrderCount;

            Double unitPrice = 0.0;

            Double orderCompletionRate = 0.0;
            if(totalOrderCount != 0 && validOrderCount != 0) {
                //订单完成率
                orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
                //平均客单价
                unitPrice = turnover.doubleValue() / validOrderCount.doubleValue();
            }

            //新增用户数
            Integer newUsers = userMapper.countByMap(map);
            newUsers = newUsers == null ? 0 : newUsers;

            log.info("营业额：{}，有效订单数：{}，订单完成率：{}，平均客单价：{}，新增用户数：{}", 
                    turnover, validOrderCount, orderCompletionRate, unitPrice, newUsers);

            return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
        } catch (Exception e) {
            log.error("获取营业数据异常", e);
            // 返回默认值
            return BusinessDataVO.builder()
                .turnover(0.0)
                .validOrderCount(0)
                .orderCompletionRate(0.0)
                .unitPrice(0.0)
                .newUsers(0)
                .build();
        }
    }

    /**
     * 查询订单管理数据
     * @return
     */
    public OrderOverViewVO getOrderOverView(){
        Map<String,Object> map = new HashMap<>();
        map.put("begin",LocalDateTime.now().with(LocalTime.MIN));
        map.put("status",Orders.TO_BE_CONFIRMED);

        //待接单
        Integer waitingOrders = orderMapper.countByMap(map);
        map.put("status",Orders.CONFIRMED);
        //待派送
        Integer deliveringOrders = orderMapper.countByMap(map);
        map.put("status",Orders.COMPLETED);
        //已完成
        Integer completedOrders = orderMapper.countByMap(map);
        map.put("status", Orders.CANCELLED);
        //已取消
        Integer cancelledOrders = orderMapper.countByMap(map);

        //全部接单
        map.put("status",null);
        Integer allOrders =orderMapper.countByMap(map);


        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveringOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }
    /**
     * 查询菜品总览
     * @return
     */
    public DishOverViewVO getDishOverView(){
        Map<String,Object> map = new HashMap<>();
        map.put("status",StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status",StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();


    }
     /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        Map<String,Object> map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }           
                                    

}
