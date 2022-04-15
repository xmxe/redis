package com.xmxe.resubmit;

import javax.servlet.http.HttpServletRequest;

/**
 * token服务接口：我们新建一个接口，创建token服务，里面主要是两个方法，一个用来创建token，一个用来验证token。
 * 创建token主要产生的是一个字符串，检验token的话主要是传达request对象，为什么要传request对象呢？主要作用就是获取header里面的token,
 * 然后检验，通过抛出的Exception来获取具体的报错信息返回给前端。
 */
public interface TokenService {

	/**
	 * 创建token * @return
	 */
	public String createToken();

	/**
	 * 检验token * @param request * @return
	 */
	public boolean checkToken(HttpServletRequest request) throws Exception;

}
