<?php
/* lib/lib.php
 * @author: Carlos Thompson
 * 
 */

function redirect($where, $type=303) {
	if(preg_match('{^(https?|s?ftp)://}',$where)) {
		header("Location: $where", true, $type);
		die();
	}
	$prot = empty($_SERVER['HTTPS'])? 'http': 'https';
	$host = $_SERVER['HTTP_HOST'];
	$port = $_SERVER['HTTP_PORT'];
	$server = $port==80? "$prot://$host":"$prot://$host:$port";
	if(preg_match('{/.*}', $where)) {
		header("Location: {$server}{$where}", true, $type);
		die();
	}
	$uri = $_SERVER['REQUEST_URI'];
	$urip = explode('/',$uri);
	$last = array_pop($urip);
	$path = ltrim(implode('/',$uri),'/');

	header("Location: {$server}/{$path}/{$where}", true, $type);
	die();
}

function is_public() {
	return empty($_SESSION) || empty($_SESSION['level']);
}


function unbrace($matches) {
	return ph_get($m=str_replace(':','/',$matches[1]),$matches[0]);
}

?>
