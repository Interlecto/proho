<?php
/* mod/db/sqlite.php
 * @author: Carlos Thompson
 * 
 * Main implementation for SQLite databases.
 */

require_once 'mod/db/db.php';
class db_sqlite extends SQLite3 implements database {
	public function __construct($filename, $flags=SQLITE3_OPEN_READWRITE|SQLITE3_OPEN_CREATE) {
		SQLite3::__construct( $filename, $flags );
	}
	
	public function str($string) {
	}

	public function select_fetch($table, $columns='*', $where=null, $orderby=null, $limit=null, $offset=null) {
	}

	public function select($table, $columns='*', $where=null, $orderby=null, $limit=250, $offset=0) {
	}

	public function select_key($table, $key_columns, $where=null, $orderby=null, $limit=250, $offset=0) {
	}

	public function select_pairs($table, $key_col, $val_col, $where=null, $orderby=null, $limit=250, $offset=0) {
	}

	public function select_col($table, $col, $where=null, $orderby=null, $limit=250, $offset=0) {
	}

	public function select_first($table, $columns='*', $where=null, $orderby=null) {
	}

	public function select_one($table, $column, $where=null, $asarray=false) {
	}

	public function update($table, $updates, $where=null) {
	}

	public function insert($table, $inserts, $columns=null, $ondup=false) {
	}

	public function insert_ignore($table, $inserts, $columns=null) {
	}

	public function query($query) {
		return SQLite3::query($query);
	}
}

?>
