package com.lcb.gmall.order.feign;

import com.lcb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 */
@FeignClient("gmall-product")
public interface ProductFeignService {

    @GetMapping(value = "/product/spuinfo/skuId/{skuId}")
     R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);
}
