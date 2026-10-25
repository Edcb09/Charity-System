-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: charity
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '留言用户ID',
  `content` text NOT NULL COMMENT '留言内容',
  `create_time` datetime DEFAULT NULL COMMENT '留言时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '0正常 1已删除',
  PRIMARY KEY (`comment_id`),
  KEY `fk_comments_project` (`project_id`),
  KEY `fk_comments_user` (`user_id`),
  CONSTRAINT `fk_comments_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`),
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='留言表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comments`
--

/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
INSERT INTO `comments` VALUES (1,5,4,'希望能帮到孩子们','2026-10-25 03:05:18',0),(2,5,2,'希望能帮到孩子们','2026-10-25 03:05:47',0);
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;

--
-- Table structure for table `donation`
--

DROP TABLE IF EXISTS `donation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `donation` (
  `donation_id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) DEFAULT NULL COMMENT '捐款订单号',
  `user_id` bigint NOT NULL COMMENT '捐赠人ID，匿名时仍保留后台记录，前端隐藏',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `amount` decimal(12,2) NOT NULL COMMENT '捐款金额',
  `is_anonymous` tinyint DEFAULT '0' COMMENT '0实名 1匿名',
  `pay_status` tinyint DEFAULT '0' COMMENT '0待支付 1已支付',
  `donate_time` datetime DEFAULT NULL COMMENT '捐款时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`donation_id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `fk_donation_user` (`user_id`),
  KEY `fk_donation_project` (`project_id`),
  CONSTRAINT `fk_donation_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`),
  CONSTRAINT `fk_donation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='捐款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `donation`
--

/*!40000 ALTER TABLE `donation` DISABLE KEYS */;
INSERT INTO `donation` VALUES (1,'1eedb51b97d84e9cbb398290c716c6c0',2,4,100.00,0,1,'2026-10-25 03:04:21',NULL),(2,'ce43913f8ff74ba7b7aef18b907229c9',2,4,100.00,1,1,'2026-10-25 03:04:23',NULL),(3,'d00981b37b0146849d0c4de83ff71e67',2,5,5000.00,0,1,'2026-10-25 03:04:53',NULL),(4,'b5e089c81158412f8da9adde5950d3bd',4,5,5000.00,0,1,'2026-10-25 03:05:09',NULL);
/*!40000 ALTER TABLE `donation` ENABLE KEYS */;

--
-- Table structure for table `fund_report`
--

DROP TABLE IF EXISTS `fund_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fund_report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `content` text COMMENT '资金使用说明',
  `report_img` text COMMENT '资金凭证文件(JSON格式)',
  `report_amount` decimal(12,2) NOT NULL COMMENT '本次支出金额',
  `status` tinyint DEFAULT '0' COMMENT '0待审核 1已公示 2驳回',
  `reject_reason` text COMMENT '驳回理由',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`report_id`),
  KEY `fk_fund_report_project` (`project_id`),
  CONSTRAINT `fk_fund_report_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金使用公示表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fund_report`
--

/*!40000 ALTER TABLE `fund_report` DISABLE KEYS */;
INSERT INTO `fund_report` VALUES (1,5,'支出金额：¥5000\n使用用途：为本项目 10 名偏远乡村留守女童发放本学期助学金，每人 500 元，用于补贴学费、作业本、教辅资料以及上下学通勤路费，帮扶孩子稳定完成学业。\n备注：附有学生签收表、身份备案资料，资金定向发放至受助学生家庭。','[{\"name\":\"273ad7f4e0b5b0426fb391adf29123ca~tplv-a9rns2rl98-pc_smart_face_crop-v1_350_262.jpg\",\"url\":\"/images/85ea900e-1123-4075-b677-7feec16ae26e.jpg\"},{\"name\":\"支出金额.docx\",\"url\":\"/images/d52ad0f9-6a65-4273-9f9c-49c487a8627e.docx\"},{\"name\":\"支出金额.pdf\",\"url\":\"/images/9e8d288c-652c-4f02-9499-ac4a47e27f0e.pdf\"},{\"name\":\"支出.xlsx\",\"url\":\"/images/2c8ea64b-0e03-452d-a429-a9893dd50022.xlsx\"}]',5000.00,1,NULL,'2026-10-25 03:16:53','2026-10-25 03:16:58'),(2,5,'支出金额：¥5000\n使用用途：采购课外文学读物、科普绘本、练习册、书包、笔袋、文具套装等学习物资，用于搭建乡村班级图书角，供给项目帮扶的留守女童阅读使用。\n备注：附带采购清单、采购发票、物资入库照片。','',5000.00,1,NULL,'2026-10-25 03:18:08','2026-10-25 03:18:14');
/*!40000 ALTER TABLE `fund_report` ENABLE KEYS */;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `msg_id` bigint NOT NULL AUTO_INCREMENT,
  `receive_user_id` bigint NOT NULL COMMENT '接收用户ID',
  `content` text COMMENT '消息内容',
  `type` tinyint DEFAULT NULL COMMENT '1项目审核结果 2捐款成功 3资金报告审核结果 4举报处理结果',
  `link_id` bigint DEFAULT NULL COMMENT '关联业务ID（project_id或donation_id）',
  `is_read` tinyint DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_receive_read` (`receive_user_id`,`is_read`),
  CONSTRAINT `fk_message_user` FOREIGN KEY (`receive_user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

