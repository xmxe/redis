CREATE TABLE `t_follow` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `user_id` int(11) DEFAULT NULL COMMENT '当前登录用户的id',
    `follow_user_id` int(11) DEFAULT NULL COMMENT '当前登录用户关注的用户的id',
    `is_valid` tinyint(1) DEFAULT NULL COMMENT '关注状态，0-没有关注，1-关注了',
    `create_date` datetime DEFAULT NULL,
    `update_date` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户和用户关注表';