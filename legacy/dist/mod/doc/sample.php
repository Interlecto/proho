<?php
/* mod/pub/install.php
 * @author: Carlos Thompson
 *
 * Creates sample data for publication module.
 */

echo "LOADING SAMPLES for publications.\n";
require_once 'mod/pub/db.php';

$proho = db_document::create('info_proho','info','es');
$proho->set('body','Se muestra información general sobre ProHo');
$proho->save();
db_obj::set_label('info_proho','Sobre ProHo','es');

?>
