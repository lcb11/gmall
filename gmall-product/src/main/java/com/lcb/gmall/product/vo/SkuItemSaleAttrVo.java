package com.lcb.gmall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
  * @Author lcb
  * @Description
  * @Date 2022/4/22
  * @Param
  * @return
  **/

@Data
@ToString
public class SkuItemSaleAttrVo {

    private Long attrId;

    private String attrName;

    private List<AttrValueWithSkuIdVo> attrValues;

}
