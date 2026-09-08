package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressMapper;
import com.sky.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressMapper addressMapper;

    /**
     * 新增地址
     * @param addressBook
     */
    public void add(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        addressBook.setUserId(userId);
        addressBook.setIsDefault(0);
        addressMapper.insert(addressBook);
    }

    /**
     * 查询用户所有地址
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        List<AddressBook> list = addressMapper.getBYUserId(addressBook);
        return list;
    }

    /**
     * 根据id修改地址
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    public void delete(Long id) {
        addressMapper.deleteById(id);
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    public AddressBook queryById(Long id) {
        AddressBook addressBook = addressMapper.queryById(id);
        return addressBook;
    }

    /**
     * 根据id设置默认地址
     *
     * @param addressBook
     */
    public void updateDefault(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        addressBook.setUserId(userId);
        addressBook.setIsDefault(0);
        //先把该用户的所有地址设置为不是默认地址
        addressMapper.setNoDefault(addressBook);
        //最后根据id设置默认地址
        addressBook.setIsDefault(1);
        addressMapper.update(addressBook);
    }
}
