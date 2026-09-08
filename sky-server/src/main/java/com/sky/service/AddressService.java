package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressService {
    /**
     * 新增地址
     * @param addressBook
     */
    void add(AddressBook addressBook);

    /**
     * 查询用户所有地址
     * @return
     */
    List<AddressBook> list(AddressBook addressBook);

    /**
     * 根据id修改地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    void delete(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    AddressBook queryById(Long id);

    /**
     * 根据id设置默认地址
     *
     * @param addressBook
     */
    void updateDefault(AddressBook addressBook);
}
