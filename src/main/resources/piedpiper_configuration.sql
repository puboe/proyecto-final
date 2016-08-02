DROP TABLE IF EXISTS `configuration`;
CREATE TABLE `configuration` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

LOCK TABLES `configuration` WRITE;

INSERT INTO `configuration` VALUES 
(1,'Ammount of states to show by default','Startup States','startupStates','10', '1'),
(2,'Web target main path','Web Target Main Path','mainWebTargetPath','http://clouds.it.itba.edu.ar/api/', '0'),
(3,'Weather API username','Username','username','weather', '0'),
(4,'Weather API password','Password','password','121212piedpiper', '0'),
(5,'Image operation path','Image Path','imagePath','/tmp/', '1');
UNLOCK TABLES;

DROP TABLE IF EXISTS `saved_state`;
CREATE TABLE `saved_state` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date_time` datetime NOT NULL,
  `steps` int(11) NOT NULL,
  `channel` varchar(3) NOT NULL,
  `enhanced` bit(1) NOT NULL,
  UNIQUE INDEX `unique_state` (`date_time` ASC, `steps` ASC),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;