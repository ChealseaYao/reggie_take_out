package com.yomi.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yomi.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {
    void submit(Orders orders);
}
