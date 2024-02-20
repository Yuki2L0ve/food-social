package com.example.points.mapper;

import com.example.commons.model.pojo.DinerPoints;
import com.example.commons.model.vo.DinerPointsRankVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 积分服务 Mapper
 */
public interface DinerPointsMapper {

    /**
     * 添加积分
     */
    @Insert("INSERT INTO t_diner_points (fk_diner_id, points, types, is_valid, create_date, update_date) " +
            "VALUES (#{fkDinerId}, #{points}, #{types}, 1, NOW(), NOW())")
    int save(DinerPoints dinerPoints);

    //查询积分排行榜  topN
    @Select("select " +
            " t1.fk_diner_id id," +
            "sum(t1.points) total," +
            "RANK() over ( order by sum(t1.points) desc ) ranks," +
            "t2.nickname," +
            "t2.avatar_url " +
            " from t_diner_points t1 " +
            " left join t_diners t2 on t1.fk_diner_id = t2.id " +
            " where t1.is_valid = 1 and t2.is_valid = 1 " +
            " group by t1.fk_diner_id " +
            " order by total desc limit #{top}")
    List<DinerPointsRankVO> findTopN(@Param("top") int top);

    // 根据食客 ID 查询当前食客积分排名
    @Select("SELECT id, total, ranks, nickname, avatar_url " +
            " FROM ( " +
            " SELECT " +
            " t1.fk_diner_id id, " +
            " sum(t1.points) total, " +
            " RANK() over ( ORDER BY sum(t1.points) DESC ) ranks, " +
            " t2.nickname, " +
            " t2.avatar_url " +
            " FROM t_diner_points t1 " +
            " LEFT JOIN t_diners t2 ON t1.fk_diner_id = t2.id " +
            " WHERE t1.is_valid = 1 AND t2.is_valid = 1 " +
            " GROUP BY t1.fk_diner_id " +
            " ORDER BY total DESC " +
            " ) t " +
            " WHERE id = #{dinerId}")
    DinerPointsRankVO findDinerRank(@Param("dinerId") Integer dinerId);

}