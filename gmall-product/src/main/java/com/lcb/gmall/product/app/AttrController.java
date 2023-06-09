package com.lcb.gmall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.lcb.gmall.product.vo.AttrRespVo;
import com.lcb.gmall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lcb.gmall.product.service.AttrService;
import com.lcb.common.utils.PageUtils;
import com.lcb.common.utils.R;



/**
 * 商品属性
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-21 16:53:24
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @GetMapping("/{attrType}/list/{catelogId}")
   public R baseAttrList(@RequestParam Map<String, Object> params,
                         @PathVariable("catelogId") Long catelogId,
                         @PathVariable("attrType") String type){

        PageUtils page=attrService.queryBaseAttrPage(params,catelogId,type);
       return R.ok().put("page", page);
   }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		//AttrEntity attr = attrService.getById(attrId);
        AttrRespVo respVo=attrService.getAttrInfo(attrId);



        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }


    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
