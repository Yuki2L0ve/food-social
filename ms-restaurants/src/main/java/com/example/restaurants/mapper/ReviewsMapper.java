package com.example.restaurants.mapper;

import com.example.commons.model.pojo.Reviews;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

/**
 * 评论服务 Mapper
 */
public interface ReviewsMapper {

    // 新增餐厅评论
    @Insert("INSERT INTO t_reviews (fk_restaurant_id, fk_diner_id, content, like_it, is_valid, create_date, update_date) " +
            " VALUES (#{fkRestaurantId}, #{fkDinerId}, #{content}, #{likeIt}, 1, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int saveReviews(Reviews reviews);

}
