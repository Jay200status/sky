package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 根据id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("服务端查看菜品")
    public Result<List<DishVO>> list(Long categoryId){
        //构造redis中的key
        String key = "dish_" + categoryId;
        //查询redis中是否存在菜品
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //判断是否存在菜品
        if(list != null && list.size() > 0){
            //如果存在直接从redis中拿数据
            return Result.success(list);
        }

        //如果不存在就从数据库中拿数据
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        //存到redis中
        list = dishService.list(dish);
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品");
        //清理缓存数据
        String key = dishDTO.getCategoryId().toString();
        clearCache(key);
        dishService.save(dishDTO);
        return Result.success();
    }

    /**
     * 菜品的分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品的分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品的分页查询:{}",dishPageQueryDTO);
        PageResult result = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(result);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除:{}",ids);
        //清理缓存数据
        clearCache("dish_**");
        dishService.delete(ids);
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}",id);
        DishVO dish = dishService.getById(id);
        return Result.success(dish);
    }

    /**
     * 根据id修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("根据id修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("根据id修改菜品:{}",dishDTO);
        //清理缓存数据
        clearCache("dish_**");
        dishService.update(dishDTO);

        return Result.success();
    }

    /**
     * 菜品的起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品的起售和停售")
    public Result<String> startOrStop(@PathVariable Integer status,Long id){

        dishService.StartOrStop(status,id);
        clearCache("dish_*");
        return Result.success();
    }
    /**
     * 清理缓存数据
     * @param pattern
     */
    private void clearCache(String pattern){
        Set<Object> keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
