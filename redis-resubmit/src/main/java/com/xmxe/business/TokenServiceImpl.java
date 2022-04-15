package com.xmxe.business;

import com.xmxe.util.RedisTemplateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Random;

/**
 * token的服务实现类：token引用了redis服务，创建token采用随机算法工具类生成随机uuid字符串,
 * 然后放入到redis中(为了防止数据的冗余保留,这里设置过期时间为10000秒,具体可视业务而定)，如果放入成功，最后返回这个token值。
 * checkToken方法就是从header中获取token到值(如果header中拿不到，就从paramter中获取)，如若不存在,直接抛出异常。
 * 这个异常信息可以被拦截器捕捉到，然后返回给前端。
 */

@Service
public class TokenServiceImpl implements TokenService {

	@Autowired
	private RedisTemplateUtil redisTemplate;


	/**
	 * 创建token * * @return
	 */
	@Override
	public String createToken() {
		String str = String.valueOf(new Random(100).nextInt(100));
		StringBuilder token = new StringBuilder();
		try {
			token.append("Constant.Redis.TOKEN_PREFIX").append(str);
			redisTemplate.setEx(token.toString(), token.toString(), 10000L);
			boolean notEmpty = token.toString() != null ? true : false;
			if (notEmpty) {
				return token.toString();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


	/**
	 * 检验token * * @param request * @return
	 */
	@Override
	public boolean checkToken(HttpServletRequest request) throws Exception {

		String token = request.getHeader("Constant.TOKEN_NAME");
		if (StringUtils.isEmpty(token)) {// header中不存在token
			token = request.getParameter("Constant.TOKEN_NAME");
			if (StringUtils.isEmpty(token)) {// parameter中也不存在token
				throw new Exception("Constant.ResponseCode.ILLEGAL_ARGUMENT");
			}
		}

		if (!redisTemplate.exists(token)) {
			throw new Exception("Constant.ResponseCode.REPETITIVE_OPERATION");
		}

		boolean remove = redisTemplate.remove(token);
		if (!remove) {
			throw new Exception("Constant.ResponseCode.REPETITIVE_OPERATION");
		}
		return true;
	}
}
