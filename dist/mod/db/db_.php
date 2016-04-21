<?php
function db_str($string) { return mysqli_real_escape_string(db::$first,$string); }
function db__str(&$string,$idx=null) { return $string = db_str($string); }
function db_val($value) { return is_null($value)? 'NULL': (is_numeric($value)? (float)$value: "'".db_str($value)."'"); }
function db__val(&$value,$idx=null) { return $value = db_val($value); }
function db_var($key) { return "`".db_str($key)."`"; }
function db__var(&$key,$idx=null) { return $key = db_var($key); }
function db_varval($string) { return preg_match('{^[_A-Za-z]\w*$}',$string)? db_var($string): db_val($string); }
function db__varval(&$string,$idx=null) { return $string = db_varval($string); }

class db extends mysqli {
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
			$a = array();
			foreach($where as $key=>$act) {
				db__var($key);
				if(is_array($act)) {
					array_walk($act,'db__val');
					$valstr = implode(',',$act);
					$a[] =  "$key IN ($valstr)";
					continue;
				}
				preg_match('{^([[(=<>!#&])?([^&)]*)&?(.*)}',$act,$m);
				switch($c=$m[1]) {
				case '&':
					$i0 = db_val($m[2]);
					$i1 = db_val($m[3]);
					$a[] = "$key>=$i0 AND $key<$i1";
					break;
				case '#':
					$a[] = "$key IS NULL";
					break;
				case '!':
					$a[] = $m[2]=='#'? "$key IS NOT NULL": "$key <> ".db_val($m[2]);
					break;
				case '(':
					$values = explode(',',$m[2]);
					array_walk($values,'db__val');
					$valstr = implode(',',$values);
					$a[] =  "$key IN ($valstr)";
					break;
				case '[':
					$s = rtrim($m[2],']');
					$l = strlen($s);
					$a[] = "LEFT($key,$l) = ".db_val($s);
					break;
				case '':
					$c='=';
				default:
					$val = db_val($m[2]);
					$a[] = "$key $c $val";
				}
			}
			return " WHERE ".(count($a)?implode(' AND ',$a):1);
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
			$a = array();
			foreach($orderby as $key)
				$a[] = substr($key,0,1)=='-'? db_varval(substr($key,1)).' DESC': db_varval($key);
			if(count($a))
				return " ORDER BY ".implode(',',$a);
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

	function __construct($a,$b,$c,$d=null,$prefix='') {
		if(!isset(db::$first)) db::$first = $this;
		$this->prefix = $prefix;
		@mysqli::__construct($a,$b,$c);
		if($this->connect_errno)
			il_add('queries','Connect Error: '.$this->connect_error);
		if($d) {
			if(!@$this->select_db($d)) {
				header('Location: /install.php');
				die("Database $d does not exist.");
			}
		}
	}

	function str($string) { return $this->real_escape_string($string); }

	function select_fetch($table,$columns='*',$where=null,$orderby=null,$limit=null,$offset=null) {
		$table = db_var($this->prefix.$table);
		$query = 'SELECT '.($this->fix_columns($columns))." FROM $table".
			($this->fix_where($where)).($this->fix_orderby($orderby)).
			($this->fix_limits($offset,$limit)).";\n";
		return $this->query($query);
	}

	function select($table,$columns='*',$where=null,$orderby=null,$limit=250,$offset=0) {
		$r = $this->select_fetch($table,$columns,$where,$orderby,$limit,$offset);
		if($r===false) return false;
		$a = array();
		while($ar = $r->fetch_array(MYSQLI_ASSOC))
			$a[] = $ar;
		$r->free();
		return $a;
	}

	function select_key($table,$key_columns,$where=null,$orderby=null,$limit=250,$offset=0) {
		$table = db_var($this->prefix.$table);
		if(is_array($key_columns)) {
			$key = $key_columns[0];
			$cols = $this->fix_columns($key_columns);
		} else {
			$key = "$key_columns";
			$cols = '*';
		}
		$query = "SELECT $cols FROM $table".
			($this->fix_where($where)).($this->fix_orderby($orderby)).
			($this->fix_limits($offset,$limit)).";\n";
		$r = $this->query($query);
		if($r===false) return false;
		$a = array();
		while($ar = $r->fetch_array(MYSQLI_ASSOC))
			$a[$ar[$key]] = $ar;
		$r->free();
		return $a;
	}

	function select_pairs($table,$key_col,$val_col,$where=null,$orderby=null,$limit=250,$offset=0) {
		$table = db_var($this->prefix.$table);
		db__var($key_col);
		db__var($val_col);
		$query = "SELECT $key_col,$val_col FROM $table".
			($this->fix_where($where)).($this->fix_orderby($orderby)).
			($this->fix_limits($offset,$limit)).";\n";
		$r = $this->query($query);
		if($r===false) return false;
		$a = array();
		while($ar = $r->fetch_array(MYSQLI_NUM))
			$a[$ar[0]] = $ar[1];
		$r->free();
		return $a;
	}

