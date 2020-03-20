CREATE TABLE IF NOT EXISTS locked_objects_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY,
world_uuid varchar(255) NOT NULL, loc_x int NOT NULL, loc_y int NOT NULL, loc_z int NOT NULL,
type varchar(255) NOT NULL, owner_id varchar(255) NOT NULL, level int NOT NULL,
created_at datetime NOT NULL, last_break_attempt datetime, break_protection_time datetime,
destroyed_at datetime, destroy_guilty varchar(255), destroy_reason varchar(255),
KEY owner_id (owner_id), FOREIGN KEY (owner_id) REFERENCES player_list(id))
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;