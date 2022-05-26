package com.lcb.common.to.mq;

import lombok.Data;

/**
 *
 */
@Data
public class StockLockedTo {

    private Long id;//库存工作单ID

    private StockDetailTo detail;//工作单详情ID

}
