<?php
/* mod/db/install.php
 * @author: Carlos Thompson
 *
 * Creates environment to handle database.
 */

echo "INSTALLING database.\n";
require_once 'mod/db/db_obj.php';

$db->query('DROP DATABASE IF EXISTS '.db_var($obj['db']['database']).';');
$db->query('CREATE DATABASE '.db_var($obj['db']['database']).' COLLATE utf8_spanish_ci;');
$db->query('USE '.db_var($obj['db']['database']).';');

$db->create('log',true);

$db->create('obj',true);
$db->insert('obj',['oid'=>0, 'label'=>'root']);

$db->create('lang',true);
$langs = [
	'en'=>['english', 'en'=>'English', 'es'=>'inglés'],
	'es'=>['espanol', 'en'=>'Spanish', 'es'=>'español'],
	'fr'=>['francais', 'en'=>'French', 'es'=>'francés', 'fr'=>'français'],
	'sv'=>['svenska', 'en'=>'Swedish', 'es'=>'sueco', 'sv'=>'svenska'],
	];

if(isset($ph_site_lang) && !isset($langs[$ph_site_lang]))
	$langs[$ph_site_lang] = [$ph_site_lang];
$updates = [];
foreach($langs as $code=>$names)
	$updates[] = [$code, $names[0]];
$db->insert('lang',$updates,['code','label']);

$db->create('i18n',true);
$updates = [
	['en', 'root', 'root'],
	['es', 'root', 'raíz'],
	['fr', 'root', 'racine'],
	['sv', 'root', 'rot'],
];
foreach($langs as $code=>$names)
	foreach($names as $lang=>$name)
		if($lang)
			$updates[] = [$lang, $names[0], $name];
$db->insert('i18n',$updates,['lang','label','phrase']);

?>
