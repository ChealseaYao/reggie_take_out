package com.yomi.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yomi.reggie.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
