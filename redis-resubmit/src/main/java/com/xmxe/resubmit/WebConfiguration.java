package com.xmxe.resubmit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;

/**
 * web配置类，实现WebMvcConfigurerAdapter，主要作用就是添加autoIdempotentInterceptor到配置类中，这样我们到拦截器才能生效，
 * 注意使用@Configuration注解，这样在容器启动是时候就可以添加进入context中。
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurerAdapter {

	@Resource
	private AutoIdempotentInterceptor autoIdempotentInterceptor;

	/**
	 * 添加拦截器 * @param registry
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(autoIdempotentInterceptor);
		super.addInterceptors(registry);
	}
}
