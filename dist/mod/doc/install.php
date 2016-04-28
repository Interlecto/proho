<?php
/* mod/doc/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle documents.
 */

$t = microtime(true);
echo "\nINSTALLING documents.\n";
require_once 'mod/doc/db.php';

(new db_module('doc',1))->save();
$db->create('status',true);

$inserts = [];
$in_obj = [];
$in_i18n = [];
foreach($http_status as $code=>$value) {
	$lab = db_type_label($value,40);
	$inserts[] = [$code,$lab];
	$in_obj[] = [$code,$lab,'status'];
	$in_i18n[] = ['en',$lab,$value];
}
$db->insert('status',$inserts,['code','label']);
$db->insert('i18n',$in_i18n,['lang','label','phrase']);
$db->insert('obj',$in_obj,['oid','label','table']);

echo 'Took '.(microtime(true)-$t)."s\n";
?>
