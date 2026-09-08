package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.ibatis.annotations.Select;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface TurnoverReportService {
    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnOverStatistics(LocalDate begin, LocalDate end);

    /**
     * 指定时间内用户数量统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 指定时间内的订单数量统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderReportVO(LocalDate begin, LocalDate end);

    /**
     * 销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出营业数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
