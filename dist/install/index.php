<?php
/* install/index.php
 * @author: Carlos Thompson
 *
 * Install database and other entries.
 */

// Development mode, shall be removed for production 
ini_set('display_errors', true);
error_reporting(E_ALL);

header('Content-type: text/plain; charset=utf-8');
set_include_path(get_include_path().PATH_SEPARATOR.'..');
if(file_exists($fn='../config/site.php'))
	require $fn;
$obj=[];

define('SET_REPLACE', 0);
define('SET_UNSET', 1);
define('SET_EMPTY', 2);
function array_set(array &$array, $key, $value, $how = SET_REPLACE) {
	if(empty($array)) return $array = [$key=>$value];
	if(!isset($array[$key])) return $array[$key] = $value;
	if(empty($array[$key]) && $how==SET_EMPTY) return $array[$key] = $value;
	if($how == SET_REPLACE) return $array[$key] = $value;
	return $array;
}
function oooo () {
	global $obj;
	foreach($GLOBALS as $var=>$val) {
		if(substr($var,0,3)=='ph_') {
			$kk = explode('_',$var);
			array_shift($kk);
			$key = array_shift($kk);
			$subkey = implode(':',$kk);
			if(!isset($obj[$key]))
				$obj[$key] = [];
			$obj[$key][$subkey] = $val;
		}
	}
	array_set($obj['db'],'filename','../data/proho.db',SET_EMPTY);
} oooo ();


require_once '../mod/db/reg.php';
$db = db_open();
foreach(scandir('../mod') as $d) {
	if(substr($d,0,1)=='.') continue;
	if(file_exists($fn="../mod/$d/install.php"))
		include $fn;
}
#print_r($GLOBALS);
db_close();
?>
