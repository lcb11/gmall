package com.lcb.gmall.order.dao;

import com.lcb.gmall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:49:40
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