/*!40000 ALTER TABLE `message` DISABLE KEYS */;
INSERT INTO `message` VALUES (1,9,'您发起的项目【残障伙伴康复支持与生活赋能项目】已通过审核，募捐项目已启动。',1,4,0,'2026-10-25 02:27:09'),(2,9,'您发起的项目【乡村困难学子助学护航计划】已通过审核，募捐项目已启动。',1,3,0,'2026-10-25 02:27:10'),(3,9,'您发起的项目【乡村留守老人温情帮扶行动】已通过审核，募捐项目已启动。',1,2,0,'2026-10-25 02:27:12'),(4,9,'您发起的项目【乡村留守儿童暖心守护计划】已通过审核，募捐项目已启动。',1,1,0,'2026-10-25 02:27:12'),(5,3,'您发起的项目【困境老人暖居帮扶】已通过审核，募捐项目已启动。',1,6,1,'2026-10-25 02:59:12'),(6,3,'您发起的项目【乡村图书室建设】已通过审核，募捐项目已启动。',1,7,1,'2026-10-25 03:02:03'),(7,3,'您发起的项目【山区女童助学计划】已通过审核，募捐项目已启动。',1,5,1,'2026-10-25 03:02:03'),(8,3,'您的项目【乡村图书室建设】因违反平台规定，已被管理员强制下架。',1,7,1,'2026-10-25 03:03:26'),(9,2,'您已成功向项目【残障伙伴康复支持与生活赋能项目】捐款100.00元，感谢您的爱心！',2,4,1,'2026-10-25 03:04:21'),(10,2,'您已成功向项目【残障伙伴康复支持与生活赋能项目】捐款100.00元，感谢您的爱心！',2,4,1,'2026-10-25 03:04:23'),(11,2,'您已成功向项目【山区女童助学计划】捐款5000.00元，感谢您的爱心！',2,5,1,'2026-10-25 03:04:53'),(12,4,'您已成功向项目【山区女童助学计划】捐款5000.00元，感谢您的爱心！',2,5,1,'2026-10-25 03:05:09'),(13,1,'申请人申请将项目【山区女童助学计划】结项归档，请前往后台审核。',1,5,0,'2026-10-25 03:18:21');
/*!40000 ALTER TABLE `message` ENABLE KEYS */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `title` varchar(100) NOT NULL COMMENT '项目标题',
  `description` text COMMENT '项目描述',
  `recipient_info` text COMMENT '受助人信息',
  `proof_img` varchar(255) DEFAULT NULL COMMENT '证明材料图片',
  `target_amount` decimal(12,2) NOT NULL COMMENT '目标筹款金额',
  `current_amount` decimal(12,2) DEFAULT '0.00' COMMENT '已筹金额',
  `end_time` datetime DEFAULT NULL COMMENT '募捐截止时间',
  `project_type` varchar(30) DEFAULT NULL COMMENT '助学/医疗/救灾',
  `status` tinyint DEFAULT '0' COMMENT '0草稿 1待审核 2募捐中 3已结束 4驳回 5已归档 6结项待审核 7已下架',
  `reject_reason` text COMMENT '驳回理由',
  `cover_img` varchar(255) DEFAULT NULL COMMENT '封面图',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `finish_time` datetime DEFAULT NULL COMMENT '实际达标或结束时间',
  PRIMARY KEY (`project_id`),
  KEY `fk_project_user` (`user_id`),
  CONSTRAINT `fk_project_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='募捐项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,9,'乡村留守儿童暖心守护计划','在偏远山区乡镇，有一批留守儿童常年和爷爷奶奶一同生活，父母在外打工一年难得回家一次。孩子们缺少课外书籍、文体用品，也缺少专业的心理陪伴引导。本次募集的善款，将统一采购图书、文具、体育器材，搭建乡村爱心书屋，定期安排志愿者入校开展心理陪伴、兴趣课堂，丰富孩子们的课余生活，弥补陪伴缺失。项目所有物资采购与资金使用明细都会对外公示，让每一份爱心都落到孩子们身上。','某山区乡镇在读留守儿童群体，共 72 名，父母长期在外务工，大多跟随祖辈生活，缺少陪伴，课外读物、文具、心理疏导资源匮乏。',NULL,10000.00,0.00,'2026-11-01 00:00:00','其他',2,NULL,'/images/eaa0341d-330d-487a-87b2-b7f3c193ccf2.jpg','2026-10-25 02:24:58','2026-10-25 02:27:12',NULL),(2,9,'乡村留守老人温情帮扶行动','这片村落里很多老人子女外出务工，独自留守家中。不少老人体弱多病，日常买菜买药、居家安全都存在困难。本次募捐资金将用于为老人们采购保暖物资、常用基础药品、助行小设备，同时组织志愿者定期上门探访，帮忙打扫家务、陪同问诊。希望通过大家的爱心，改善留守老人的生活条件，让独居老人感受到社会的关怀，资金使用全程留档公示，接受公众监督。','村落留守老年群体，共 5 位独居、空巢老人，大多年过七旬，收入微薄，行动不便，缺少日常照料、基础医疗物资。',NULL,10000.00,0.00,'2026-11-01 00:00:00','医疗',2,NULL,'/images/99466845-a421-428c-af6f-ae0e03de84ea.jpg','2026-10-25 02:25:50','2026-10-25 02:27:12',NULL),(3,9,'乡村困难学子助学护航计划','这批学生学习认真刻苦，但受家庭条件限制，学费、住宿费、教辅资料和生活费给家庭带来很重的负担，部分孩子随时面临学业中断。本次募捐筹集的资金，将作为助学金，补贴孩子们在校生活与学习开销，同时配套发放书包、教辅、保暖衣物，持续支持他们安心读书。我们会持续跟踪孩子们的学习情况，定期发布项目进展和善款流向，守护这群孩子的求学路。','县域内家庭困难中小学生群体，共 46 名，来自低保、单亲、重病家庭，家庭收入难以负担学习、食宿开支。',NULL,10000.00,0.00,'2026-11-01 00:00:00','助学',2,NULL,'/images/86d6a05a-bf00-4755-a47c-6f2c2b546329.jpg','2026-10-25 02:26:25','2026-10-25 02:27:10',NULL),(4,9,'残障伙伴康复支持与生活赋能项目','社区内不少残障伙伴行动、自理存在障碍，长期康复训练需要耗材与辅助器具，很多家庭无力持续承担相关费用。本次募捐资金将用于采购康复器材、护理用品、助辅工具，同时支持常态化康复课程、手工技能培训，帮助他们提升自理能力，学习简单手艺，增加收入机会。项目物资统一登记发放，定期公示进展，助力残障伙伴更好地融入生活。','社区持证残障群体，共 39 人，包含肢体、智力残障人士，收入有限，康复训练耗材、辅助器具不足。',NULL,190000.00,200.00,'2026-11-01 00:00:00','医疗',2,NULL,'/images/e86aa564-e03f-44ac-b152-0a1d400890fd.jpg','2026-10-25 02:26:59','2026-10-25 02:27:09',NULL),(5,3,'山区女童助学计划','大山里不少女童因家庭经济拮据面临辍学，本项目为她们募集助学金、教辅书籍与生活用品，补贴学费和通勤开支，支持她们安心完成中小学学业，用教育拓宽走出大山的道路，守护每一份求学的希望。','偏远乡村留守女童',NULL,10000.00,10000.00,'2026-11-01 00:00:00','助学',5,NULL,'/images/2e8e2843-4610-4bd9-a026-b54b0a812574.jpg','2026-10-25 02:44:22','2026-10-25 03:02:03','2026-10-25 03:05:09'),(6,3,'困境老人暖居帮扶','不少乡村独居老人收入微薄、行动不便，家中居住环境简陋。项目募集资金用于修缮房屋、添置保暖物资、无障碍扶手以及日常米面粮油，定期安排志愿者上门陪伴照料，改善老人居住条件，让孤寡老人感受温暖。','独居低保、失能高龄老人',NULL,1000.00,0.00,'2026-10-26 00:00:00','其他',2,NULL,'/images/f09344a1-1565-4f0b-a07a-d94026012edb.jpg','2026-10-25 02:48:22','2026-10-25 02:59:12',NULL),(7,3,'乡村图书室建设','很多乡村学校图书存量少、种类老旧，孩子们缺少课外阅读资源。项目募集资金采购课外读物、书架与阅读桌椅，在乡村学校搭建爱心图书室，丰富孩子们的阅读世界，开拓视野。','乡村小学全体学生',NULL,10000.00,0.00,'2026-11-01 00:00:00','助学',7,NULL,'/images/404595a3-b8af-4766-bab2-e9e587bec775.jpg','2026-10-25 02:49:00','2026-10-25 03:02:03',NULL);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '举报用户ID',
  `reason` varchar(255) NOT NULL COMMENT '举报理由',
  `status` tinyint DEFAULT '0' COMMENT '0待处理 1已处理 2已驳回',
  `create_time` datetime DEFAULT NULL COMMENT '举报时间',
  `handle_result` varchar(255) DEFAULT NULL COMMENT '处理结果说明',
  `admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
  `target_type` tinyint DEFAULT '1' COMMENT '1项目 2留言',
  `comment_id` bigint DEFAULT NULL COMMENT '举报的留言ID',
  PRIMARY KEY (`report_id`),
  KEY `fk_report_admin` (`admin_id`),
  KEY `fk_report_project` (`project_id`),
  KEY `fk_report_user` (`user_id`),
  CONSTRAINT `fk_report_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_report_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

/*!40000 ALTER TABLE `report` DISABLE KEYS */;
/*!40000 ALTER TABLE `report` ENABLE KEYS */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `role` tinyint DEFAULT NULL COMMENT '0捐赠人 1项目申请人 2管理员',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `status` tinyint DEFAULT '0' COMMENT '0正常 1禁用',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$50TAL4AdU1C7P1JdDn4DV.TwXY9TNp7QqDO3BM9L0DsKrA3NHyuQS','1号管理员','13800000000',2,'2026-10-13 20:00:20',0,NULL,'255@qq.com'),(2,'zhangsan','$2a$10$8gcY709CfXnJ932czinFG.PDsP5fFrBln4FaEtC6nrDKfi9Y00SQ.','张三',NULL,0,'2026-10-13 21:17:22',0,NULL,NULL),(3,'lisi','$2a$10$8gcY709CfXnJ932czinFG.PDsP5fFrBln4FaEtC6nrDKfi9Y00SQ.','李四','13800000001',1,'2026-10-14 19:21:43',0,'/images/8e43c68a-b987-4696-b0b8-184eb294638c.png',NULL),(4,'wangwu','$2a$10$IcUN6lhKdxoOo2hll.shO.2f5VObQ5UOwRhCW12QjdesZkfAvvbr2','王五','13345678901',0,'2026-10-20 04:39:15',0,NULL,NULL),(9,'apply','$2a$10$iRL4UPeSjQ1lb642X1Yequsj2LQENsoUx611P5IkwqhzWMPuWnPrC','申请人','',1,'2026-10-25 00:13:27',0,'/images/39b33a17-65fb-40ee-b554-7d7000512a65.jpg',NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-25  7:51:14
