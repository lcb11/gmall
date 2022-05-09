package com.lcb.gmall.auth.server.controller;

import com.alibaba.fastjson.TypeReference;
import com.lcb.common.constant.AuthServerConstant;
import com.lcb.common.exception.BizCodeEnume;
import com.lcb.common.utils.R;
import com.lcb.gmall.auth.server.feign.MemberFeignService;
import com.lcb.gmall.auth.server.feign.ThirdPartFeignService;
import com.lcb.gmall.auth.server.vo.UserLoginVo;
import com.lcb.gmall.auth.server.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        //TODO 接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)){
            long l = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-l<60000){
                //60s内不能再发验证码
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMessage());
            }
        }

        //验证码再次校验 redis 存k-V phone-code
        String code = UUID.randomUUID().toString().substring(0, 5);
        String substring = code+"_"+System.currentTimeMillis();

        //redis缓存验证码，设置过期时间，防止同一个手机号在60s内再次操作手机号
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,substring,10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }


    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes,
                         HttpSession httpSession){
        if(result.hasErrors()){



            /*result.getFieldErrors().stream().map(fieldError -> {
                String field=fieldError.getField();
                String defaultMessage = fieldError.getDefaultMessage();
                errors.put(field,defaultMessage);
            })*/
            //Request method 'POST' not supported
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错
            return "redirect:http://auth.gmall.com/reg.html";
        }


        //1、校验验证码
        String code = vo.getCode();

        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            if(code.equals( s.split("_")[0])){
                //验证码通过
                //删除验证码,令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //真正注册，调用远程服务
                R r = memberFeignService.regist(vo);
                if(r.getCode()==0){
                    //成功

                    return "redirect:http://auth.gmall.com/login.html";
                }else {
                    Map<String, String> errors=new HashMap<>();
                    errors.put("msg",r.getData(new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gmall.com/reg.html";
                }
            }else {
                Map<String, String> errors =new HashMap<>();
                errors.put("code","验证码错误");

                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gmall.com/reg.html";
            }
        }else {
            Map<String, String> errors =new HashMap<>();
            errors.put("code","验证码错误");

            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gmall.com/reg.html";
        }

    }

    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes){


        //远程登录
        R login = memberFeignService.login(vo);
        if(login.getCode()==0){
            //成功
            return "redirect:http://gmall.com";
        }else {
            Map<String,String> errors=new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gmall.com/login.html";
        }


    }
}
