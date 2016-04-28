<?php
/* mod/db/mysql.php
 * @author: Carlos Thompson
 *
 * Main implementation for MySQL/MariaSQL databases.
 */

function db_str($string) { return mysqli_real_escape_string(db_mysql::$first,$string); }
function db__str(&$string,$idx=null) { return $string = db_str($string); }
function db_val($value) { return is_null($value)? 'NULL': (is_numeric($value)? (float)$value: (substr($value,0,2)=='0x'? "x'".db_str(substr($value,2))."'":"'".db_str($value)."'")); }
function db__val(&$value,$idx=null) { return $value = db_val($value); }
function db_var($key) { return "`".db_str($key)."`"; }
function db__var(&$key,$idx=null) { return $key = db_var($key); }
function db_varval($string) { return preg_match('{^[_A-Za-z]\w*$}',$string)? db_var($string): db_val($string); }
function db__varval(&$string,$idx=null) { return $string = db_varval($string); }

require_once 'mod/db/db.php';
class db_mysql extends mysqli implements database {
	static $first;

	private function fix_columns($columns) {
		if(empty($columns)) return '*';
		if($columns=='*') return $columns;
		if(is_array($columns)) {
			array_walk($columns,'db__varval');
			return implode(',',$columns);
		}
		return db_varval($columns);
	}
	private function fix_where($where) {
		if(empty($where)) return '';
		if(is_array($where)) {
			$array = array();
			foreach($where as $key=>$act) {
				db__var($key);
				if(is_array($act)) {
					array_walk($act,'db__val');
					$valstr = implode(',',$act);
					$array[] =  "$key IN ($valstr)";
					continue;
				}
				preg_match('{^([[(=<>!#&])?([^&)]*)&?(.*)}',$act,$m);
				switch($c=$m[1]) {
				case '&':
					$i0 = db_val($m[2]);
					$i1 = db_val($m[3]);
					$array[] = "$key>=$i0 AND $key<$i1";
					break;
				case '#':
					$array[] = "$key IS NULL";
					break;
				case '!':
					$array[] = $m[2]=='#'? "$key IS NOT NULL": "$key <> ".db_val($m[2]);
					break;
				case '(':
					$values = explode(',',$m[2]);
					array_walk($values,'db__val');
					$valstr = implode(',',$values);
					$array[] =  "$key IN ($valstr)";
					break;
				case '[':
					$s = rtrim($m[2],']');
					$l = strlen($s);
					$array[] = "LEFT($key,$l) = ".db_val($s);
					break;
				case '':
					$c='=';
				default:
					$val = db_val($m[2]);
					$array[] = "$key $c $val";
				}
			}
			return " WHERE ".(count($array)?implode(' AND ',$array):1);
		}
		if($where===false) return " WHERE 0";
		if(is_numeric($where)) return " WHERE ".(int)$where;
		if(is_string($where)) return " WHERE ".(preg_match('{^[_A-Za-z]\w*$}',$where)?db_var($where):$where);
		return '';
	}
	private function fix_orderby($orderby) {
		if(empty($orderby)) return '';
		if(is_string($orderby)) {
			if(substr($orderby,0,1)=='-') return " ORDER BY ".db_varval(substr($orderby,1))." DESC";
			return " ORDER BY ".db_varval($orderby);
		}
		if(is_array($orderby)) {
			$array = array();
			foreach($orderby as $key)
				$array[] = substr($key,0,1)=='-'? db_varval(substr($key,1)).' DESC': db_varval($key);
			if(count($array))
				return " ORDER BY ".implode(',',$array);
		}
		return '';
	}
	private function fix_limits($offset,$limit) {
		if((int)$limit) return ' LIMIT '.((int)$offset).','.((int)$limit);
		return '';
	}
	private function fix_updates($updates) {
		$pairs=array();
		foreach($updates as $key=>$value)
			$pairs[] = db_var($key).'='.db_val($value);
		return implode(', ',$pairs);
	}
	private function fix_inserts($inserts,&$columns) {
		if(empty($inserts)) return '';
		if(!is_array($inserts)) $inserts = array("$inserts");

		if(empty($columns)) $columns = array();
		if(!is_array($columns)) $columns = array("$columns");
		$n = count($columns);

		if(isset($inserts[0]) && is_array($inserts[0])) {
			#multiple row insertion:
			$this->fix_inserts_columns($inserts[0],$columns);
			return $this->fix_inserts_multiple($inserts,$columns);
		}
		#single row insertion
		$this->fix_inserts_columns($inserts,$columns);
		return $this->fix_inserts_single($inserts,$columns);
	}
	private function fix_unneet_inserts(&$irow,&$columns) {
		$krow = array_keys($irow);
		$vrow = array_values($irow);
		$c = count($columns);
		$k = count($krow);
		$redo = false;
		for($j=0;$j<$c && $j<$k;$j++) {
			if("{$krow[$j]}"=="{$columns[$j]}") continue;
			if("{$columns[$j]}"=="$j") $columns[$j]=$krow[$j];
			elseif("{$krow[$j]}"=="$j") {
				$krow[$j]=$columns[$j];
				$redo = true;
			}
		} // numeric columns are now converted into names if names exists;
		if($redo) $irow = array_combine($krow,$vrow);
		// now add into $columns any key that is not already there;
		foreach($krow as $k) if(!in_array($k,$columns)) $columns[] = $k;
		$c = count($columns);

		$nuevo = array();
		foreach($columns as $i) $nuevo[$i] = isset($irow[$i])? $irow[$i]: null;
		if($nuevo!=$irow || $columns!=$krow) $irow=$nuevo;
	}
	private function fix_inserts_single($irow,&$columns) {
		$vrow = array_values($irow);
		if(count($columns)!=count($vrow)) {
			$this->fix_unneet_inserts($irow,$columns);
			return $this->fix_inserts_single($irow,$columns);
		}
		array_walk($vrow,'db__val');
		return ' ('.implode(',',$vrow).')';
	}
	private function fix_inserts_multiple($inserts,&$columns) {
		//print_r(array('i'=>$inserts,'c'=>$columns));
		$c = count($columns);
		$rows = array();
		$neet = true;
		foreach($inserts as $irow) {
			$vrow = array_values($irow);
			if($c!=count($vrow)) {
				$neet = false;
				break;
			}
			array_walk($vrow,'db__val');
			$rows[] = '('.implode(',',$vrow).')';
		}
		if($neet) return chr(10).implode(",\n",$rows);
		for($i=0;$i<count($inserts);$i++) {
			$this->fix_unneet_inserts($inserts[$i],$columns);
		}
		return $this->fix_inserts_multiple($inserts,$columns);
	}
	private function fix_inserts_columns($irow,&$columns) {
		$krow = array_keys($irow);
		$c = count($krow);
		if($c<count($columns)) return false;
		for($j=0;$j<$c;$j++) {
			if(!isset($columns[$j])) { $columns[$j]=$krow[$j]; continue; }
			if($krow[$j]==$j) continue;
			if($columns[$j]!=$krow[$j]) return false;
		}
		return true;
	}

