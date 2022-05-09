package com.lcb.gmall.auth.server.feign;

import com.lcb.common.utils.R;
import com.lcb.gmall.auth.server.vo.UserLoginVo;
import com.lcb.gmall.auth.server.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 *
 */
@FeignClient("gmall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);


    @PostMapping("/member/member/login")
     R login(@RequestBody UserLoginVo vo);
}
