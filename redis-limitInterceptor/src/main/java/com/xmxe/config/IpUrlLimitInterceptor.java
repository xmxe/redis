package com.xmxe.config;

import com.alibaba.fastjson.JSON;
import com.xmxe.util.IpAddressUtils;
import com.xmxe.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * ip+url重复请求现在拦截器
 */
@Slf4j
public class IpUrlLimitInterceptor implements HandlerInterceptor {

	@Autowired
	RedisUtil redisUtil;

	private static final String LOCK_IP_URL_KEY="lock_ip_";

	private static final String IP_URL_REQ_TIME="ip_url_times_";

	private static final long LIMIT_TIMES=5;

	private static final int IP_LOCK_TIME=60;

	@Override
	public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
		log.info("request请求地址uri={},ip={}", httpServletRequest.getRequestURI(), IpAddressUtils.getIpAddr(httpServletRequest));
		if (ipIsLock(IpAddressUtils.getIpAddr(httpServletRequest))){
			log.info("ip访问被禁止={}",IpAddressUtils.getIpAddr(httpServletRequest));
			returnJson(httpServletResponse, JSON.toJSONString(""));
			return false;
		}
		if(!addRequestTime(IpAddressUtils.getIpAddr(httpServletRequest),httpServletRequest.getRequestURI())){
			returnJson(httpServletResponse, JSON.toJSONString(""));
			return false;
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

	}

	/**
	 * @Description: 判断ip是否被禁用
	 * @param ip
	 * @return java.lang.Boolean
	 */
	private Boolean ipIsLock(String ip){

		if(redisUtil.hasKey(LOCK_IP_URL_KEY+ip)){
			return true;
		}
		return false;
	}
	/**
	 * @Description: 记录请求次数
	 * @param ip
	 * @param uri
	 * @return java.lang.Boolean
	 */
	private Boolean addRequestTime(String ip,String uri){
		String key=IP_URL_REQ_TIME+ip+uri;

		if (redisUtil.hasKey(key)){
			long time=redisUtil.incr(key,(long)1);
			if (time>=LIMIT_TIMES){
				redisUtil.getLock(LOCK_IP_URL_KEY+ip,ip,IP_LOCK_TIME);
				return false;
			}
		}else {
			redisUtil.getLock(key,(long)1,1);
		}
		return true;
	}

	private void returnJson(HttpServletResponse response, String json) throws Exception {
		PrintWriter writer = null;
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/json; charset=utf-8");
		try {
			writer = response.getWriter();
			writer.print(json);
		} catch (IOException e) {
			log.error("LoginInterceptor response error ---> {}", e.getMessage(), e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}


}