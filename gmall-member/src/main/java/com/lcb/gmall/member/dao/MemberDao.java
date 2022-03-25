package com.lcb.gmall.member.dao;

import com.lcb.gmall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:33:44
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
