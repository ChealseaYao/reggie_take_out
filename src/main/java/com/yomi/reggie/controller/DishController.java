package com.yomi.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yomi.reggie.common.R;
import com.yomi.reggie.dto.DishDto;
import com.yomi.reggie.entity.Category;
import com.yomi.reggie.entity.Dish;
import com.yomi.reggie.entity.DishFlavor;
import com.yomi.reggie.service.CategoryService;
import com.yomi.reggie.service.DishFlavorService;
import com.yomi.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清理某个分类下的菜品缓存数据
        String key = "dish" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null, Dish::getName, name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理某个分类下的菜品缓存数据
        String key = "dish" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 停售、启售
     *
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateDishStatus(@PathVariable("status") Integer status, @RequestParam("ids") List<Long> id) {
        if (id == null) {
            return R.error("没有这个菜品");
        }

        // 更新状态
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, id).set(Dish::getStatus, status);

        boolean updateResult = dishService.update(updateWrapper);
        if (updateResult) {
            return R.success("菜品状态更新成功");
        } else {
            return R.error("菜品状态更新失败");
        }
    }


    @DeleteMapping
    public R<String> deleteDish(@RequestParam List<Long> ids) {
        dishService.removewithIds(ids);

        return R.success("删除成功");

    }

    /**
     * 根据条件查询对应的菜品数据
     *
     * @param dish
     * @return
     */
   /* @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dishDtoList = null;
        //动态构造key
        String key = "dish" + dish.getCategoryId() + "_" + dish.getStatus();

        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if (dishDtoList != null) {
            //如果存在，直接返回
            return R.success(dishDtoList);
        }


        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, 1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，缓存
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }


}
