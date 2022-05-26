package com.lcb.gmall.member.exception;

/**
 * @Description:
 **/
public class UsernameException extends RuntimeException {


    public UsernameException() {
        super("存在相同的用户名");
    }
}
