package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入用户订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 批量插入数据
     * @param orderDetails
     */
    void BatchOrders(ArrayList<OrderDetail> orderDetails);

    /**
     * 更新订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 分页条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 各个状态的订单数量统计
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStstus(Integer status);

    /**
     * 根据orderNumber更新数据库订单信息
     * @param orders
     */
    @Update("update orders set pay_status = #{payStatus}, status = #{status}, checkout_time = #{checkoutTime} where number = #{number}")
    void updateByOrderNumber(Orders orders);

    /**
     * 查询未支付的且超时的所有任务
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimerLT(Integer status, LocalDateTime time);

    /**
     * 根据日期查询当天营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据时间和状态计算订单数
     * @param map
     * @return
     */
    Integer getByOrderTimeAndStatus(Map map);

    /**
     * 销量排名top10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSaleList(LocalDateTime begin, LocalDateTime end);

}