	function select_col($table,$col,$where=null,$orderby=null,$limit=250,$offset=0) {
		$table = db_var($this->prefix.$table);
		db__var($col);
		$query = "SELECT $col FROM $table".
			($this->fix_where($where)).($this->fix_orderby($orderby)).
			($this->fix_limits($offset,$limit)).";\n";
		$r = $this->query($query);
		if($r===false) return false;
		$a = array();
		while($ar = $r->fetch_array(MYSQLI_NUM))
			$a[] = $ar[0];
		$r->free();
		return $a;
	}

	function select_first($table,$columns='*',$where=null,$orderby=null) {
		$r = $this->select($table,$columns,$where,$orderby,1);
		return $r? $r[0]: false;
	}

	function select_one($table,$column,$where=null,$asarray=false) {
		$table = db_var($this->prefix.$table);
		db__var($column);
		$query = "SELECT $column FROM $table".($this->fix_where($where));
		if(!is_bool($asarray)) $query.= $this->fix_orderby($asarray);
		elseif($asarray===true) $query.= " ORDER BY $column";
		$query.= ";\n";
		$r = $this->query($query);
		if($r===false) return false;
		if($asarray===false) {
			$a = $r->fetch_array(MYSQLI_NUM);
			$r->free();
			return $a[0];
		}
		$a = array();
		while($ar = $r->fetch_array(MYSQLI_NUM))
			$a[] = $ar[0];
		$r->free();
		return $a;
	}

	function update($table,$updates,$where=null) {
		$table = db_var($this->prefix.$table);
		$query = "UPDATE $table SET ".($this->fix_updates($updates)).($this->fix_where($where)).";\n";
		$this->query($query);
	}

	function insert($table,$inserts,$columns=null,$ondup=false) {
		$table = db_var($this->prefix.$table);
		$values = $this->fix_inserts($inserts,$columns);
		$colstr = $this->fix_columns($columns);
		$fc = db_var($columns[0]);
		$dupseq = empty($ondup)? '': chr(10).'ON DUPLICATE KEY UPDATE '.($ondup===true? "$fc=$fc": (is_array($ondup)? $this->fix_updates($ondup):"$ondup"));
		$query = "INSERT INTO $table ($colstr) VALUES$values$dupseq;\n";
		$this->query($query);
	}

	function insert_ignore($table,$inserts,$columns=null) {
		$table = db_var($this->prefix.$table);
		$values = $this->fix_inserts($inserts,$columns);
		$colstr = $this->fix_columns($columns);
		$fc = db_var($columns[0]);
		$query = "INSERT IGNORE INTO $table ($colstr) VALUES$values;\n";
		$this->query($query);
	}

	function query($query) {
		il_add('queries',$query);
		$a = mysqli::query($query);
		if($a===false) {
			il_add('queries','Error ('.$this->errno.'): '.$this->error.chr(10));
			$errno = $this->errno;
			$error = $this->escape_string($this->error);
			$qquery = $this->escape_string($query);
			mysqli::query("INSERT INTO `log`(`errno`,`error`,`query`) VALUES ($errno,'$error','$qquery');");
		}
		return $a;
	}
};

function db_query($query) {
	return db::$first->query($query);
}

function db_select_fetch($table, $columns='*', $where=null, $order=null, $limit=0, $offset=0) {
	return db::$first->select_fetch($table, $columns, $where, $order, $limit, $offset);
}

function db_select($table, $columns='*', $where=null, $order=null, $limit=250, $offset=0) {
	return db::$first->select($table, $columns, $where, $order, $limit, $offset);
}

function db_select_key($table, $columns, $where=null, $order=null, $limit=250, $offset=0) {
	return db::$first->select_key($table, $columns, $where, $order, $limit, $offset);
}

function db_select_pairs($table, $col1, $col2, $where=null, $order=null, $limit=250, $offset=0) {
	return db::$first->select_pairs($table, $col1, $col2, $where, $order, $limit, $offset);
}

function db_select_col($table, $columns, $where=null, $order=null, $limit=250, $offset=0) {
	return db::$first->select_col($table, $columns, $where, $order, $limit, $offset);
}

function db_select_first($table, $columns, $where=null, $order=null) {
	return db::$first->select_first($table, $columns, $where, $order);
}

function db_select_one($table, $columns, $where=null, $asarray=false) {
	return db::$first->select_one($table, $columns, $where, $asarray);
}

function db_update($table, $updates, $where=null) {
	return db::$first->update($table, $updates, $where);
}

function db_insert($table, $inserts, $columns=null, $ondup=false) {
	return db::$first->insert($table, $inserts, $columns, $ondup);
}

function db_insert_ignore($table, $inserts, $columns=null) {
	return db::$first->insert_ignore($table, $inserts, $columns);
}

function db_comment($limit) {
	return db_query('-- '.trim($limit).chr(10));
}
?>
