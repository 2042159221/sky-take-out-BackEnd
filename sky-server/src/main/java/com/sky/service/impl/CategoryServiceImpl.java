package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类管理服务实现类
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增分类
     * @param categoryDTO 分类信息
     */
    @Override
    public void save(CategoryDTO categoryDTO) {
        // 判断分类名称是否已存在
        Integer count = categoryMapper.countByName(categoryDTO.getName());
        if (count > 0) {
            // 分类名称已存在
            throw new BusinessException(MessageConstant.ALREADY_EXISTS);
        }
        
        Category category = new Category();
        
        // 属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);
        
        // 设置默认状态为禁用
        category.setStatus(StatusConstant.DISABLE);
        
        // 设置创建时间和修改时间
        // category.setCreateTime(LocalDateTime.now());
        // category.setUpdateTime(LocalDateTime.now());
        
        // 设置创建人和修改人ID（从ThreadLocal中获取）
        // category.setCreateUser(BaseContext.getCurrentId());
        // category.setUpdateUser(BaseContext.getCurrentId());
        
        // 以上公共字段由AutoFill注解自动填充
        
        // 调用mapper保存分类信息
        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 开启分页
        PageHelper.startPage(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        
        // 执行查询
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        
        // 返回结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id删除分类
     * @param id 分类id
     */
    @Override
    public void deleteById(Long id) {
        // 查询当前分类是否关联菜品
        Integer count = dishMapper.countByCategoryId(id);
        if (count > 0) {
            // 当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        
        // 查询当前分类是否关联套餐
        count = setmealMapper.countByCategoryId(id);
        if (count > 0) {
            // 当前分类下有套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        
        // 执行删除操作
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO 分类信息
     */
    @Override
    public void update(CategoryDTO categoryDTO) {
        // 如果修改了分类名称，则需要判断新的分类名称是否已存在
        Category oldCategory = categoryMapper.getById(categoryDTO.getId());
        if (oldCategory != null && !oldCategory.getName().equals(categoryDTO.getName())) {
            Integer count = categoryMapper.countByName(categoryDTO.getName());
            if (count > 0) {
                // 分类名称已存在
                throw new BusinessException(MessageConstant.ALREADY_EXISTS);
            }
        }
        
        Category category = new Category();
        
        // 属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);
        
        // 设置修改时间和修改人
        // category.setUpdateTime(LocalDateTime.now());
        // category.setUpdateUser(BaseContext.getCurrentId());
        
        // 以上公共字段由AutoFill注解自动填充
        
        // 执行更新操作
        categoryMapper.update(category);
    }

    /**
     * 启用禁用分类
     * @param status 状态，1表示启用，0表示禁用
     * @param id 分类id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 创建分类对象
        Category category = Category.builder()
                .id(id)
                .status(status)
                // .updateTime(LocalDateTime.now())
                // .updateUser(BaseContext.getCurrentId())
                .build();
                
        // 以上公共字段由AutoFill注解自动填充
        
        // 执行更新操作
        categoryMapper.update(category);
    }

    /**
     * 根据类型查询分类
     * @param type 分类类型，1表示菜品分类，2表示套餐分类
     * @return 分类列表
     */
    @Override
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }
} 