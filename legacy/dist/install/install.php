<?php
/* install/index.php
 * @author: Carlos Thompson
 *
 * Install database and other entries.
 */

// Development mode, shall be removed for production
ini_set('display_errors', true);
error_reporting(E_ALL);
$tt = microtime(true);

header('Content-type: text/plain; charset=utf-8');
set_include_path(get_include_path().PATH_SEPARATOR.'..');
if(file_exists($fn='config/site.php'))
	require $fn;
require 'lib/server.php';

function ph_add(...$n) {}

require_once 'mod/db/reg.php';
function _explode(&$val,$idx) { $val = explode(':',$val); }

//var_dump($obj);
$db = db_open();
db_obj::set_db($db);

$dirs = ['db','doc','pro'];
$dirf = scandir('mod');
foreach($dirf as $d)
	if(!in_array($d,$dirs)) $dirs[]=$d;

if(!isset($_GET['noinstall'])) {
	foreach($dirs as $d) {
		if(substr($d,0,1)=='.') continue;
		if(file_exists($fn="mod/$d/install.php"))
			require_once $fn;
	}
}

if(isset($_GET['sample'])) {
	echo "\n\nPopulating with sample data:\n\n";
	foreach($dirs as $d) {
		if(substr($d,0,1)=='.') continue;
		if(file_exists($fn="mod/$d/sample.php"))
			require_once $fn;
	}
}

#print_r($GLOBALS);
db_close();
echo "The End!\n";
echo 'Took '.(microtime(true)-$tt)."s\n";
?>
