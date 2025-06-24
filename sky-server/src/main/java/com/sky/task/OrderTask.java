package com.sky.task;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sky.controller.user.AddressBookController;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.impl.AddressBookServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义定时任务，实现订单状态定时处理
 * 
 */
@Component
@Slf4j
public class OrderTask {

    private final AddressBookServiceImpl addressBookServiceImpl;

    private final AddressBookController addressBookController;
    @Autowired
    private OrderMapper orderMapper;

    OrderTask(AddressBookController addressBookController, AddressBookServiceImpl addressBookServiceImpl) {
        this.addressBookController = addressBookController;
        this.addressBookServiceImpl = addressBookServiceImpl;
    }

    /**
     * 处理支付超时订单
     * 
     */
    @Scheduled(cron = "0 * * * * ?")
    public void executeTask(){
        log.info("定时任务开启：{}",new Date());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //select * from orders where status =1 and order_time< 当前时间-15分钟
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT,time);
        if(ordersList != null && ordersList.size() > 0 ) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("支付超时,自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            });
        }
    }
    /**
     * 处理派送中状态的订单
     */
    @Scheduled(cron ="0  0  1 * * ?")
    public void  processDeliveryOrder(){
        log.info("处理派送中订单：{}",new Date());
        //select * from orders where status = 4 and  order_time =< 当前时间-1小时
        LocalDateTime time = LocalDateTime.now().plusHours(-1);
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS,time);
        if(ordersList != null && ordersList.size() > 0 ) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }

}
