package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品
     */
    void save(DishDTO dishDTO);

    /**
     * 菜品的分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 根据id修改菜品
     * @param dishDTO
     */
    void update(DishDTO dishDTO);

    /**
     * 根据分类id查询菜品
     * @param dish
     * @return
     */
    List<DishVO> list(Dish dish);

    /**
     * 根据主键查询菜品
     * @param id
     * @return
     */
    DishVO getById(Long id);

    /**
     * 菜品的起售和停售
     * @param status
     * @param id
     */
    void StartOrStop(Integer status, Long id);
}
