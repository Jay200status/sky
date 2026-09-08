package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.TurnoverReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TurnoverReportServiceImpl implements TurnoverReportService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnOverStatistics(LocalDate begin, LocalDate end) {
        //当前list存放begin到end的日期
        ArrayList<LocalDate> list = new ArrayList<>();

        list.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            list.add(begin);
        }
        //计算begin到end日期的营业额
        ArrayList<Double> amountList = new ArrayList<>();
        for (LocalDate date : list) {
            LocalDateTime beginDate = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endDate = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginDate);
            map.put("end", endDate);
            map.put("status", Orders.COMPLETED);
            //根据map中的数据查询当天营业额
            Double sum = orderMapper.sumByMap(map);
            sum = sum == null ? 0.0 : sum;
            amountList.add(sum);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .turnoverList(StringUtils.join(amountList,","))
                .build();
    }

    /**
     * 指定时间内用户数量统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前list用来存放begin到end的日期
        ArrayList<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            list.add(begin);
        }
        //存放每天的新用户数量
        ArrayList<Integer> newUserList = new ArrayList<>();
        //存放每天的总用户数量
        ArrayList<Integer> allUserList = new ArrayList<>();

        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            //计算总的用户数
            map.put("end", endTime);
            Integer countAll = userMapper.countByMap(map);
            allUserList.add(countAll);
            //计算新用户数
            map.put("begin", beginTime);
            Integer countUser = userMapper.countByMap(map);
            newUserList.add(countUser);

        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .totalUserList(StringUtils.join(allUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 指定时间内订单数量统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderReportVO(LocalDate begin, LocalDate end) {
        //存放begin到end的日期
        ArrayList<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            list.add(begin);
        }

        ArrayList<Integer> AllOrderCountList = new ArrayList<>();
        ArrayList<Integer> ValidOrderCountList = new ArrayList<>();
        for (LocalDate localDate : list) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            //查找对应日期的总订单数
            Integer AllOrderCount = getOrderCount(beginTime, endTime, null);
            AllOrderCountList.add(AllOrderCount);
            //查找对应日期的有效订单数
            Integer ValidOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            ValidOrderCountList.add(ValidOrderCount);
        }
        //订单总数
        Integer allOrderNumber = AllOrderCountList.stream().reduce(0, Integer::sum);
        //有效订单数
        Integer validOrderNumber = ValidOrderCountList.stream().reduce(0, Integer::sum);
        //订单完成率
        Double completion = 0.0;
        if(validOrderNumber != 0){
            completion = allOrderNumber.doubleValue() / validOrderNumber;
        }
        //封装vo返回数据
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(StringUtils.join(list, ","))
                .orderCountList(StringUtils.join(AllOrderCountList, ","))
                .validOrderCountList(StringUtils.join(ValidOrderCountList, ","))
                .totalOrderCount(allOrderNumber)
                .validOrderCount(validOrderNumber)
                .orderCompletionRate(completion)
                .build();
        return orderReportVO;
    }

    /**
     * 销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSaleList(beginTime,endTime);
        List<String> names = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 根据时间和状态计算订单数
     * @param begin
     * @param end
     * @param status
     * @return
     */
    public Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.getByOrderTimeAndStatus(map);
    }

    /**
     * 导出营业数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询近30天的营业数据
        LocalDate beginTime = LocalDate.now().minusDays(30);
        LocalDate endTime = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginTime, LocalTime.MIN), LocalDateTime.of(endTime, LocalTime.MAX));
        //2.根据poi写入到excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //根据已经存在的表创建excel对象
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取表格对象
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间是:" + beginTime + "至" + endTime);
            //获取第四行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            //获取第五行
            row = sheet.getRow(5);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            //填充中间每日数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = LocalDate.now().plusDays(1);
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获取第七行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());

            }
            //3.根据输出流下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            excel.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
