<?php
/* mod/pro/install.php
 * @author: Carlos Thompson
 *
 * Creates sample data for user profiles module.
 */

if($db->select_count('obj',['label'=>'chlewey']))
	return;

echo "LOADING SAMPLES for user profiles.\n";
require_once 'mod/pro/db.php';

$users = [
	db_person::create('chlewey','chlewey@gmail.com','Carlos Eugenio','Thompson Pinzón'),
	db_person::create('luzby','luzby.b@gmail.com','Luz Beatriz','Baquero Cerón'),
	db_person::create('jairo',null,'Jairo','del 509'),
	db_person::create('admon',null,'Administración'),
	db_person::create('esteban',null,'Esteban','del 105'),
	db_person::create('andrea',null,'Andrea','del 702'),
	db_person::create('carlos',null,'Carlos','del 307'),
	db_person::create('claudia',null,'Claudia','del 212'),
	db_person::create('sofia',null,'Sofía','del 408'),
	];

foreach($users as $user)
	$user->save();

?>

