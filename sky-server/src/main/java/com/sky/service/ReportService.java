package com.sky.service;

import java.time.LocalDate;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import jakarta.servlet.http.HttpServletResponse;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;   

public interface ReportService {
    /**
     * 根据时间区间统计营业额
     * @param beginTime
     * @param endTime
     * @return
     */
    TurnoverReportVO getTurnover(LocalDate beginTime , LocalDate endTime);

    /**
     * 根据时间区间通国际用户数量
     * @param begin 
     * @parm end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin,LocalDate end);

    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate begin,LocalDate end);

    /**
     * 根据时间区间统计销量排名
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin,LocalDate end);

    /**
     * 导出运营数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
