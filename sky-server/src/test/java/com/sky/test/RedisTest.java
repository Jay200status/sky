package com.sky.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

//@SpringBootTest
public class RedisTest {
//    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test() {
        System.out.println(redisTemplate);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        HashOperations hashOperations = redisTemplate.opsForHash();
        SetOperations setOperations = redisTemplate.opsForSet();
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        ListOperations listOperations = redisTemplate.opsForList();
    }

    /**
     * 操作字符串类型的数据
     */
    @Test
    public void testString() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("key1", "value1");
        valueOperations.set("key2", "value2");
        valueOperations.set("key3", "value3",5, TimeUnit.MINUTES);
        Object object = valueOperations.get("key3");
        System.out.println(object);
        //key不重复的时候才会设置
        valueOperations.setIfAbsent("key4", "value4");
        valueOperations.setIfAbsent("key5", "value5");
    }

    /**
     * 操作hash类型的数据
     */
    @Test
    public void testHash(){
        //hset hget hdel hkeys hvals
        HashOperations hashOperations = redisTemplate.opsForHash();

        hashOperations.put("key","field1","value1");
        hashOperations.put("key","field2","value2");
        String str = (String) hashOperations.get("key", "field1");
        System.out.println(str);
        Set keys = hashOperations.keys("key");
        System.out.println(keys);
        List values = hashOperations.values("key");
        System.out.println(values);
        hashOperations.delete("key","field1");
    }

    /**
     * 操作列表类型的数据
     */
    @Test
    public void testList(){
        //lpush lrange lpop llen
        ListOperations listOperations = redisTemplate.opsForList();
        listOperations.leftPushAll("key6","value1","key2","value2");
        listOperations.leftPushAll("key7","value2","key3","value3");
        listOperations.leftPush("key7","value7");

        List key6 = listOperations.range("key6", 0, -1);
        System.out.println(key6);

        listOperations.leftPop("key7");

        Long size = listOperations.size("key6");
        System.out.println(size);
    }

    /**
     * 操作集合类型的数据
     */
    @Test
    public void testSet(){
        SetOperations setOperations = redisTemplate.opsForSet();
        setOperations.add("set1", "a","b","c");
        setOperations.add("set2", "value2");

        //获取集合成员
        Set set1 = setOperations.members("set1");
        System.out.println(set1);

        //获取集合长度
        Long size = setOperations.size("set1");
        System.out.println(size);

        //移除集合对象
        setOperations.remove("set1", "a");
        System.out.println(setOperations.size("set1"));

        //获取交集
        Set intersect = setOperations.intersect("set1","set2");
        System.out.println(intersect);

        //获取并集
        Set union = setOperations.union("set1", "set2");
        System.out.println(union);
    }

    /**
     * 操作有序集合的数据
     */
    @Test
    public void testZSet(){
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.add("zset1","a",10);
        zSetOperations.add("zset2","b",20);
        zSetOperations.add("zset3","c",30);

        Set zset1 = zSetOperations.range("zset1", 0, -1);
        System.out.println(zset1);
        //为某个特定数据增加
        zSetOperations.incrementScore("zset3","c",10);

        zSetOperations.remove("zset3","c");
        System.out.println(zSetOperations.size("zset3"));
    }

    /**
     * 通用命令
     */
    @Test
    public void testCommon(){
        //获取所有的键
        Set keys = redisTemplate.keys("*");
        System.out.println(keys);

        Boolean set1 = redisTemplate.hasKey("set1");
        System.out.println(set1);

        for (Object key : keys) {
            //获取键的类型
            DataType type = redisTemplate.type(key);
            System.out.println(type);
            //获取键的值
            System.out.println(type.name());
        }

        redisTemplate.delete("set1");
    }
}
