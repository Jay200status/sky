package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 查找商品是否已经存在
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 更新商品数量
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void update(ShoppingCart cart);

    /**
     * 插入商品
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name,user_id,dish_id,dish_flavor,setmeal_id,number,amount,image,create_time) " +
            "values (#{name},#{userId},#{dishId},#{dishFlavor},#{setmealId},#{number},#{amount},#{image},#{createTime});")
    void insert(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param userID
     */
    @Delete("delete from shopping_cart where user_id = #{userID}")
    void deleteById(Long userID);

    /**
     * 批量插入购物车
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
