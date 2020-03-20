CREATE TABLE IF NOT EXISTS locked_objects_members_list(id int AUTO_INCREMENT NOT NULL PRIMARY KEY,
locked_object_id int NOT NULL, uuid varchar(255) NOT NULL, added_at datetime NOT NULL,
KEY locked_object_id (locked_object_id), FOREIGN KEY (locked_object_id) REFERENCES locked_objects_list(id))
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;