package com.lcb.gmall.order.interceptor;

import com.lcb.vo.MemberResVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResVo> threadLocal=new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MemberResVo memberResVo = new MemberResVo();
        memberResVo.setMemberId(123456L);
        threadLocal.set(memberResVo);
        return true;
    }
}
