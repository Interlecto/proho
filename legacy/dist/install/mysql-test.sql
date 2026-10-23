
-- 
-- $blogs = [
-- 	1=>[
-- 		'title'=>'La importancia del verde',
-- 		'body'=>'Aun cuando la política del edificio...',
-- 		'author'=>'Jairo, del 509',
-- 		'author-tag'=>'jairo',
-- 	],
-- 	2=>[
-- 		'title'=>'Un año de logros',
-- 		'body'=>'Señores copropietarios. Les presento...',
-- 		'author'=>'Administración',
-- 		'author-tag'=>'admin',
-- 	],
-- 	3=>[
-- 		'title'=>'Los cerramientos y el modelo de ciudad',
-- 		'body'=>'En los últimos años hemos venido viendo cómo...',
-- 		'author'=>'Esteban, del 105',
-- 		'author-tag'=>'esteban',
-- 	],
-- ];

insert into users(id, username, given_name)
values
	(2, 'admin', 'Administración'),
	(15, 'jairo', 'Jairo'),
	(16, 'esteban', 'Esteban'),
	(17, 'andrea', 'Andrea'),
	(18, 'carlos', 'Carlos'),
	(19, 'claudia', 'Claudia'),
	(20, 'sofia', 'Sofía');

insert into blog_post(title,body,author,`status`)
values
	('La importancia del verde','Aun cuando la política del edificio...',15,4),
	('Un año de logros','Señores copropietarios. Les presento...',2,4),
	('Los cerramientos y el modelo de ciudad','En los últimos años hemos venido viendo cómo...',16,5);

-- $noticias = [
-- 	1=>[
-- 		'title'=>'Cuota extaordinaria',
-- 		'body'=>'La asamblea aprobó una cuota extraordinaria $20.000.000, repartida por el coeficiente de copropiedad, para el arreglo de zonas comunes.',
-- 		'tags'=>['plata', 'cuota'],
-- 	],
-- 	2=>[
-- 		'title'=>'Reglamento sobre mascotas',
-- 		'body'=>'El Concejo Distrital aprobó una nueva norma sobre mascotas en propiedad horizontal.',
-- 		'tags'=>['mascotas'],
-- 	],
-- 	3=>[
-- 		'title'=>'Nuevo revisor fiscal',
-- 		'body'=>'En asamblea se nombró a la señora Pepa Pérez como nueva revisora fiscal.',
-- 		'tags'=>['nombramientos','plata'],
-- 	],
-- ];

insert into news_item(id,title,body,author,`status`)
values
	(51,'Cuota extaordinaria', 'La asamblea aprobó una cuota extraordinaria $20.000.000, repartida por el coeficiente de copropiedad, para el arreglo de zonas comunes.', 2,4),
	(47,'Reglamento sobre mascotas', 'El Concejo Distrital aprobó una nueva norma sobre mascotas en propiedad horizontal.', 2,5),
	(43,'Nuevo revisor fiscal', 'En asamblea se nombró a la señora Pepa Pérez como nueva revisora fiscal.', 2,4);

insert into tags(`key`, `name`)
values
	('plata', 'Plata'),
	('cuota', 'Cuota'),
	('mascotas', 'Mascotas'),
	('nombramientos', 'Nombramientos');

insert into news_item_tag(news_id, tag)
values
	(51,'plata'),
	(51,'cuota'),
	(47,'mascotas'),
	(43,'nombramientos'),
	(43,'plata');
