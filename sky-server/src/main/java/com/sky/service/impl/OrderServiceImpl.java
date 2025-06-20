package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.controller.user.AddressBookController;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
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
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final AddressBookController addressBookController;

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


    OrderServiceImpl(AddressBookController addressBookController) {
        this.addressBookController = addressBookController;
    }


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
        for (ShoppingCart Cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(Cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        //插入N条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(order.getId()).orderNumber(order.getNumber()).orderAmount(order.getAmount()).orderTime(order.getOrderTime()).build();

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
        Orders orders = Orders.builder().id(ordersDB.getId()).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID).checkoutTime(LocalDateTime.now()).build();

        orderMapper.update(orders);
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

        return  new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        //根据id查询订单
        Orders orders =orderMapper.getById(id);

        //查询订单对应的菜品、套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        //将该订单机器详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }
}
