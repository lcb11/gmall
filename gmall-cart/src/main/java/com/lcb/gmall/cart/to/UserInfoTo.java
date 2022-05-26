package com.lcb.gmall.cart.to;

import lombok.Data;

/**
 * @Description:
 **/

@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    /**
     * 是否临时用户
     */
    private Boolean tempUser = false;

}
