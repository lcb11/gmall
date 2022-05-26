package com.lcb.gmall.ware.feign;

import com.lcb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gmall-order")
public interface OrderFeignService {

    @GetMapping("/order/order/status/{orderSn}")
     R getOrderStatus(@PathVariable("orderSn") String orderSn);
}
