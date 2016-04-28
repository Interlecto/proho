<?php
/* mod/pub/install.php
 * @author: Carlos Thompson
 *
 * Creates sample data for publication module.
 */

echo "LOADING SAMPLES for publications.\n";
require_once 'mod/pub/db.php';

$blogposts = [
	[
		'title'=>'La importancia del verde',
		'author'=>'jairo',
		'body'=>'Dentro de la nueva onda ecológica...',
		'tags'=>['ecologia','ciudad'],
	],
	[
		'title'=>'Un año de logros',
		'author'=>'admon',
		'body'=>"Señores copropietarios:\nMe coplace informarles que...",
		'cat'=>'informe',
	],
	[
		'title'=>'Los cerramientos y el modelo de ciudad',
		'author'=>'esteban',
		'body'=>'Hemos venido viendo cómo la ciudad se ha venido...',
		'tags'=>['sociedad','ciudad'],
	],
];

foreach($blogposts as $blogpost) {
	$label = db_type_label($blogpost['title'],40);
	echo " > $label\n";
	if($db->select_count('obj',['label'=>$label]))
		continue;
	$db->insert_ignore('i18n',['lang'=>'es','label'=>$label,'phrase'=>$blogpost['title']]);
	
	$author = db_person::find_label($blogpost['author']);
	$bs = db_blog_post::create($label);
	$bs->set('lang','es');
	$bs->set('author',$author->id());
	$bs->set('body',$blogpost['body']);
	$bs->set('status',2);
	$bs->set('scope',2);
	$bs->set('priority',1);
	$bs->save();
	
	$pid = $bs->id();
	$updates = [];
	if(isset($blogpost['tags'])) {
		foreach($blogpost['tags'] as $tag)
			$updates[] = [$pid,$tag];
		$db->insert('pub_tag',$updates,['pid','label']);
	}
	if(isset($blogpost['cat']))
		$db->insert('pub_cat',['pid'=>$pid,'label'=>$blogpost['cat']]);
}

$newsitems = [
	[
		'title'=>'Cuota extaordinaria',
		'author'=>'admon',
		'body'=>"La asamblea aprobó una cuota extraordinaria $20.000.000, repartida por el coeficiente de copropiedad, para el arreglo de zonas comunes.",
		'tags'=>['plata'],
	],
	[
		'title'=>'Reglamento sobre mascotas',
		'author'=>'admon',
		'body'=>"El Concejo Distrital aprobó una nueva norma sobre mascotas en propiedad horizontal.",
		'tags'=>['mascotas'],
	],
	[
		'title'=>'Nuevo revisor fiscal',
		'author'=>'admon',
		'body'=>"De conformidad con lo acordado por la asamblea, el consejo de administracipon ha contratado al señor Pedro Pérez como nuevo revisor fiscal",
		'tags'=>['plata'],
	],
];

foreach($newsitems as $newsitem) {
	$label = db_type_label($newsitem['title'],40);
	echo " > $label\n";
	if($db->select_count('obj',['label'=>$label]))
		continue;
	$db->insert_ignore('i18n',['lang'=>'es','label'=>$label,'phrase'=>$newsitem['title']]);

	$author = db_person::find_label($newsitem['author']);
	$ni = db_news_item::create($label);
	$ni->set('lang','es');
	$ni->set('author',$author->id());
	$ni->set('body',$newsitem['body']);
	$ni->set('status',2);
	$ni->set('scope',2);
	$ni->set('priority',1);
	$ni->save();
	
	$pid = $ni->id();
	$updates = [];
	if(isset($newsitem['tags'])) {
		foreach($newsitem['tags'] as $tag)
			$updates[] = [$pid,$tag];
		$db->insert('pub_tag',$updates,['pid','label']);
	}
	if(isset($newsitem['cat']))
		$db->insert('pub_cat',['pid'=>$pid,'label'=>$newsitem['cat']]);
}


?>
