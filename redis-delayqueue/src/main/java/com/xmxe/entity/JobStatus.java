package com.xmxe.entity;

import lombok.Getter;

/**
 * 任务状态
 **/
@Getter
public enum JobStatus {

	/**
	 * 可执行状态，等待消费
	 */
	READY,
	/**
	 * 不可执行状态，等待时钟周期
	 */
	DELAY,
	/**
	 * 已被消费者读取，但还未得到消费者的响应
	 */
	RESERVED,
	/**
	 * 已被消费完成或者已被删除
	 */
	DELETED;
}
