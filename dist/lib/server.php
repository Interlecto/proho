<?php
/* lib/server.php
 * @author: Carlos Thompson
 *
 */

if(!isset($_SESSION)) session_start();
$ph_uri_case = [['{.*}',0,'mod/doc/status.php','404']];

$obj = [
	0=>[],
	'site'=>[
		'name' => 'ProHo',
		],
	'db'=>[
		'engine' => 'mysql',
		'server' => 'localhost',
		'username' => 'root',
		'password' => '',
		'database' => 'proho',
		'prefix' => '',
		'filename' => 'data/proho.db',
		],
	];
foreach($GLOBALS as $k=>$v) {
	if(in_array($k,['ph_uri_case'])) continue;
	$kk = explode('_',$k);
	if(count($kk)<2 || $kk[0] != 'ph') continue;
	array_shift($kk);
	$kp = array_shift($kk);
	if(empty($kk))
		$obj[0][$kp] = $v;
	else
		$obj[$kp][implode(':',$kk)] = $v;
}

$prefixes = ['HTTP','SERVER','REQUEST','SCRIPT','PATH','REDIRECT'];
foreach($prefixes as $p) {
	$obj[strtolower($p)] = [];
}
foreach($_SERVER as $key=>$value) {
	$r = explode('_',$key);
	if(in_array($r[0],$prefixes)) {
		$ns = array_shift($r);
		$kr = implode(':',$r);
		$obj[strtolower($ns)][strtolower($kr)] = $value;
	} else {
		$obj[0][strtolower($key)] = $value;
	}
}
$obj['get'] = $_GET;
$obj['post'] = $_POST;
if(isset($_COOKIES))
	$obj['cookies'] = $_COOKIES;
if(isset($_SESSION)) {
	foreach(['warning','danger','alert','error'] as $alert) {
		if(isset($_SESSION[$alert])) {
			if(!isset($obj['alert']))
				$obj['alert'] = [];
			$obj['alert'][$alert] = $_SESSION[$alert];
			unset($_SESSION[$alert]);
		}
	}
	$obj['session'] = $_SESSION;
}

$line = isset($_GET['line'])? $_GET['line']:
	(isset($obj['redirect']['url'])? ltrim($obj['redirect']['url'],'/'):
		(isset($obj['request']['uri'])? ltrim($obj['request']['uri'],'/'):
			''));

$script = isset($obj['script']['name'])? ltrim($obj['script']['name'],'/'):
		(isset($obj[0]['php_self'])? ltrim($obj[0]['php_self'],'/'):
			'');

$obj['line'] = [
	'script' => $script,
	'line' => $line,
];

$obj['dir'] = [
	'root' => $obj[0]['document_root'],
];
$mods=(rtrim($obj['dir']['root'],'/').'/mod');
if($script=='api.php') {
	if(!preg_match('{(\w+)\b([^?]*)(\?|$)}',$line,$m)) {
		$obj[] = 'No Match';
		return $obj;
	}
	if(count($m)<3) {
		$obj[] = $m;
		return $obj;
	}
	$mod = $m[1];
	if(file_exists("$mods/$mod/api.php"))
		$obj['line']['module'] = "mod/$mod/api.php";
	elseif(file_exists("$mods/$mod/$mod.php"))
		$obj['line']['module'] = "mod/$mod/$mod.php";
	else
		$obj['line']['module'] = "";
	$obj['line']['query'] = $m[2] or "";
} elseif($script=='index.php') {
	require_once($mods.'/db/reg.php');
	$d = dir($mods);
	while(false!==($entry = $d->read())) {
		if(substr($entry,0,1)=='.') continue;
		$obj['dir'][$entry] = "$mods/$entry/";
		if($entry=='db') continue;
		if(file_exists($fn="$mods/$entry/reg.php"))
			require_once $fn;
	}
	$d->close();

	$level = -1;
	foreach($ph_uri_case as $rec) {
		if($rec[1]<=$level) continue;
		if(preg_match($rec[0], $line, $m)) {
			$level = $rec[1];
			$obj['line']['module'] = $rec[2];
			$obj['line']['class'] = $rec[3];
			$obj['line']['parts'] = $m;
			$obj['line']['case'] = $rec[0];
		}
	}
}

return $obj;
?>