	function __construct($host,$username,$passwd,$database=null,$prefix='') {
		if(!isset(db_mysql::$first)) db_mysql::$first = $this;
		$this->prefix = $prefix;
		@mysqli::__construct($host,$username,$passwd);
		if($this->connect_errno)
			ph_add('queries','Connect Error: '.$this->connect_error);
		if($database) {
			if(!@$this->select_db($database)) {
				if(trim($_SERVER['SCRIPT_NAME'],'/')=='install.php')
					return;
				if(file_exists($fn=rtensure($GLOBALS['obj']['dir']['root']).'install/index.php'))
					redirect('/install/index.php', 303, "Database $database does not exist.");
				else
					die("Installation path <code>install/index.php</code> ($fn) not found.");
			}
		}
	}

	function str($string) { return $this->real_escape_string($string); }

	function select_fetch($table,$columns='*',$where=null,$orderby=null,...$extra) {
		$table = db_var($this->prefix.$table);
		$limit = isset($extra[0])? $extra[0]: null;
		$offset = isset($extra[1])? $extra[1]: null;

		$query = 'SELECT '.($this->fix_columns($columns))." FROM $table".
			($this->fix_where($where)).($this->fix_orderby($orderby)).
			($this->fix_limits($offset,$limit)).";\n";
		return $this->query($query);
	}

