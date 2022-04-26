package com.xmxe.handle;

import com.xmxe.exception.UserException;
import com.xmxe.utils.ResultVOUtils;
import com.xmxe.vo.ResultVO;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandle {

    @ResponseBody
    @ExceptionHandler(value = UserException.class)
    public ResultVO handle(Exception e){
        if (e instanceof UserException){
            UserException userException = (UserException) e;
            return ResultVOUtils.error(userException.getCode(), userException.getMessage());
        }
        return null;
    }
}
