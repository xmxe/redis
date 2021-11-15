package com.xmxe.business;

import com.xmxe.util.RedisTemplateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * [SpringBoot + Redis 解决海量重复提交问题](https://mp.weixin.qq.com/s/Ghd4Sk6vuufRiURoFS_lCA)
 */
public class ReSubmit {
}

/**
 * 自定义一个注解，定义此注解的主要目的是把它添加在需要实现幂等的方法上，凡是某个方法注解了它，都会实现自动幂等。
 * 后台利用反射如果扫描到这个注解，就会处理这个方法实现自动幂等，使用元注解ElementType.METHOD表示它只能放在方法上，etentionPolicy.RUNTIME表示它在运行时。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface AutoIdempotent {

}

/**
 * token服务接口：我们新建一个接口，创建token服务，里面主要是两个方法，一个用来创建token，一个用来验证token。
 * 创建token主要产生的是一个字符串，检验token的话主要是传达request对象，为什么要传request对象呢？主要作用就是获取header里面的token,
 * 然后检验，通过抛出的Exception来获取具体的报错信息返回给前端。
 */
interface TokenService {

	/** * 创建token * @return */
	public String createToken();

	/** * 检验token * @param request * @return */
	public boolean checkToken(HttpServletRequest request) throws Exception;

}

/**
 * token的服务实现类：token引用了redis服务，创建token采用随机算法工具类生成随机uuid字符串,
 * 然后放入到redis中(为了防止数据的冗余保留,这里设置过期时间为10000秒,具体可视业务而定)，如果放入成功，最后返回这个token值。
 * checkToken方法就是从header中获取token到值(如果header中拿不到，就从paramter中获取)，如若不存在,直接抛出异常。
 * 这个异常信息可以被拦截器捕捉到，然后返回给前端。
 */

@Service
class TokenServiceImpl implements TokenService {

	@Autowired
	private RedisTemplateUtil redisTemplate;


	/** * 创建token * * @return */
	@Override
	public String createToken() {
		String str = String.valueOf(new Random(100).nextInt(100));
		StringBuilder token = new StringBuilder();
		try {
			token.append("Constant.Redis.TOKEN_PREFIX").append(str);
			redisTemplate.setEx(token.toString(), token.toString(),10000L);
			boolean notEmpty = token.toString() != null ? true : false;
			if (notEmpty) {
				return token.toString();
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return null;
	}


	/** * 检验token * * @param request * @return */
	@Override
	public boolean checkToken(HttpServletRequest request) throws Exception {

		String token = request.getHeader("Constant.TOKEN_NAME");
		if (StringUtils.isEmpty(token) ) {// header中不存在token
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

/**
 * web配置类，实现WebMvcConfigurerAdapter，主要作用就是添加autoIdempotentInterceptor到配置类中，这样我们到拦截器才能生效，
 * 注意使用@Configuration注解，这样在容器启动是时候就可以添加进入context中。
 */
@Configuration
class WebConfiguration extends WebMvcConfigurerAdapter {

	@Resource
	private AutoIdempotentInterceptor autoIdempotentInterceptor;

	/** * 添加拦截器 * @param registry */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(autoIdempotentInterceptor);
		super.addInterceptors(registry);
	}
}

/** * 拦截器 */
@Component
class AutoIdempotentInterceptor implements HandlerInterceptor {

	@Autowired
	private TokenService tokenService;

	/** * 预处理 * * @param request * @param response * @param handler * @return * @throws Exception */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Method method = handlerMethod.getMethod();
		//被ApiIdempotment标记的扫描
		AutoIdempotent methodAnnotation = method.getAnnotation(AutoIdempotent.class);
		if (methodAnnotation != null) {
			try {
				return tokenService.checkToken(request);// 幂等性校验, 校验通过则放行, 校验失败则抛出异常, 并通过统一异常处理返回友好提示
			}catch (Exception ex){

				throw ex;
			}
		}
		//必须返回true,否则会被拦截一切请求
		return true;
	}


	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

	}

	/** * 返回的json值 * @param response * @param json * @throws Exception */
	private void writeReturnJson(HttpServletResponse response, String json) throws Exception{
		PrintWriter writer = null;
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=utf-8");
		try {
			writer = response.getWriter();
			writer.print(json);

		} catch (IOException e) {
		} finally {
			if (writer != null)
				writer.close();
		}
	}

}

/**
 * 模拟业务请求类，首先我们需要通过/get/token路径通过getToken()方法去获取具体的token，然后我们调用testIdempotence方法，
 * 这个方法上面注解了@AutoIdempotent，拦截器会拦截所有的请求，当判断到处理的方法上面有该注解的时候，就会调用TokenService中的
 * checkToken()方法，如果捕获到异常会将异常抛出调用者，下面我们来模拟请求一下：
 */
@RestController
class BusinessController {


	@Resource
	private TokenService tokenService;

	@PostMapping("/get/token")
	public String  getToken(){
		String token = tokenService.createToken();

		return token;
	}


	@AutoIdempotent
	@PostMapping("/test/Idempotence")
	public String testIdempotence() {
		return "";
	}
}