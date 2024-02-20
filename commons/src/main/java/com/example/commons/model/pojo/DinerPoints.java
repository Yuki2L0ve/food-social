package com.example.commons.model.pojo;

import com.example.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel
@Setter
@Getter
public class DinerPoints extends BaseModel {
    @ApiModelProperty("关联食客ID")
    private Integer fkDinerId;
    @ApiModelProperty("积分")
    private Integer points;
    @ApiModelProperty(name = "积分类型", example = "0=签到，1=关注好友，2=添加Feed，3=添加商户评论")
    private Integer types;
}