package com.sky.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.service.SetmealService;
import com.sky.dto.SetmealDTO;
import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Tag(name = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService SetmealService;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping("path")
    @Operation(summary = "新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        SetmealService.saveWithDish(setmealDTO);
        return Result.success();
    }
    
}
