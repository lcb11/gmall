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
public class SpuItemAttrGroupVo {

    private String groupName;

    private List<Attr> attrs;

}
