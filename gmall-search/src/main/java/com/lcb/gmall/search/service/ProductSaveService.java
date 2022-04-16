package com.lcb.gmall.search.service;


import com.lcb.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface ProductSaveService {

    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
