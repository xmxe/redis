package com.xmxe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebAppConfig implements WebMvcConfigurer {
	@Bean
	IpUrlLimitInterceptor getIpUrlLimitInterceptor(){
		return new IpUrlLimitInterceptor();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(getIpUrlLimitInterceptor())
				// 配置拦截的路径
				.addPathPatterns("/**");
	}
}