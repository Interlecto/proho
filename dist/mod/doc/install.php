<?php
/* mod/doc/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle documents.
 */

echo "INSTALLING documents.\n";
require_once 'mod/doc/db.php';

$db->create('status',true);

$updates = [];
$up_i18n = [];
foreach($http_status as $code=>$value) {
	$lab = db_type_label($value,40);
	$updates[] = [$code,$lab];
	$up_i18n[] = ['en',$lab,$value];
}
$db->insert('status',$updates,['code','label']);
$db->insert('i18n',$up_i18n,['lang','label','phrase']);


?>
