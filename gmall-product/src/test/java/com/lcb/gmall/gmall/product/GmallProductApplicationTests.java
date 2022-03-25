package com.lcb.gmall.gmall.product;

import com.lcb.gmall.product.entity.BrandEntity;
import com.lcb.gmall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallProductApplicationTests {

    @Autowired
    BrandService brandService;
    @Test
    void contextLoads() {

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("huawei");
       /* brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功.....");*/
        brandService.updateById(brandEntity);
    }

}
