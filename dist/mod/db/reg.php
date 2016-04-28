<?php
/* mod/db/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for database module.
 */

switch($obj['db']['engine']) {
case 'mysql':
	require_once 'mod/db/mysql.php';

	function db_open() {
		global $obj;
		$db = $obj['db']['handler'] = new db_mysql(
			$obj['db']['server'],
			$obj['db']['username'],
			$obj['db']['password'],
			$obj['db']['database'],
			$obj['db']['prefix']
		);
		db_obj::set_db($db);
		return $db;
	}

	function db_close() {
		global $obj;
		return $obj['db']['handler']->close();
	}

	break;
case 'sqlite':
default:
	require_once 'mod/db/sqlite.php';

	function db_open() {
		global $obj;
		$db = $obj['db']['handler'] = new db_sqlite(
			$obj['db']['filename']
		);
		db_obj::set_db($db);
		return $db;
	}

	function db_close() {
		global $obj;
		return $obj['db']['handler']->close();
	}

	break;
}

?>
