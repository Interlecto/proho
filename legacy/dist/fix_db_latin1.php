<?php

$db = new mysqli('localhost', 'root', '0gonBet0', 'proho');
if($db->errno) die('Error ('.$db->errno.'): '.$db->error.chr(10));

function dbesc($str) { global $db; return $db->real_escape_string($str); }
function db_utf8($str) { global $db; return $db->real_escape_string(utf8_encode($str)); }

echo "\nUSERS\n";

$res = $db->query('SELECT * FROM users;');
if($res===false) die('Reading users failed.');
$n = 0;
while($ar = $res->fetch_array(MYSQLI_ASSOC)) {
	$enc = mb_detect_encoding($ar['given_name'],['utf-8','latin1']);
	$id = (int)$ar['id'];
	echo "> $id) $enc\n";
	if($enc=='ISO-8859-1') {
		if(false===($db->query($q="UPDATE users SET given_name='".db_utf8($ar['given_name'])."' WHERE id=$id;")))
			echo ": falla al convertir\n= $q\n";
		else
			$n++;
	}
}
echo "Converted $n entities.\n";

echo "\nBLOG\n";

$res = $db->query('SELECT * FROM blog_post;');
if($res===false) die('Reading blog entries failed.');
$n = 0;
while($ar = $res->fetch_array(MYSQLI_ASSOC)) {
	$enc1 = mb_detect_encoding($ar['title'],['utf-8','latin1']);
	$enc2 = mb_detect_encoding($ar['body'],['utf-8','latin1']);
	$id = (int)$ar['id'];
	echo "> $id) $enc1, $enc2\n";
	$a=[];
	if($enc1=='ISO-8859-1')
		$a[] = "title='".db_utf8($ar['title'])."'";
	if($enc2=='ISO-8859-1')
		$a[] = "body='".db_utf8($ar['body'])."'";
	if(!empty($a)) {
		if(false===($db->query($q="UPDATE blog_post SET ".implode(', ',$a)." WHERE id=$id;")))
			echo ": falla al convertir\n= $q\n";
		else
			$n++;
	}
}
echo "Converted $n entities.\n";

echo "\nNOTICIAS\n";

$res = $db->query('SELECT * FROM news_item;');
if($res===false) die('Reading news items failed.');
$n = 0;
while($ar = $res->fetch_array(MYSQLI_ASSOC)) {
	$enc1 = mb_detect_encoding($ar['title'],['utf-8','latin1']);
	$enc2 = mb_detect_encoding($ar['body'],['utf-8','latin1']);
	$id = (int)$ar['id'];
	echo "> $id) $enc1, $enc2\n";
	$a=[];
	if($enc1=='ISO-8859-1')
		$a[] = "title='".db_utf8($ar['title'])."'";
	if($enc2=='ISO-8859-1')
		$a[] = "body='".db_utf8($ar['body'])."'";
	if(!empty($a)) {
		if(false===($db->query($q="UPDATE news_item SET ".implode(', ',$a)." WHERE id=$id;")))
			echo ": falla al convertir\n= $q\n";
		else
			$n++;
	}
}
echo "Converted $n entities.\n";

$db->close();

?>
