package com.sky.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.dto.GoodsSalesDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService{



    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

   

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
        
        @Override
        public UserReportVO getUserStatistics(LocalDate begin ,LocalDate end) {
            List<LocalDate> dateList = new  ArrayList<>();
            dateList.add(begin);

            while(!begin.equals(end)){
                begin = begin.plusDays(1);
                dateList.add(begin);
            }
            List<Integer> newUserList = new ArrayList<>(); //新增用户数量
            List<Integer> totalUserList = new ArrayList<>(); //总用户数

            for(LocalDate date : dateList)  {
                LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
                //新增用户数量 select count(id) from user where create_time > ? and create_time < ?
                Integer newUser = getUserCount(beginTime ,endTime);
                //总用户数量 select count(id) from user where create_time < ?
                Integer totalUser = getUserCount(null ,endTime);

                newUserList.add(newUser);
                totalUserList.add(totalUser);

            }
            return UserReportVO.builder()
                    .dateList(StringUtils.join(dateList,","))
                    .newUserList(StringUtils.join(newUserList,","))
                    .totalUserList(StringUtils.join(totalUserList, ","))
                    .build();
            
        }

        /**
         * 根据时间区间统计用户数量
         * @param beginTime
         * @param endTime
         * @return
         */
        private Integer getUserCount(LocalDateTime beginTime ,LocalDateTime endTime){
            Map<String, Object> map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            return userMapper.countByMap(map);
        }

        /**
         * 根据时间区间统计订单数量
         * @param begin
         * @param end
         * @return
         * 
         */
        public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){
            List<LocalDate> dateList =new ArrayList();
            dateList.add(begin);

            while(!begin.equals(end)){
                begin = begin.plusDays(1);
                dateList.add(begin);

            }
            //每天订单总数集合
            List<Integer> orderCountList = new ArrayList<>();
            //每天有效订单数集合
            List<Integer> validOrderCountList = new ArrayList<>();
            for (LocalDate date : dateList) {
                LocalDateTime beginTime  = LocalDateTime.of(date,LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的总订单数量 select count(id) from orders where order_time > ? and order_time < ?
                Integer orderCount = getOrderCount(beginTime,endTime,null);

            //查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = ?
                Integer validOrderCount = getOrderCount(beginTime,endTime,Orders.COMPLETED);
                orderCountList.add(orderCount);
                validOrderCountList.add(validOrderCount);
            }
            //时间区间内的总订单数量
            Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
            //时间区间内的总有效订单数
            Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
            //订单完成率
            Double orderCompletionRate = 0.0;
            if(totalOrderCount != 0){
                orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            }
            return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
        }

        /**
         * 根据时间区间统计订单数量
         * @param beginTime
         * @param endTime
         * @param status
         * @return
         */
    private Integer getOrderCount(LocalDateTime beginTime ,LocalDateTime endTime ,Integer status){
        Map<String, Object> map = new HashMap<>();
        map.put("begin",beginTime);
        map.put("end",endTime);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }

    /**
     * 根据时间区间统计销量排名
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin,LocalDate end){
       LocalDateTime beginTime = LocalDateTime.of(begin ,LocalTime.MIN);
       LocalDateTime endTime = LocalDateTime.of(end , LocalTime.MAX);
       List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime,endTime);
       String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
       String numberlist = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");
       return SalesTop10ReportVO.builder()
            .nameList(nameList)
            .numberList(numberlist)
            .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response){
        //查询数据库，获取营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查询概览运营数据，提供给Excel模板文件
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN),LocalDateTime.of(end,LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于提供好的模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获取Excel文件中的第一个Sheet
            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue(begin +"至" + end + "期间的运营数据");
            //获取第四行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for(int i = 0; i < 30; i++){
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
                row = sheet.getRow(7 + i );
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
                
            }
            //通过输出流将Excel文件下载到客户端浏览器中
            String fileName = URLEncoder.encode("运营数据报表" + begin + "至" + end + ".xlsx", "UTF-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }    
}
