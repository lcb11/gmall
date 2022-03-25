package com.lcb.gmall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.ware.entity.WareInfoEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:56:47
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}
