package com.yomi.reggie.dto;

import com.yomi.reggie.entity.Setmeal;
import com.yomi.reggie.entity.SetmealDish;
import lombok.Data;

import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}

