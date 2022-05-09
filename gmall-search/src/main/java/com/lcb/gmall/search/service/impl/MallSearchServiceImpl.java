package com.lcb.gmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lcb.common.to.es.SkuEsModel;
import com.lcb.common.utils.R;
import com.lcb.gmall.search.config.GmallElasticSearchConfig;
import com.lcb.gmall.search.constant.EsConstant;
import com.lcb.gmall.search.feign.ProductFeignService;
import com.lcb.gmall.search.service.MallSearchService;
import com.lcb.gmall.search.vo.AttrResponseVo;
import com.lcb.gmall.search.vo.SearchParam;
import com.lcb.gmall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    //去es中查询
    @Override
    public SearchResult search(SearchParam param) {
        //动态构建出查询所需要的DSL语句

        SearchResult result=null;
        //准备检索请求
        SearchRequest searchRequest=buildSearchRequrest(param);
        try {
            //执行检索请求
            SearchResponse response = client.search(searchRequest, GmallElasticSearchConfig.COMMON_OPTIONS);

            //分析响应数据，封装成需要的格式
            result=buildSearchResult(response,param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /*
      * @Author lcb
      * @Description 构建结果数据
      * @Date 2022/4/17
      * @Param [response]
      * @return com.lcb.gmall.search.vo.SearchResult
      **/
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {

        SearchResult result = new SearchResult();
        //1、封装返回的所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels=new ArrayList<>();
        if(hits.getHits()!=null&&hits.getHits().length>0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProduct(esModels);

        //2、当前商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos=new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //得到属性ID
            long attrId = bucket.getKeyAsNumber().longValue();
            //得到属性名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //得到属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        //3、当前商品所涉及的所有品牌信息
        List<SearchResult.BrandVo> brandVos=new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();

            //得到品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            //得到品牌名字
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            //得到品牌图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //4、当前所有商品所涉及到的分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos=new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            //得到分类ID
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            //得到分类名-得到子聚合
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        //=========================以上从聚合信息获取==================

        //5、分页信息--页码
        result.setPageNum(param.getPageNum());

        //5、分页信息-总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        //5、分页信息-总页码
        int  totalPages = (int)total%EsConstant.PRODUCT_PAGESIZE==0?(int)total/EsConstant.PRODUCT_PAGESIZE:((int)total/EsConstant.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);


        //6、构建面包屑导航功能
        if(param.getAttrs()!=null&&param.getAttrs().size()>0) {
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }

                //取消掉面包屑，要跳到哪里,将请求地址的url置空  拿到所有查询条件
                String replace = param.get_queryString().replace("&attrs=" + attr, "");
                navVo.setLink("http://search.gmall.com/list.html?" + replace);
                System.out.println(replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }
        //品牌 分类
        if(param.getBrandId()!=null&&param.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("品牌");
            //TODO 远程查询所有品牌
            R r = productFeignService.brandsinfo(param.getBrandId());
            if(r.getCode()==0){
                List<SearchResult.BrandVo> brand = r.getData("brand", new TypeReference<List<SearchResult.BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                for (SearchResult.BrandVo brandVo : brand) {
                   buffer.append(brandVo.getBrandName()+";");
                   String replace = param.get_queryString().replace("&brands=" + brandVo, "");
                    navVo.setLink("http://search.gmall.com/list.html?" + replace);
                }
                navVo.setNavValue(buffer.toString());
            }

            navs.add(navVo);
        }

        //分类面包屑

        return result;
    }

    /*
      * @Author lcb
      * @Description  构建检索请求  模糊匹配 过滤（按照属性、分类、品牌、价格区间、库存） 排序、分页、高亮、聚合分析
      * @Date 2022/4/17
      * @Param []
      * @return org.elasticsearch.action.search.SearchRequest
      **/
    private SearchRequest buildSearchRequrest(SearchParam param) {

        SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();

        /*
          * @Author lcb
          * @Description 查询：模糊匹配 过滤（按照属性、分类、品牌、价格区间、库存）
          * @Date 2022/4/17
          **/
        //1、构建bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //1.1、must-模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }

        //1.2、filter-模糊匹配--按照三级分类ID查询
        if(param.getCatalog3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }

        //1.2、filter-模糊匹配--按照品牌ID查询
        if(param.getBrandId()!=null && param.getBrandId().size()>0){
            boolQuery.filter(QueryBuilders.termQuery("brandId",param.getBrandId()));
        }

        //1.2、filter-模糊匹配--按照所有指定属性查询
        if(param.getAttrs()!=null&&param.getAttrs().size()>0){

            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                //attrStr 检索条件
                String[] s = attrStr.split("_");
                String attrId=s[0];//检索属性id
                String[] attrValues=s[1].split(":");//这个属性检索用的值
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrValue",attrValues));
                //每一个必须都得生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        //1.2、filter-模糊匹配--按照是否有库存查询
        if(param.getHasStock()!=null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        //1.2、filter-模糊匹配--按照价格区间查询
        if (!StringUtils.isEmpty(param.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");

            String[] s = param.getSkuPrice().split("_");
            if(s.length==2){
                //区间
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length==1){
                if(param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }

                if(param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }


        //把以前所有条件都拿来进行拼装
        sourceBuilder.query(boolQuery);




        /*
          * @Author lcb
          * @Description 排序、分页、高亮
          * @Date 2022/4/17
          **/

        //2.1 排序
        if(!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order=s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }

        //2.2 分页
        sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3高亮
        if (!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder builder=new HighlightBuilder();

            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }


        /*
          * @Author lcb
          * @Description 聚合分析
          * @Date 2022/4/17
          **/
        //TODO 1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        //品牌聚合 -子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        sourceBuilder.aggregation(brand_agg);

        //TODO 2、分类聚合
        TermsAggregationBuilder catalog_agg= AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

        sourceBuilder.aggregation(catalog_agg);

        //TODO 3、属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合分析出当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析出当前attr_id对应的所有可能的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);

        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL语句"+s);
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }
}
