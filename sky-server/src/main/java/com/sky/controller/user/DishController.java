package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@Api("C端-菜品查询接口")
@RequestMapping("/user/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        //构造redis中的key
        String key = "dish_" + categoryId;
        //查询redis是否存在菜品
        List<DishVO> dishvo = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //判断是否存在菜品
        if(dishvo != null && dishvo.size() > 0){
            //如果存在直接从缓存里面拿数据
            return Result.success(dishvo);
        }
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        //如果不存在就在数据库里面查数据 并保存在redis
        dishvo = dishService.list(dish);
        redisTemplate.opsForValue().set(key,dishvo);

        return Result.success(dishvo);
    }
}
