package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressMapper;
import com.sky.result.Result;
import com.sky.service.AddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api("地址相关接口")
@RequestMapping("/user/addressBook")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressMapper addressMapper;
    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result AddAddress(@RequestBody AddressBook addressBook) {
        addressService.add(addressBook);
        return Result.success();
    }

    /**
     * 查询用户所有地址
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询用户所有地址")
    public Result<List<AddressBook>> list(){
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressService.list(addressBook);
        return Result.success(list);
    }

    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> defaultAddress(){
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(1);
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressService.list(addressBook);
        if(list != null && list.size() > 0){
            return Result.success(list.get(0));
        }
        return Result.error("没有查询到默认地址");
    }

    /**
     * 根据id修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result update(@RequestBody AddressBook addressBook){
        addressService.update(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result delete(Long id){
        addressService.delete(id);
        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> queryById(@PathVariable Long id){
        AddressBook addressBook = addressService.queryById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("根据id设置默认地址")
    public Result updateDefault(@RequestBody AddressBook addressBook){
        addressService.updateDefault(addressBook);
        return Result.success();
    }
}
