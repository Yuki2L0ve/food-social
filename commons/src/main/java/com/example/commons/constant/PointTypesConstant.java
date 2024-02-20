package com.example.commons.constant;


import lombok.Getter;

/**
 * 积分类型
 */
@Getter
public enum PointTypesConstant {

    sign(0), // 签到
    follow(1), // 关注
    feed(2), // 添加Feed
    review(3), // 添加商户评论
    ;

    private int type;

    PointTypesConstant(int key) {
        this.type = type;
    }

}
