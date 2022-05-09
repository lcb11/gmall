package com.lcb.gmall.order.feign;

import com.lcb.gmall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 *
 */
@FeignClient("gmall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/addresses")
     List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);
}
