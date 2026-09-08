package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {
    /**
     * 根据菜品id查询套餐id
     * @param setMealIds
     * @return
     */
    List<Long> selectSetMealDishIdsBySetMealId(List<Long> setMealIds);

    /**
     * 插入套餐菜品关系数据
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 删除菜品和套餐的关系
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId};")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐id查询套餐与菜品的关联关系
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId};")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
