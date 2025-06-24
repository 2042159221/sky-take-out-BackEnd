package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderSubmitVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderServiceImpl测试类
 * 主要测试地址配送范围检查功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务测试")
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private WeChatPayUtil weChatPayUtil;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private OrderDetailMapper orderDetailMapper;
    
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    
    @Mock
    private AddressBookMapper addressBookMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final String TEST_AK = "BQJQUAaMKyautoBFSDKVjuqRuh5N9Jn8";
    private static final String TEST_SHOP_ADDRESS = "广东省广州市黄埔区九龙街区凤福花园8栋";

    @BeforeEach
    void setUp() {
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(orderService, "ak", TEST_AK);
        ReflectionTestUtils.setField(orderService, "shopAddress", TEST_SHOP_ADDRESS);
    }

    @Test
    @DisplayName("测试配送范围内的地址 - 正常情况")
    void testCheckOutOfRange_WithinRange() throws Exception {
        // 准备测试数据
        String testAddress = "广东省广州市黄埔区九龙街区凤福花园9栋";
        
        // Mock百度地图API响应 - 店铺地址解析
        String shopCoordinateResponse = createSuccessfulGeocodeResponse("113.123456", "23.123456");
        
        // Mock百度地图API响应 - 客户地址解析
        String userCoordinateResponse = createSuccessfulGeocodeResponse("113.124456", "23.124456");
        
        // Mock路线规划API响应 - 距离在范围内（3000米）
        String routeResponse = createSuccessfulRouteResponse(3000);

        try (MockedStatic<HttpClientUtil> mockedHttpClient = mockStatic(HttpClientUtil.class)) {
            mockedHttpClient.when(() -> HttpClientUtil.doGet(eq("http://api.map.baidu.com/geocoding/v3"), any(Map.class)))
                    .thenReturn(shopCoordinateResponse)
                    .thenReturn(userCoordinateResponse);
            
            mockedHttpClient.when(() -> HttpClientUtil.doGet(eq("http://api.map.baidu.com/directionlite/v1/driving"), any(Map.class)))
                    .thenReturn(routeResponse);

            // 执行测试 - 使用反射调用私有方法
            Method checkOutOfRangeMethod = OrderServiceImpl.class.getDeclaredMethod("checkOutOfRange", String.class);
            checkOutOfRangeMethod.setAccessible(true);
            
            // 应该不抛出异常
            assertDoesNotThrow(() -> {
                try {
                    checkOutOfRangeMethod.invoke(orderService, testAddress);
                } catch (Exception e) {
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    @DisplayName("测试配送范围外的地址 - 超出范围")
    void testCheckOutOfRange_OutOfRange() throws Exception {
        // 准备测试数据
        String testAddress = "北京市朝阳区某地址";
        
        // Mock百度地图API响应 - 店铺地址解析
        String shopCoordinateResponse = createSuccessfulGeocodeResponse("113.123456", "23.123456");
        
        // Mock百度地图API响应 - 客户地址解析（北京的坐标）
        String userCoordinateResponse = createSuccessfulGeocodeResponse("116.123456", "39.123456");
        
        // Mock路线规划API响应 - 距离超出范围（6000米）
        String routeResponse = createSuccessfulRouteResponse(6000);

        try (MockedStatic<HttpClientUtil> mockedHttpClient = mockStatic(HttpClientUtil.class)) {
            mockedHttpClient.when(() -> HttpClientUtil.doGet(eq("http://api.map.baidu.com/geocoding/v3"), any(Map.class)))
                    .thenReturn(shopCoordinateResponse)
                    .thenReturn(userCoordinateResponse);
            
            mockedHttpClient.when(() -> HttpClientUtil.doGet(eq("http://api.map.baidu.com/directionlite/v1/driving"), any(Map.class)))
                    .thenReturn(routeResponse);

            // 执行测试 - 使用反射调用私有方法
            Method checkOutOfRangeMethod = OrderServiceImpl.class.getDeclaredMethod("checkOutOfRange", String.class);
            checkOutOfRangeMethod.setAccessible(true);
            
            // 应该抛出OrderBusinessException异常
            OrderBusinessException exception = assertThrows(OrderBusinessException.class, () -> {
                try {
                    checkOutOfRangeMethod.invoke(orderService, testAddress);
                } catch (Exception e) {
                    if (e.getCause() instanceof OrderBusinessException) {
                        throw (OrderBusinessException) e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            });
            
            assertEquals("超出配送范围", exception.getMessage());
        }
    }

    // 辅助方法：创建成功的地理编码响应
    private String createSuccessfulGeocodeResponse(String lng, String lat) {
        JSONObject response = new JSONObject();
        response.put("status", "0");
        
        JSONObject result = new JSONObject();
        JSONObject location = new JSONObject();
        location.put("lng", lng);
        location.put("lat", lat);
        result.put("location", location);
        response.put("result", result);
        
        return response.toJSONString();
    }

    // 辅助方法：创建成功的路线规划响应
    private String createSuccessfulRouteResponse(int distance) {
        JSONObject response = new JSONObject();
        response.put("status", "0");
        
        JSONObject result = new JSONObject();
        JSONArray routes = new JSONArray();
        JSONObject route = new JSONObject();
        route.put("distance", distance);
        routes.add(route);
        result.put("routes", routes);
        response.put("result", result);
        
        return response.toJSONString();
    }
}