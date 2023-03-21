package com.lcb.gmall.ware.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lcb.gmall.ware.entity.WareInfoEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仓库信息
 *
 */
@Mapper
public interface WareInfoDao extends BaseMapper<WareInfoEntity> {
	
}