	function select($table,$columns='*',$where=null,$orderby=null,...$extra) {
		if(!isset($extra[0])) $extra[0] = 250;
		if(!isset($extra[1])) $extra[1] = 0;
		$result = $this->select_fetch($table,$columns,$where,$orderby,...$extra);
		if($result===false) return false;
		$array = $result->fetch_all(MYSQLI_ASSOC);
		$result->free();
		return $array;
	}

	function select_key($table,$key_columns,$where=null,$orderby=null,...$extra) {
		if(!isset($extra[0])) $extra[0] = 250;
		if(!isset($extra[1])) $extra[1] = 0;

		if(is_array($key_columns)) {
			$key = $key_columns[0];
			$cols = $key_columns;
		} else {
			$key = "$key_columns";
			$cols = '*';
		}
		$result = $this->select_fetch($table, $cols, $where, $orderby, ...$extra);

		if($result===false) return false;
		$array = array();
		while($ar = $result->fetch_array(MYSQLI_ASSOC))
			$array[$ar[$key]] = $ar;
		$result->free();
		return $array;
	}

	function select_pairs($table,$key_col,$val_col,$where=null,$orderby=null,...$extra) {
		if(!isset($extra[0])) $extra[0] = 250;
		if(!isset($extra[1])) $extra[1] = 0;

		$result = $this->select_fetch($table, [$key_col, $val_col], $where, $orderby, ...$extra);
		if($result===false) return false;
		$array = array();
		while($ar = $result->fetch_array(MYSQLI_NUM))
			$array[$ar[0]] = $ar[1];
		$result->free();
		return $array;
	}

	function select_col($table,$col,$where=null,$orderby=null,...$extra) {
		if(!isset($extra[0])) $extra[0] = 250;
		if(!isset($extra[1])) $extra[1] = 0;

		$result = $this->select_fetch($table, [$col], $where, $orderby, ...$extra);
		if($result===false) return false;
		$array = array();
		while($ar = $result->fetch_array(MYSQLI_NUM))
			$array[] = $ar[0];
		$result->free();
		return $array;
	}

	function select_first($table,$columns='*',$where=null,$orderby=null) {
		$result = $this->select($table,$columns,$where,$orderby,1);
		return $result? $result[0]: false;
	}

	function select_one($table,$column,$where=null,$asarray=false) {
		$result = $this->select_fetch($table,[$column],$where,null,1);
		if($result===false) return false;
		if($asarray===false) {
			$array = $result->fetch_array(MYSQLI_NUM);
			$result->free();
			return $array[0];
		}
		$array = [];
		while($ar = $result->fetch_array(MYSQLI_NUM))
			$array[] = $ar[0];
		$result->free();
		return $array;
	}

	function select_count($table,$where=null) {
		$result = $this->select_fetch($table,'*',$where);
		if($result===false) return false;
		$n = $result->num_rows;
		$result->free();
		return $n;
	}

	function update($table,$updates,$where=null) {
		$table = db_var($this->prefix.$table);
		$query = "UPDATE $table SET ".($this->fix_updates($updates)).($this->fix_where($where)).";\n";
		return $this->query($query);
	}

	function insert($table,$inserts,$columns=null,$ondup=false) {
		$table = db_var($this->prefix.$table);
		$values = $this->fix_inserts($inserts,$columns);
		$colstr = $this->fix_columns($columns);
		$fc = db_var($columns[0]);
		$dupseq = empty($ondup)? '': chr(10).'ON DUPLICATE KEY UPDATE '.($ondup===true? "$fc=$fc": (is_array($ondup)? $this->fix_updates($ondup):"$ondup"));
		$query = "INSERT INTO $table ($colstr) VALUES$values$dupseq;\n";
		return $this->query($query);
	}

