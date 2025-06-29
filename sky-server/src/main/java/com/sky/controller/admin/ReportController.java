package com.sky.controller.admin;

import java.time.LocalDate;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;



/**
 * 报表
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
@Tag(name = "报表相关接口")
public class ReportController {


    @Autowired
    private ReportService reportService;

  
    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     * 
     */
    @GetMapping("/turnoverStatistics")
    @Operation(summary = "营业额数据统计")
    public Result<TurnoverReportVO> turnoverStatistics(
        @DateTimeFormat(pattern = "yyyy-mm-dd")
                LocalDate begin,
        @DateTimeFormat(pattern = "yyy-mm-dd")
                LocalDate end
    ){
        return Result.success(reportService.getTurnover(begin, end));
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @Operation(summary = "用户数据统计")
    public Result<UserReportVO> userStatistics(
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        return Result.success(reportService.getUserStatistics(begin,end));
    }

    /**
     * 订单数量统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @Operation(summary ="用户数据统计")
    public Result<OrderReportVO> orderStatistics(
        @DateTimeFormat(pattern = "yyy-MM-dd")
                LocalDate begin,
        @DateTimeFormat(pattern ="yyyy-MM-dd")
                LocalDate end){
                    return Result.success(reportService.getOrderStatistics(begin,end));

                }


    /**
     * 销量排名统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @Operation(summary ="销量排名统计")
    public Result<SalesTop10ReportVO> top10(
        @DateTimeFormat(pattern = "yyy-MM-dd")
                LocalDate begin,
        @DateTimeFormat(pattern = "yyy-MM-dd")
                LocalDate end
    ){
        return Result.success(reportService.getSalesTop10(begin,end));
    }

    /**
     * 导出运营数据报表
     * @param respons
     */
    @GetMapping("/export")
    @Operation(summary = "导出运营数据报表")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
