package com.lcb.gmall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.lcb.common.exception.NoStockException;
import com.lcb.common.to.mq.OrderTo;
import com.lcb.common.to.mq.StockDetailTo;
import com.lcb.common.to.mq.StockLockedTo;
import com.lcb.common.utils.PageUtils;
import com.lcb.common.utils.Query;
import com.lcb.common.utils.R;
import com.lcb.gmall.ware.dao.WareSkuDao;
import com.lcb.gmall.ware.entity.WareOrderTaskDetailEntity;
import com.lcb.gmall.ware.entity.WareOrderTaskEntity;
import com.lcb.gmall.ware.entity.WareSkuEntity;
import com.lcb.gmall.ware.feign.OrderFeignService;
import com.lcb.gmall.ware.feign.ProductFeignService;
import com.lcb.gmall.ware.service.WareOrderTaskDetailService;
import com.lcb.gmall.ware.service.WareOrderTaskService;
import com.lcb.gmall.ware.service.WareSkuService;
import com.lcb.gmall.ware.vo.OrderItemVo;
import com.lcb.gmall.ware.vo.OrderVo;
import com.lcb.gmall.ware.vo.SkuHasStockVo;
import com.lcb.gmall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderFeignService orderFeignService;




    private void  unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId){
        wareSkuDao.unLockStock(skuId,wareId,num);

        //跟新库存工作单状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);//变为已解锁
        orderTaskDetailService.updateById(entity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /**
         * skuId: 1
         * wareId: 2
         */
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }


        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 还可以用什么办法让异常出现以后不回滚？高级
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");

                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }


            wareSkuDao.insert(skuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            //查询sku总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /*
     * @Author lcb
     * @Description 为某个订单锁定库存
     * @Date 2022/5/8
     * @Param [vo]
     * @return java.util.List<com.lcb.gmall.ware.vo.LockStockResult>
     **/
    @Transactional(rollbackFor = NoStockException.class)//默认只要运行时异常都会回滚，所以可以不加
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        //保存库存工作单详情
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);


        //找到每个商品在那个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在那个仓库有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());


        //锁定库存  每个商品都锁定成功
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked=false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有任何仓库有库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功返回1，否则返回0
               Long count= wareSkuDao.lockSkuStock(skuId,wareId,hasStock.getNum());
               if(count==1){
                   skuStocked=true;
                   //TODO 告诉mq库存锁定成功
                   WareOrderTaskDetailEntity orderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, null, hasStock.getNum(), taskEntity.getId(), wareId, 1);
                   orderTaskDetailService.save(orderTaskDetailEntity);
                   StockLockedTo lockedTo = new StockLockedTo();
                   lockedTo.setId(taskEntity.getId());
                   StockDetailTo stockDetailTo = new StockDetailTo();
                   BeanUtils.copyProperties(orderTaskDetailEntity,stockDetailTo);
                   //只发ID不行，防止回滚以后找不到数据
                   lockedTo.setDetail(stockDetailTo);
                   rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);
                   break;
               }else {
                   ////当前仓库失败，下一个仓库

               }
            }
            if(skuStocked==false){
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //全部锁定成功

        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {

            StockDetailTo detail = to.getDetail();
            Long skuId = detail.getSkuId();
            Long detailId = detail.getId();
            //查询数据库关于这个订单的锁库存消息
            WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
            if(byId!=null){
                //解锁
                Long id = to.getId();//库存工作单的id
                WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
                String orderSn = taskEntity.getOrderSn();//根据订单号查询订单状态
                R r = orderFeignService.getOrderStatus(orderSn);
                if(r.getCode()==0){
                    //订单数据返回成功
                    OrderVo data = r.getData(new TypeReference<OrderVo>() {
                    });
                    if(data==null||data.getStatus()==4){
                        //订单被取消了 订单不存在
                        if(byId.getLockStatus()==1){
                            //当前库存工作单为1 即已锁定但是为未解锁
                            unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                        }

                    }
                }else {
                    //消息拒绝重新放回队列，让别人继续消费解锁
                    throw new RuntimeException("远程服务失败");
                }
            }else {

            }
    }

    //防止订单服务卡顿，导致订单状态一直改变不了，库存消息优先到期，查询订单新建状态，什么都不做就走了
    //导致卡顿订单永远无法解锁库存
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询库存解锁状态，防止重复解锁库存
        //R r = orderFeignService.getOrderStatus(orderSn);
        WareOrderTaskEntity task=orderTaskService.getOrderByOrderSn(orderSn);
        Long id=task.getId();
        //按照工作单找到所i有没有解锁的库存
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("taSK_id", id)
                        .eq("lock_status", 1));

        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }

    }


    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}