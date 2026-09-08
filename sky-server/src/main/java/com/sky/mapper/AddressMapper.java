package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressMapper {
    /**
     * 新增地址
     * @param addressBook
     */
    @Insert("insert into address_book (user_id,consignee,sex,phone,province_code,province_name,city_code,city_name,district_code,district_name,detail,label,is_default) " +
            "values (#{userId},#{consignee},#{sex},#{phone},#{provinceCode},#{provinceName},#{cityCode},#{cityName},#{districtCode},#{districtName},#{detail},#{label},#{isDefault})")
    void insert(AddressBook addressBook);

    /**
     * 获取当前用户所有地址
     * @param userID
     * @return
     */
    List<AddressBook> getBYUserId(AddressBook userID);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    @Delete("delete from address_book where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Select("select * from address_book where id = #{id}")
    AddressBook queryById(Long id);

    /**
     * 根据用户id把所有地址设置为不是默认地址
     * @param addressBook
     */
    @Update("update address_book set is_default = 0 where user_id = #{userId}")
    void setNoDefault(AddressBook addressBook);
}
