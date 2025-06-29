package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;

import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    
    @Autowired
    private AddressBookMapper addressBookMapper;
    
    @Value("${sky.baidu.ak}")
    private String ak;
    
    @Value("${sky.shop.address}")
    private String shopAddress;
    
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况处理（收获地址为空、超出配送范围、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_FOUND);
        }

        //检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //查询当前用户购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.PENDING_PAYMENT);
        order.setStatus(Orders.UN_PAID);

        orderMapper.insert(order);

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        //插入N条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
            .id(order.getId())
            .orderNumber(order.getNumber())
            .orderAmount(order.getAmount())
            .orderTime(order.getOrderTime())
            .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        //当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        log.info("用户订单支付：{}, 用户ID: {}", ordersPaymentDTO.getOrderNumber(), userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
            ordersPaymentDTO.getOrderNumber(),//商户订单号
            new BigDecimal(0.01),//支付金额。单位：元
            "苍穹外卖订单",//商品描述
            user.getOpenid());//微信用户openid

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")){
            throw new OrderBusinessException("该订单已经支付");
        }
        
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        // 如果是模拟支付模式，自动更新订单状态为已支付
        if (weChatPayUtil.isMockPayMode()) {
            log.info("模拟支付模式：自动更新订单状态为已支付, 订单号: {}", ordersPaymentDTO.getOrderNumber());
            // 调用支付成功处理方法
            paySuccess(ordersPaymentDTO.getOrderNumber());
        }

        return vo;    
    }

    /**
     * 支付成功，修改订单服务
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        //当前登录用户
        Long userId = BaseContext.getCurrentId();
        //根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        //根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
            .id(ordersDB.getId())
            .status(Orders.TO_BE_CONFIRMED)
            .payStatus(Orders.PAID)
            .checkoutTime(LocalDateTime.now())
            .build();

        orderMapper.update(orders);
        
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);//消息类型，1表示来单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);

        // 通过WebSocket给管理端发送消息，提醒有新的订单
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户端订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        //设置分页参数
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        //分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        
        List<OrderVO> list = new ArrayList<>();

        //查询出订单明细，并封装入OrderVO进行响应
        if(page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();//订单id

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);

        //查询订单对应的菜品、套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        //将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        //根据id 查询订单
        Orders ordersDB = orderMapper.getById(id);

        //校验订单是否存在
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                ordersDB.getNumber(), 
                ordersDB.getNumber(), 
                new BigDecimal(0.01), 
                new BigDecimal(0.01)
            );

            //支付状态修改为已退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id) {
        //根据id查询订单
        Long userId = BaseContext.getCurrentId();

        //根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            
            //将原订单详情里面的菜品信息重新赋值到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车对象批量插入购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("分页参数 - page: {}, pageSize: {}", ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        
        // 使用PageHelper进行分页，确保参数正确传递
        com.github.pagehelper.Page<Orders> page = com.github.pagehelper.PageHelper.startPage(
            ordersPageQueryDTO.getPage(),
            ordersPageQueryDTO.getPageSize()
        );

        // 执行查询
        page = orderMapper.pageQuery(ordersPageQueryDTO);
        log.info("page.getResult()内容: {}", page.getResult());

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        //根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);

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
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                ordersDB.getNumber(),
                ordersDB.getNumber(),
                new BigDecimal(0.01),
                new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        //拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 订单只有存在且状态为1（待付款）2（待接单） 3（已接单）才可以取消
        if (ordersDB == null || 
            (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED) && 
             !ordersDB.getStatus().equals(Orders.CONFIRMED) && 
             !ordersDB.getStatus().equals(Orders.PENDING_PAYMENT))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                ordersDB.getNumber(),
                ordersDB.getNumber(),
                new BigDecimal(0.01),
                new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        //管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 订单只有存在且状态为3（已接单）才可以进行派送
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        //更新订单状态，状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 订单只有存在且状态为4（派送中）才可以完成
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);
        String shopCoordinate = HttpClientUtil.doGet("http://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);

        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONObject location = result.getJSONObject("location");
        String lng = location.getString("lng");
        String lat = location.getString("lat");
        //店铺经纬度
        String shopLngLat = lng + "," + lat;

        //查询客户的坐标
        map.put("address", address);
        String userCoordinate = HttpClientUtil.doGet("http://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        result = jsonObject.getJSONObject("result");
        location = result.getJSONObject("location");
        lng = location.getString("lng");
        lat = location.getString("lat");
        //客户经纬度
        String userLngLat = lng + "," + lat;

        //解析店铺和客户的距离
        map.put("origin", shopLngLat);
        map.put("destinations", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("http://api.map.baidu.com/directionlite/v1/driving", map);
        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("路线规划失败");
        }

        //数据解析
        result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = result.getJSONArray("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 用户催单
     * @param id
     */
    public void reminder(Long id) {
        // 查询订单是否存在
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

       //基于WebSocket实现催单
       Map map = new HashMap<>();
       map.put("type", 2);//2代表用户催单
       map.put("orderId",id);
       map.put("content","订单号：" + orders.getNumber());
       webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

}
