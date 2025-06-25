package com.sky.controller.admin;

import java.time.LocalDate;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.sky.controller.user.AddressBookController;
import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 报表
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
@Tag(name = "报表相关接口")
public class ReportController {

    private final AddressBookController addressBookController;

    @Autowired
    private ReportService reportService;

    ReportController(AddressBookController addressBookController) {
        this.addressBookController = addressBookController;
    }

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
    
}
