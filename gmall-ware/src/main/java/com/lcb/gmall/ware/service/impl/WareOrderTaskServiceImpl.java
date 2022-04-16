package com.lcb.gmall.ware.service.impl;

import com.lcb.common.utils.PageUtils;
import com.lcb.common.utils.Query;
import com.lcb.gmall.ware.dao.WareOrderTaskDao;
import com.lcb.gmall.ware.entity.WareOrderTaskEntity;
import com.lcb.gmall.ware.service.WareOrderTaskService;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


@Service("wareOrderTaskService")
public class WareOrderTaskServiceImpl extends ServiceImpl<WareOrderTaskDao, WareOrderTaskEntity> implements WareOrderTaskService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareOrderTaskEntity> page = this.page(
                new Query<WareOrderTaskEntity>().getPage(params),
                new QueryWrapper<WareOrderTaskEntity>()
        );

        return new PageUtils(page);
    }

}