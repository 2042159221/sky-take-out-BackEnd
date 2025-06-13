package com.sky.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;

@Mapper
public interface DishMapper {


    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 根据主键查询菜品
     * 
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}" )
    Dish getById(Long id);

    /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);
    
    /**
     * 根据条件统计菜品总数
     * @param dishPageQueryDTO
     * @return
     */
    Integer countByCondition(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据主键删除菜品
     * @param ids
     */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long ids);

} 