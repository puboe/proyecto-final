DROP TABLE IF EXISTS configuration;
CREATE TABLE configuration (
  id serial NOT NULL,
  description varchar(255) DEFAULT NULL,
  display_name varchar(255) DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  value varchar(255) DEFAULT NULL,
  active boolean,
  PRIMARY KEY (id)
) ;

INSERT INTO configuration(id, description, display_name, name, value, active) VALUES 
(1,'Ammount of states to show by default','Startup States','startupStates','10', '1'),
(2,'Web target main path','Web Target Main Path','mainWebTargetPath','http://clouds.it.itba.edu.ar/api/', '0'),
(3,'Weather API username','Username','username','weather', '0'),
(4,'Weather API password','Password','password','121212piedpiper', '0'),
(5,'Image operation path','Image Path','imagePath','/tmp/', '1');

DROP TABLE IF EXISTS saved_state;
CREATE TABLE saved_state (
  id serial NOT NULL,
  date_time timestamp NOT NULL,
  steps int NOT NULL,
  channel varchar(3) NOT NULL,
  enhanced boolean NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX unique_state on saved_state (date_time ASC, steps ASC);