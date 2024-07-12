package com.yomi.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yomi.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
