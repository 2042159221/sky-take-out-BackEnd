package com.sky.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.sky.aspect.AutoFillAspect;
import com.sky.constant.MessageConstant;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.DishVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    private final AutoFillAspect autoFillAspect;

    private final AliOssUtil aliOssUtil;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper SetmealDishMapper;

    DishServiceImpl(AliOssUtil aliOssUtil, AutoFillAspect autoFillAspect) {
        this.aliOssUtil = aliOssUtil;
        this.autoFillAspect = autoFillAspect;
    }

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);

            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 获取总记录数
        int total = dishMapper.countByCondition(dishPageQueryDTO);
        log.info("菜品分页查询，总记录数：{}", total);
        
        // 分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        List<DishVO> records = dishMapper.pageQuery(dishPageQueryDTO);
        
        return new PageResult(total, records);
    }



    /**
     * 菜品批量删除
     * 
     * @param ids
     * 
     */
    @Transactional

    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售中的菜品
        for(Long id : ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除---是否被套餐关联
        List<Long> setmealDishes = SetmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealDishes != null && setmealDishes.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
        
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */

    @Override
    public DishVO getByIdWithFLavor(Long id) {
        //查询菜品基本信息
        Dish dish = dishMapper.getById(id);

        //查询菜品口味信息
        List<DishFlavor> dishFlavors = dishFlavorMapper.getById (id);
        
        //将查询到的数据封装到VO
        DishVO dishVO = new DishVO ();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }
    
    /**
     *  根据id修改菜品基本信息和口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品基本信息
        dishMapper.update(dish);

        //删除原有的口味数据
        List<DishFlavor>flavors  = dishDTO.getFlavors();
        if(flavors !=null && flavors.size() >0){
            flavors.forEach(dishFlavor ->{
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId){
        Dish dish = Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        List<Dish> list = dishMapper.list(dish);
        return list;
    }

    /**条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);
        List<DishVO> dishVOList = new ArrayList<>();
        for(Dish d : dishList){
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);

         }
        return dishVOList;
    }

}
