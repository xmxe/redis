package com.xmxe.controller;

import com.xmxe.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
public class SearchController {
	/**
	 * 根据输入词汇判断是否是敏感词汇或非法字符
	 */
	@GetMapping("search")
	public String search(String searchkey) {

		//非法敏感词汇判断
		SensitiveFilter filter = null;
		try {
			filter = SensitiveFilter.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int n = filter.CheckSensitiveWord(searchkey, 0, 1);
		if (n > 0) { //存在非法字符
			log.info("这个人输入了非法字符--> {},不知道他到底要查什么~ userid--> {}", searchkey, "userid");
			return null;

			// 也可将敏感文字替换*等字符
			// SensitiveFilter filter = SensitiveFilter.getInstance();
			// String text = "敏感文字";
			// String x = filter.replaceSensitiveWord(text, 1, "*");

		}

		return "";
	}
}