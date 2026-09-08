package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //查看用户地址表是否为空 如果为空抛出业务异常
        AddressBook addressBook = addressMapper.queryById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查看用户购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        //批量插入数据
        orderMapper.BatchOrders(orderDetails);
        //清空购物车
        shoppingCartMapper.deleteById(shoppingCart.getUserId());
        //封装vo返回数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        //跳过微信支付步骤，不关注返回值，直接将上面的判断该订单已经支付的条件存进js对象中
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //支付成功后，根据订单号码orderNumber更新数据库订单状态
        Orders orders = Orders.builder()
                .number(ordersPaymentDTO.getOrderNumber())
                .payStatus(Orders.PAID)
                .status(Orders.TO_BE_CONFIRMED)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.updateByOrderNumber(orders);

        //通过websocket向客户端浏览器页面发送消息
        Map map = new HashMap();
        map.put("type",1); //1是来单提醒 2是催单提醒
        map.put("orderId",orders.getId());
        map.put("content","订单号"+ordersPaymentDTO.getOrderNumber());
        //转换为json字符串
        String jsonString = JSON.toJSONString(map);
        //通过websocket将字符串推送到页面上
        webSocketServer.sendToAllClient(jsonString);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //分页条件查询
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        ArrayList<OrderVO> list = new ArrayList<>();

        //查询出订单明细 并封装如OrderVo响应
        if(orders != null && orders.getTotal() > 0){
            for (Orders order : orders) {
                Long orderId = order.getId(); //订单id
                //查询订单明细
                List<OrderDetail> orderDetailList = orderMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(orders.getTotal(),list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //查询该订单的对应菜品或者套餐明细
        List<OrderDetail> orderDetailList = orderMapper.getByOrderId(orders.getId());
        //将该订单封装成orderVo并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancel(Long id) throws Exception {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //校验订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(orders.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders1 = new Orders();
        orders1.setId(orders.getId());
        //订单处于待接单状态下取消需要进行退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    orders.getNumber(),//商户订单号
                    orders.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            //支付状态设置为退款
            orders1.setPayStatus(Orders.REFUND);
        }
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelReason("用户取消");
        orders1.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders1);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id) {
        //当前用户id
        Long userId = BaseContext.getCurrentId();
        //根据id查询订单详情信息
        List<OrderDetail> orderDetailList = orderMapper.getByOrderId(id);
        //将订单详情转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //将订单详情里面的信息复制给购物车对象
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            return shoppingCart;
        }).collect(Collectors.toList());
        //将购物车对象批量插入数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //根据条件查询订单
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        //部分订单状态，需要额外返回订单菜品信息
        List<OrderVO> orderVOList = getOrderVOList(orders);
        return new PageResult(orders.getTotal(),orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> orders) {
        //需要返回订单菜品信息，自定义vo响应结果
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = orders.getResult();
        if(!CollectionUtils.isEmpty(orders)){
            for (Orders order : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                String orderDetails = getOrderDishesStr(order);
                //将订单彩票信息封装到ordervo中
                orderVO.setOrderDishes(orderDetails);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param order
     * @return
     */
    private String getOrderDishesStr(Orders order) {
        //查询订单菜品详情信息
        List<OrderDetail> orderDetailList = orderMapper.getByOrderId(order.getId());
        //将每一条信息拼接成字符串
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String str = x.getName() + "*" + x.getNumber() + ";";
            return str;
        }).collect(Collectors.toList());
        return String.join("",orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        //根据状态，分别查询出待接单，待派送，派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStstus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStstus(Orders.CONFIRMED);
        Integer deliveryInPogress = orderMapper.countStstus(Orders.DELIVERY_IN_PROGRESS);

        //将查询出来的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInPogress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //根据id查询订单信息
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        //判断订单是否存在且处于待接单状态
        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //支付状态
        Integer payStatus = orders.getPayStatus();
        //如果是已支付需要退款
        if(payStatus == Orders.TO_BE_CONFIRMED){
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01)
            );
            log.info("申请退款",refund);
        }
        //拒单需要退款，根据订单id更新订单状态，拒单原因，取消原因
        Orders orderNew = new Orders();
        orderNew.setId(orders.getId());
        orderNew.setStatus(Orders.CANCELLED);
        orderNew.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        BeanUtils.copyProperties(orders, orderNew);
        orderMapper.update(orderNew);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception {
        //根据id查询订单
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        //如果是已支付需要退款
        if(orders.getPayStatus() == 1){
            //用户已经支付 需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01)
            );
            log.info("申请退款",refund);
            //管理端取消订单需要退款，根据订单id更新订单状态，取消原因，取消时间
            Orders ordersNew = new Orders();
            ordersNew.setId(orders.getId());
            ordersNew.setStatus(Orders.CANCELLED);
            ordersNew.setCancelTime(LocalDateTime.now());
            ordersNew.setCancelReason(ordersCancelDTO.getCancelReason());
            orderMapper.update(ordersNew);
        }
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //判断订单是否存在，并且状态为1
        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orderNew = new Orders();
        orderNew.setId(orders.getId());
        //更新订单状态，状态转为派送中
        orderNew.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orderNew);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //判断订单是否存在，且状态为4
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orderNew = new Orders();
        orderNew.setId(orders.getId());
        //更新订单状态，转换为完成
        orderNew.setStatus(Orders.COMPLETED);
        orderNew.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orderNew);
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //判断订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //通过websocket向客户端浏览器推送消息
        Map map = new HashMap();
        map.put("type",2); //1是来单提醒 2是客户催单
        map.put("orderId",id);
        map.put("content","订单号:"+orders.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address){
        Map map = new HashMap<>();
        map.put("address", address);
        map.put("output","json");
        map.put("ak",ak);
        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺的经纬度坐标
        String shopLngLat = lat + "," + lng;
        map.put("address",address);
        //获取用户地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        jsonObject = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = jsonObject.getString("lat");
        lng = jsonObject.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;
        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }
        //数据解析
        Object result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) ((JSONObject) result).get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
