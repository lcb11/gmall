package com.lcb.gmall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.to.mq.OrderTo;
import com.lcb.common.to.mq.StockLockedTo;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.ware.entity.WareSkuEntity;
import com.lcb.gmall.ware.vo.LockStockResult;
import com.lcb.gmall.ware.vo.SkuHasStockVo;
import com.lcb.gmall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:59:40
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);


    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}

