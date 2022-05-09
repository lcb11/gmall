package com.lcb.gmall.search.service;

import com.lcb.gmall.search.vo.SearchParam;
import com.lcb.gmall.search.vo.SearchResult;

public interface MallSearchService {

    SearchResult search(SearchParam param); //根据检索参数，返回所需要的结果
}
