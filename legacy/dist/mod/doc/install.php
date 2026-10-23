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

$db->create('doc_scope',true);
$db->create('doc_format',true);
$db->create('doc_ns',true);
$db->create('document',true);
$db->create('doc_tag',true);

$formats = "0:plain_text;1:markdown;2:html;3:html+braces;4:ilm;5:mediawiki;6:wordpress";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('doc_format',$updates,['key','label']);

$scope = "0:private;1:protected;2:public";
$updates = explode(';',$scope);
array_walk($updates,'_explode');
$db->insert('doc_scope',$updates,['key','label']);

(db_namespace::create('info',2))->save();
(db_namespace::create('help',2))->save();

$help = db_document::create('help_index','help','es');
$help->set('body','Se presenta ayuda sobre el uso de distintas partes de este sitio web.');
$help->save();
db_obj::set_label('help_index','Ayuda','es');

echo 'Took '.(microtime(true)-$t)."s\n";
?>
