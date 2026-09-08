package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    //菜品
    @Autowired
    private DishMapper dishMapper;

    //菜品口味
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    //套餐
    @Autowired
    private SetMealDishMapper setMealDishMapper;
    @Autowired
    private SetMealMapper setMealMapper;

    /**
     * 新增菜品
     */
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入一条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long id = dish.getId();
        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(id);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品的分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> record = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(record.getTotal(),record.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    public void delete(List<Long> ids) {
        //判断菜品是否能删除--是否是起售
        ids.forEach(id -> {
            Dish dish = dishMapper.getById(id);
            //判断是否是起售，如果是的话就抛不能删除的异常
            if(dish.getStatus() != StatusConstant.DISABLE){
                throw new DeletionNotAllowedException("商品起售中,不能删除");
            }
        });
        //判断菜品是否能删除--是否关联了套餐
        List<Long> setMealIds = setMealDishMapper.selectSetMealDishIdsBySetMealId(ids);
        if(setMealIds != null && setMealIds.size() > 0){
            //当前菜品关联了套餐 不能删除
            throw new DeletionNotAllowedException("当前菜品关联了套餐 不能删除");
        }
        //批量删除菜品
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味
            dishFlavorMapper.deleteById(id);
        }
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    public DishVO getById(Long id) {
        //根据主键查询菜品
        Dish dish = dishMapper.getById(id);
        //根据id查询口味
        List<DishFlavor> dishFlavors = dishFlavorMapper.getById(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 菜品的起售停售
     * @param status
     * @param id
     */
    @Transactional
    public void StartOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if(status == StatusConstant.DISABLE){
            //如果是停售操作，还需要将包含当前菜品的套餐也停售
            ArrayList<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            //根据菜品id查询套餐id
            List<Long> setMealIds = setMealDishMapper.selectSetMealDishIdsBySetMealId(dishIds);
            if(setMealIds != null && setMealIds.size() > 0){
                for (Long setMealId : setMealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setMealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setMealMapper.update(setmeal);
                }
            }
        }

    }

    /**
     * 根据id修改菜品
     * @param dishDTO
     */
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品基本信息
        dishMapper.update(dish);
        //修改菜品口味信息
        dishFlavorMapper.deleteById(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    public List<DishVO> list(Dish dish) {
        //根据分类id查询菜品
        List<Dish> dishList = dishMapper.getByCatrgoryId(dish.getCategoryId());
        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            //根据菜品id查询对应的口味
            List<DishFlavor> flavorList = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(flavorList);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
