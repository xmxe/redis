package com.xmxe.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultVO<T> {

    /**
     * 返回码
     */
    private Integer code;

    /**
     * 返回信息
     */
    private String msg;

    /**
     * 返回的数据
     */
    private T data;

}