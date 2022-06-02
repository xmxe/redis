package com.xmxe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MyWebAppConfig extends WebMvcConfigurerAdapter {
	@Bean
	IpUrlLimitInterceptor getIpUrlLimitInterceptor(){
		return new IpUrlLimitInterceptor();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(getIpUrlLimitInterceptor()).addPathPatterns("/**");
		super.addInterceptors(registry);
	}
}
