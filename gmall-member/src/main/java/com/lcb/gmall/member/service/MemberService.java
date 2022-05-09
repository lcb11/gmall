package com.lcb.gmall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.member.entity.MemberEntity;
import com.lcb.gmall.member.exception.PhoneExsitException;
import com.lcb.gmall.member.exception.UsernameExistException;
import com.lcb.gmall.member.vo.MemberLoginVo;
import com.lcb.gmall.member.vo.MemberRegistVo;
import com.mysql.cj.jdbc.exceptions.PacketTooBigException;

import java.util.Map;

/**
 * 会员
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:33:44
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExsitException;

    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);
}

