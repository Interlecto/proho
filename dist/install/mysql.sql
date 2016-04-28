
drop database if exists proho;
create database proho character set utf8 collate utf8_spanish_ci;
use proho;

drop table if exists stuff;
create table stuff (
	id int not null auto_increment primary key,
	codename char(20) character set ascii not null,
	description varchar(60) character set utf8 collate utf8_general_ci
);
insert into stuff(id,codename,description)
values (0,'website','Este sitio web');

drop table if exists users;
create table users (
	id int not null auto_increment primary key,
	username char(20) character set ascii not null,
	email varchar(60) character set ascii,
	given_name varchar(60),
	surname varchar(60)
);

drop table if exists permit_levels;
create table permit_levels (
	`level` tinyint not null primary key,
	`name` char(20) character set ascii not null,
	description varchar(60)
);
insert into permit_levels(`level`,`name`,description)
values
	(0,'not_allowed','Sin permiso'),
	(1,'read','Ver información básica'),
	(2,'read2','Ver información privilegiada'),
	(3,'read3','Ver toda la información'),
	(4,'edit','Editar información básica'),
	(5,'edit2','Editar toda la información'),
	(6,'edit3','Editar toda la información'),
	(7,'create','Crear información básica'),
	(8,'create2','Crear información privilegiada'),
	(9,'create3','Crear toda la información');

drop table if exists user_permit;
create table user_permit (
	user_id int not null,
	stuff_id int not null,
	`level` tinyint not null default 1,
	foreign key (user_id) references users(id) on delete cascade,
	foreign key (stuff_id) references stuff(id) on delete cascade,
	foreign key (`level`) references permit_levels(`level`) on delete cascade,
	primary key (user_id, stuff_id)
);

drop table if exists formats;
create table formats (
	`key` char(20) character set ascii not null primary key,
	`name` varchar(40)
);
insert into formats(`key`,`name`)
values
	('html','HTML'),
	('htmlx','HTML extendido'),
	('md','Mark Down'),
	('text','Texto plano'),
	('ilm','Interlecto Markup');

drop table if exists blog_categories;
create table blog_categories (
	`key` char(20) character set ascii not null primary key,
	`name` varchar(40)
);

drop table if exists blog_status;
create table blog_status (
	`id` tinyint not null primary key,
	`name` char(20) character set ascii,
	`description` varchar(60)
);
insert into blog_status(id,`name`,description)
values
	(0, 'draft', 'Borrador'),
	(1, 'pending', 'Pendiente de aprovación'),
	(2, 'pending_pub', 'Pendiente de publicación'),
	(3, 'private', 'Publicado privadamente'),
	(4, 'privileged', 'Publicado privilegiadamente'),
	(5, 'public', 'Publicado públicamente'),
	(6, 'un_private', 'Removido tras publicado privadamente'),
	(7, 'un_privileged', 'Removido tras publicado privilegiadamente'),
	(8, 'un_public', 'Removido tras publicado públicamente');

drop table if exists blog_post;
create table blog_post (
	id int not null auto_increment primary key,
	title varchar(120),
	author int not null,
	category char(20) character set ascii,
	format char(20) character set ascii,
	body text,
	published datetime,
	`status` tinyint,
	foreign key (author) references users(id) on delete cascade,
	foreign key (category) references blog_categories(`key`) on delete cascade,
	foreign key (format) references formats(`key`),
	foreign key (`status`) references blog_status(id) on delete cascade
);

drop table if exists blog_history;
create table blog_history (
	post_id int not null,
	`update` datetime,
	diff text,
	diferential boolean,
	editor int,
	primary key (post_id, `update`),
	foreign key (post_id) references blog_post(id) on delete cascade,
	foreign key (editor) references users(id) on delete cascade
);

drop table if exists blog_uri;
create table blog_uri (
	post_id int not null,
	idx smallint not null default 0,
	uri varchar(64) character set ascii,
	primary key(post_id,idx),
	foreign key(post_id) references blog_post(id) on delete cascade
);

drop table if exists tags;
create table tags (
	`key` char(20) character set ascii not null primary key,
	`name` varchar(40)
);

drop table if exists blog_post_tag;
create table blog_post_tag (
	post_id int not null,
	tag char(20) character set ascii not null,
	primary key(post_id,tag),
	foreign key(post_id) references blog_post(id) on delete cascade,
	foreign key(tag) references tags(`key`) on delete cascade
);

drop table if exists news_categories;
create table news_categories (
	`key` char(20) character set ascii not null primary key,
	`name` varchar(40)
);

drop table if exists news_status;
create table news_status (
	`id` tinyint not null primary key,
	`name` char(20) character set ascii,
	`description` varchar(60)
);
insert into news_status(id,`name`,description)
values
	(0, 'draft', 'Borrador'),
	(1, 'pending', 'Pendiente de aprovación'),
	(2, 'pending_pub', 'Pendiente de publicación'),
	(3, 'private', 'Publicado privadamente'),
	(4, 'privileged', 'Publicado privilegiadamente'),
	(5, 'public', 'Publicado públicamente'),
	(6, 'un_private', 'Removido tras publicado privadamente'),
	(7, 'un_privileged', 'Removido tras publicado privilegiadamente'),
	(8, 'un_public', 'Removido tras publicado públicamente');

drop table if exists news_item;
create table news_item (
	id int not null auto_increment primary key,
	title varchar(120),
	author int not null,
	category char(20) character set ascii,
	format char(20) character set ascii,
	body text,
	publish_on datetime,
	publish_off datetime,
	`status` tinyint,
	foreign key (author) references users(id) on delete cascade,
	foreign key (category) references news_categories(`key`) on delete cascade,
	foreign key (format) references formats(`key`),
	foreign key (`status`) references news_status(id) on delete cascade
);

drop table if exists news_history;
create table news_history (
	news_id int not null,
	`update` datetime,
	diff text,
	diferential boolean,
	editor int,
	primary key (news_id, `update`),
	foreign key (news_id) references news_item(id) on delete cascade,
	foreign key (editor) references users(id) on delete cascade
);

drop table if exists news_uri;
create table news_uri (
	news_id int not null,
	idx smallint not null default 0,
	uri varchar(64) character set ascii,
	primary key(news_id,idx),
	foreign key(news_id) references news_item(id) on delete cascade
);

drop table if exists news_item_tag;
create table news_item_tag (
	news_id int not null,
	tag char(20) character set ascii not null,
	primary key(news_id,tag),
	foreign key(news_id) references news_item(id) on delete cascade,
	foreign key(tag) references tags(`key`) on delete cascade
);

