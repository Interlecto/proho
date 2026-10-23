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
	
	function create($table, $coldesc, $recreate=false) {
		if($recreate) {
			$this->query("DROP TABLE IF EXISTS $table;");
			$query = "CREATE TABLE $table (\n";
		} else {
			$query = "CREATE TABLE IF NOT EXISTS $table (\n";
		}
		$cols = [];
		foreach($codesc as $column=>$attribs) {
			$s = $column;
			$type = strtoupper($attribs[0]);
			$s.= ' '.$type;
			if(!empty($attribs[1])) {
				$flags = $attribs[1];
				if($flags & DB_PRIMARY)
					$s.= " CONSTRAIN pk_$table PRIMARY KEY".(($flags & DB_AUTO)? ' AUTOINCREMENT': '');
				if($flags & DB_NOTNULL)
					$s.= " CONSTRAIN nn_$table NOT NULL";
				if($flags & DB_UNIQUE)
					$s.= " CONSTRAIN un_$table UNIQUE";
			}
			$cols[] = $s;
		}
		$query.= implode(",\n", $cols);
		$query.= ');';
	}
}

?>
