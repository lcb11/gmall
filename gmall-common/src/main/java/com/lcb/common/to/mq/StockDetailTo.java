package com.lcb.common.to.mq;

import lombok.Data;

/**
 *
 */
@Data
public class StockDetailTo {
    /**
     * id
     */
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;
    /**
     * @Author lcb
     * @Description 仓库ID
     * @Date 2022/5/17
     * @Param
     * @return
     **/
    private Long wareId;

    private Integer lockStatus;
}
