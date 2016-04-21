<?php
/* mod/db/db.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the database module.
 */

interface database {
	public function str($string);
	public function select_fetch($table, $columns='*', $where=null, $orderby=null, $limit=null, $offset=null);
	public function select($table, $columns='*', $where=null, $orderby=null, $limit=250, $offset=0);
	public function select_key($table, $key_columns, $where=null, $orderby=null, $limit=250, $offset=0);
	public function select_pairs($table, $key_col, $val_col, $where=null, $orderby=null, $limit=250, $offset=0);
	public function select_col($table, $col, $where=null, $orderby=null, $limit=250, $offset=0);
	public function select_first($table, $columns='*', $where=null, $orderby=null);
	public function select_one($table, $column, $where=null, $asarray=false);
	public function update($table, $updates, $where=null);
	public function insert($table, $inserts, $columns=null, $ondup=false);
	public function insert_ignore($table, $inserts, $columns=null);
	public function query($query);
};

?>
