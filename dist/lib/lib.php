<?php
/* lib/lib.php
 * @author: Carlos Thompson
 *
 */

function redirect($where, $type=303, $message='') {
	#die($message);
	#echo $message; return;
	if(preg_match('{^(https?|s?ftp)://}',$where)) {
		header("Location: $where", true, $type);
		die($message);
	}
	$prot = empty($_SERVER['HTTPS'])? 'http': 'https';
	$host = $_SERVER['HTTP_HOST'];
	$port = $_SERVER['HTTP_PORT'];
	$server = $port==80? "$prot://$host":"$prot://$host:$port";
	if(preg_match('{/.*}', $where)) {
		header("Location: {$server}{$where}", true, $type);
		die($message);
	}
	$uri = $_SERVER['REQUEST_URI'];
	$urip = explode('/',$uri);
	$last = array_pop($urip);
	$path = ltrim(implode('/',$uri),'/');

	header("Location: {$server}/{$path}/{$where}", true, $type);
	die($message);
}

function preg_redirect($pattern, $transform, $line=null, $type=303) {
	if(is_null($line)) $line = $obj['line']['line'];
	lensure($line);
	$target = preg_replace($pattern, $trasnform, $line);
	lensure($target);
	if($target==$line) return;
	redirect($target, $type, "<p>Redirige de <code>$line</code> as <a href\"$target\"><code>$target</code></a></p>\n");
}

function check_redirect($target, $line=null, $type=303) {
	global $obj;
	if(is_null($line)) $line = $obj['line']['line'];
	lensure($line);
	lensure($target);
	if($target==$line) return;
	redirect($target, $type, "<p>Redirige de <code>$line</code> as <a href\"$target\"><code>$target</code></a></p>\n");
}

function is_public() {
	return empty($_SESSION) || empty($_SESSION['level']);
}

function ltensure($str, $c='/', $al=null) {
	if(empty($al)) $al = $c;
	return $c.ltrim($str,$c);
}

function rtensure($str, $c='/', $al=null) {
	if(empty($al)) $al = $c;
	return rtrim($str,$c).$c;
}

function lensure(&$str, $c='/', $al=null) {
	if(empty($al)) $al = $c;
	return $str = $c.ltrim($str,$c);
}

function rensure(&$str, $c='/', $al=null) {
	if(empty($al)) $al = $c;
	return $str = rtrim($str,$c).$c;
}

function temp2html($template,$version=5) {
	$html = preg_replace_callback('#{((\w+):?(!?)([^}]*))}#','unbrace',$template);
	while(strpos($html,chr(2))!==false) {
		$html2 = preg_replace('{\x02[^\x02\x03]+\x03}', '', $html);
		if($html2==$html) {
			break;
		}
		$html = $html2;
	}
	return $html;
}

$unbrace_stack = [];
function unbrace_if($val) {
	global $unbrace_stack;
	array_push($unbrace_stack,$val);
	return $val? '': chr(2);
}
function unbrace_else() {
	global $unbrace_stack;
	$val = array_pop($unbrace_stack);
	array_push($unbrace_stack,!$val);
	return $val? chr(2): chr(3);
}
function unbrace_fi() {
	global $unbrace_stack;
	$val = array_pop($unbrace_stack);
	return $val? '': chr(3);
}

function unbrace($matches) {
	$s='';
	switch($matches[2]) {
	case 'elif':
		$s = unbrace_else();
	case 'if':
		if(count($matches)<5)
			return $s.unbrace_if(false);
		$v = str_replace(':','/',$matches[4]);
		$neg = $matches[3]=='!';
		if(ph_empty($v))
			return $s.unbrace_if($neg);
		else
			return $s.unbrace_if(!$neg);
	case 'else':
		return unbrace_else();
	case 'fi':
		if(count($matches)<5)
			return unbrace_fi();
		$n = (int)$matches[4];
		if($n<1) $n=1;
		while($n--)
			$s.= unbrace_fi();
		return $s;
	case 'rem':
		return chr(2);
	case 'mer':
		return chr(3);
	default:
		return ph_get($m=str_replace(':','/',$matches[1]),$matches[0]);
	}
	return $matches[0];
}

?>
