<?php
/* mod/db/db.php
 * @author: Carlos Thompson
 *
 * Main implementation of the database module.
 */

define('DB_CLEAR', 0);
define('DB_PRIMARY', 1);
define('DB_UNIQUE', 2);
define('DB_AUTO', 4);
define('DB_CURRENT', 8);
define('DB_DESC', 16);
define('DB_NOTNULL', 32);
define('DB_DEFNULL', 64);
define('DB_AUTO_KEY', DB_PRIMARY|DB_AUTO|DB_NOTNULL);
define('DB_PRIM_KEY', DB_PRIMARY|DB_NOTNULL);
define('DB_UNIKEY', DB_UNIQUE|DB_NOTNULL);

interface database {
	public function str($string);
	public function select($table, $columns='*', $where=null, $orderby=null, ...$extra);
	public function select_key($table, $key_columns, $where=null, $orderby=null, ...$extra);
	public function select_pairs($table, $key_col, $val_col, $where=null, $orderby=null, ...$extra);
	public function select_col($table, $col, $where=null, $orderby=null, ...$extra);
	public function select_first($table, $columns='*', $where=null, $orderby=null);
	public function select_one($table, $column, $where=null, $asarray=false);
	public function select_count($table, $where=null);
	public function update($table, $updates, $where=null);
	public function insert($table, $inserts, $columns=null, $ondup=false);
	public function insert_ignore($table, $inserts, $columns=null);
	public function delete($table, $where, ...$extra);
	public function query($query);
	public function log($query,$errno=null,$error=null);
	public function create($table, $recreate=false);
};

require_once "mod/db/db_type.php";
require_once "mod/db/db_obj.php";

?>
