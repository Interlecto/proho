<?php
/* mod/sec/install.php
 * @author: Carlos Thompson
 *
 * Sample data for user security module.
 */

if($db->select_count('obj',['label'=>'superuser']))
	return;
require_once 'mod/sec/db.php';

echo "\nLOADING SAMPLES for user security.\n";

$u=[];

$p = db_person::find_label('chlewey');
$u[0] = db_user::upgrade($p);
$u[0]->set_password('uno',0);
$u[0]->set_status('active');

$p = db_person::find_label('luzby');
$u[1] = db_user::upgrade($p);
$u[1]->set_password('dos',1);
$u[1]->set_status('active');

$u[2] = db_user::create('superuser','superuser@proho.local','Super','User');
$u[2]->set_password('tres',2);
$u[2]->set_status('reserved');

foreach($u as $user)
	$user->save();

?>
