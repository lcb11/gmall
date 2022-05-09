package com.lcb.gmall.search.controller;

import com.lcb.gmall.search.service.MallSearchService;
import com.lcb.gmall.search.vo.SearchParam;
import com.lcb.gmall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /*
      * @Author lcb
      * @Description 自动将检索结果封装成指定对象
      * @Date 2022/4/17
      * @Param [param]
      * @return java.lang.String
      **/
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){

        param.set_queryString(request.getQueryString());

        //根据页面查询参数，去es中检索商品
        SearchResult result=mallSearchService.search(param);
        model.addAttribute("result",result);

        return "list1";
    }
}
