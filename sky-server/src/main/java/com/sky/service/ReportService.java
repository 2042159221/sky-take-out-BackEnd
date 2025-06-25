package com.sky.service;

import java.time.LocalDate;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

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

}
