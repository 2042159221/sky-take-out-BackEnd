package com.sky.service;

import com.sky.dto.DishDTO;

/**
 * @title DishService
 * @description [简要说明类的功能]
 *
 * @author Ming
 * @date 2025/6/12
 * @version 1.0
 * @since 2025/6/12
 * @project sky-take-out
 *
 * @contact email: millionfire@outlook.com
 * @github https://github.com/2042159221
 *
 * @copyright Copyright (c) 2025
 * All rights reserved.
 */
public interface DishService {
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
