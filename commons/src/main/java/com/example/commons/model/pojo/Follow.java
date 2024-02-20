package com.example.commons.model.pojo;

import com.example.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "食客关注实体类")
@Setter
@Getter
public class Follow extends BaseModel {
    @ApiModelProperty("用户ID")
    private int dinerId;            // 食客id
    @ApiModelProperty("关注用户ID")
    private Integer followDinerId;  // 该食客所关注的对象id
}
