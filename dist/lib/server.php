<?php
/* lib/server.php
 * @author: Carlos Thompson
 * 
 */

$obj = [0=>[]];
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
if(isset($_SESSION))
	$obj['session'] = $_SESSION;

return $obj;
?>
