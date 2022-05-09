package com.lcb.common.exception;

/**
 *
 */
public class NoStockException extends RuntimeException {

    private Long skuId;
    public NoStockException(Long skuId){
        super("商品"+skuId+"没有足够库存");
    }
    public NoStockException(String data){
        super("库存锁定失败");
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
