package com.sky.mapper;

import com.sky.entity.Orders;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户是否存在
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByid(String openid);

    /**
     * 创建新用户
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户
     * @param userId
     * @return
     */
    @Select("select * from orders where number = #{orderNumber}")
    User getById(Long userId);

    /**
     * 用户数量统计
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
