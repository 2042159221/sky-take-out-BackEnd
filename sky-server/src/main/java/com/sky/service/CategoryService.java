package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

/**
 * 分类管理服务接口
 */
public interface CategoryService {
    
    /**
     * 新增分类
     * @param categoryDTO 分类信息
     */
    void save(CategoryDTO categoryDTO);
    
    /**
     * 分页查询
     * @param categoryPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);
    
    /**
     * 根据id删除分类
     * @param id 分类id
     */
    void deleteById(Long id);
    
    /**
     * 修改分类
     * @param categoryDTO 分类信息
     */
    void update(CategoryDTO categoryDTO);
    
    /**
     * 启用禁用分类
     * @param status 状态，1表示启用，0表示禁用
     * @param id 分类id
     */
    void startOrStop(Integer status, Long id);
    
    /**
     * 根据类型查询分类
     * @param type 分类类型，1表示菜品分类，2表示套餐分类
     * @return 分类列表
     */
    List<Category> list(Integer type);
} 