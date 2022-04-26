package com.xmxe.exception;

import com.xmxe.enums.ResultEnum;
import lombok.Getter;

@Getter
public class UserException extends RuntimeException{

    private Integer code;

    public UserException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public UserException(ResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.code = resultEnum.getCode();
    }
}
