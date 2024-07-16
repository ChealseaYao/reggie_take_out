package com.yomi.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yomi.reggie.common.R;
import com.yomi.reggie.entity.Employee;
import com.yomi.reggie.entity.Orders;
import com.yomi.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private OrdersService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize){
        //构造分页构造器
        Page pageInfo = new Page(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper();

        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        //执行查询
        orderService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);

    }

    /**
     * 管理员查询订单分页
     *
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public ResponseEntity<R<Page<Orders>>> adminPage(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {

        // Define the date-time formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime startTime = null;
        LocalDateTime endDateTime = null;

        // Parse beginTime and endTime
        try {
            if (beginTime != null && !beginTime.isEmpty()) {
                startTime = LocalDateTime.parse(beginTime, formatter);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDateTime = LocalDateTime.parse(endTime, formatter);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.error("Invalid date-time format"));
        }

        // 构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        // 添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        // 添加订单号查询条件
        if (number != null && !number.isEmpty()) {
            queryWrapper.eq(Orders::getNumber, number);
        }



        // 添加时间范围查询条件
        if (startTime != null && endDateTime != null) {
            queryWrapper.between(Orders::getOrderTime, startTime, endDateTime);
        }

        // 执行查询
        orderService.page(pageInfo, queryWrapper);

        return ResponseEntity.ok(R.success(pageInfo));
    }


}
