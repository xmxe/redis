package com.xmxe.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Follow {
	Integer id;
    Integer user_id;
	Integer follow_user_id;
	Integer is_valid;
	Date create_date;
	Date update_date;
}