	function insert_ignore($table,$inserts,$columns=null) {
		$table = db_var($this->prefix.$table);
		$values = $this->fix_inserts($inserts,$columns);
		$colstr = $this->fix_columns($columns);
		$fc = db_var($columns[0]);
		$query = "INSERT IGNORE INTO $table ($colstr) VALUES$values;\n";
		return $this->query($query);
	}

	function delete($table,$where,...$extra) {
		$limit = isset($extra[0])? $extra[0]: null;
		$table = db_var($this->prefix.$table);
		$query = "DELETE FROM $table".
			($this->fix_where($where)).
			(empty($limit)?'':' LIMIT '.((int)$limit))
			.";\n";
		return $this->query($query);
	}

	function log($query,$errno=null,$error=null) {
		$le_q = $this->str($query);
		$le_n = (int)(is_null($errno)? $this->errno: $errno);
		$le_r = $this->str(is_null($error)? $this->error: $error);
		$qerr = "INSERT INTO `log` (`query`,`errno`,`error`) VALUES('$le_q',$le_n,'$le_r');";
		return mysqli::query($qerr);
	}

	function query($query) {
		ph_add('queries',$query);
		$array = mysqli::query($query);
		if($array===false || $this->errno) {
			ph_add('queries','Error ('.$this->errno.'): '.$this->error.chr(10));
			print_r([$query,$this->errno,$this->error]);
			$this->log($query, $this->errno, $this->error);
		}
		return $array;
	}

	function create($table, $recreate=false) {
		if(is_string($table))
			$table = db_table::$pool[$table];
		if($recreate)
			$this->query('DROP TABLE IF EXISTS '.$table->mysql_ref($this).';');
		$this->query($table->mysql_create($this));
	}
};

function db_mysql_str(database $db,string $str) {
	return "'".$db->str($str)."'";
}

function db_mysql_bin(database $db,string $str) {
	return "x'".$db->str($str)."'";
}

function db_mysql_int(database $db,string $str) {
	return (int)$str;
}

function db_mysql_dt(database $db,string $str) {
	return "'".date('Y-m-d H:i:s',(int)$str)."'";
}

function db_mysql_date(database $db,string $str) {
	return "'".date('Y-m-d',(int)$str)."'";
}

function db_mysql_time(database $db,string $str) {
	return "'".date('H:i:s',(int)$str)."'";
}

function db_mysql_bool(database $db,string $str) {
	return empty($str)? 'false': 'true';
}

db_type::$pool['binary32']->set_mysql('BINARY','db_mysql_bin');
db_type::$pool['tinyint']->set_mysql('TINYINT','db_mysql_int');
db_type::$pool['smallint']->set_mysql('SMALLINT','db_mysql_int');
db_type::$pool['int']->set_mysql('INT','db_mysql_int');
db_type::$pool['unsigned']->set_mysql('INT UNSIGNED','db_mysql_int');
db_type::$pool['bigint']->set_mysql('BIGINT','db_mysql_int');
db_type::$pool['label10']->set_mysql('CHAR',null,'ascii');
db_type::$pool['label20']->set_mysql('CHAR',null,'ascii');
db_type::$pool['label40']->set_mysql('CHAR',null,'ascii');
db_type::$pool['text64']->set_mysql('VARCHAR');
db_type::$pool['text200']->set_mysql('VARCHAR');
db_type::$pool['email']->set_mysql('VARCHAR',null,'ascii');
db_type::$pool['timestamp']->set_mysql('TIMESTAMP','db_mysql_dt');
db_type::$pool['datetime']->set_mysql('DATETIME','db_mysql_dt');
db_type::$pool['date']->set_mysql('DATE','db_mysql_date');
db_type::$pool['time']->set_mysql('TIME','db_mysql_time');
db_type::$pool['vartext']->set_mysql('TEXT');
db_type::$pool['boolean']->set_mysql('BOOLEAN','db_mysql_bool');

?>
