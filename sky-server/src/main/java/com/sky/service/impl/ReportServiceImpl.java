package com.sky.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import com.sky.controller.user.AddressBookController;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService{

    private final AddressBookServiceImpl addressBookServiceImpl;

    private final AddressBookController addressBookController;

    @Autowired
    private OrderMapper orderMapper;

    ReportServiceImpl(AddressBookController addressBookController, AddressBookServiceImpl addressBookServiceImpl) {
        this.addressBookController = addressBookController;
        this.addressBookServiceImpl = addressBookServiceImpl;
    }

    /**
     * 根据时间区间统计营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnover(LocalDate begin , LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin =begin.plusDays(1);//日期计算，获得指定日期后一天的日期
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("status",Orders.COMPLETED);
            map.put("begin",beginTime);
            map.put("end",endTime);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
            }

            //数据封装
            return TurnoverReportVO.builder()
                    .dateList(StringUtils.join(dateList,","))
                    .turnoverList(StringUtils.join(turnoverList, ","))
                    .build();
        }
    }
