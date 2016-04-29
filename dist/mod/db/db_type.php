<?php
/* mod/db/db_type.php
 * @author: Carlos Thompson
 *
 * Implementation for database elements.
 */

require_once "mod/db/db.php";
require_once "lib/lib.php";

class db_type {
	static $pool=[];
	public $name;
	public $length;
	public $clean_func;

	public function __construct(string $name, $length=null, $func=null) {
		$this->name = $name;
		$this->length = $length;
		$this->clean_func = $func;
		db_type::$pool[$name] = $this;
	}

	public function clean(string $input,...$params) {
		if(is_null($this->clean_func)) {
			if(is_null($this->length)) return $input;
			else return substr($input,0,$this->length);
		}
		if(is_null($this->length))
			return ($this->clean_func)($input,...$params);
		else
			return ($this->clean_func)($input,$this->length,...$params);
	}

	/* MySQL specifics */
	public $my_def=null;
	public $my_func=null;
	public $collect=null;

	public function set_mysql($type, $func=null, $collect=null) {
		$this->my_def = $type;
		$this->my_func = $func;
		$this->collect = $collect;
		return $this;
	}
	public function mysql_def(database $db) {
		return is_null($this->length)? $this->my_def: "{$this->my_def}($this->length)";
	}
	public function mysql_data(database $db, string $str) {
		if(is_null($this->my_func)) return "'".$db->str($str)."'";
		return ($this->my_func)($db,$str);
	}
}

function db_type_bin(string $str, int $n, ...$params) {
	$clean = preg_replace('/[^0-9a-f]+/','',strtolower($str));
	return substr($clean,0,2*$n);
}

function db_type_int(string $input, ...$params) {
	return (int)$input;
}

function db_type_label(string $input, int $n, ...$params) {
	$s = preg_replace(["{'}",'{^\W+|\W+$}','{\W+}'],['','','_'],strtolower(toASCII($input)));
	return substr($s,0,$n);
}

function db_type_email(string $input, int $n,...$params) {
	preg_match('/[-.\w]+@(?:[-\w]+\.)\w{2,}/',$input,$m);
	return substr($m[0],0,$n);
}

function db_type_time(string $input,...$params) {
	return strtotime($input);
}

function db_type_bool(string $input,...$params) {
	return !empty($input);
}

new db_type('binary32',32,'db_type_bin');
new db_type('tinyint',null,'db_type_int');
new db_type('smallint',null,'db_type_int');
new db_type('int',null,'db_type_int');
new db_type('unsigned',null,'db_type_int');
new db_type('bigint',null,'db_type_int');
new db_type('label10',10,'db_type_label');
new db_type('label20',20,'db_type_label');
new db_type('label40',40,'db_type_label');
new db_type('text64',64);
new db_type('text200',200);
new db_type('email',80,'db_type_email');
new db_type('timestamp',null,'db_type_time');
new db_type('datetime',null,'db_type_time');
new db_type('date',null,'db_type_time');
new db_type('time',null,'db_type_time');
new db_type('vartext');
new db_type('boolean',null,'db_type_bool');

class db_column {
	public $name;
	public $type;
	public $default=null;
	public $flags;

	public function __construct(string $name, string $type, $flags=DB_CLEAR, $default=null) {
		$this->name = $name;
		$this->type = db_type::$pool[$type];
		$this->flags = $flags;
		$this->default = is_null($default)? null: $this->type->clean($default);
	}

	public function make_pk() {
		$this->flags |= DB_PRIMARY;
	}

	public function make_unique() {
		$this->flags |= DB_UNIQUE;
	}

	public function set_collect($collect) {
		$this->collect = $collect;
	}

	public function clean(string $input, ...$params) {
		return $this->type->clean($input, ...$params);
	}

	/* MySQL specifics */
	public function mysql_def(database $db) {
		$s = [$this->mysql_ref($db)];
		$s[] = $this->type->mysql_def($db);
		$collect = $this->type->collect;
		if($collect) {
			$p = explode('_',$collect);
			$cs = $p[0];
			$s[] = "CHARACTER SET $cs";
			if(count($p)>1)
				$s[] = "COLLECT ".$collect;
		}
		if($this->flags & DB_NOTNULL)
			$s[] = 'NOT NULL';
		if($this->flags & DB_AUTO)
			$s[] = 'AUTO_INCREMENT';
		elseif($this->flags & DB_DEFNULL)
			$s[] = 'DEFAULT NULL';
		elseif($this->flags & DB_CURRENT)
			$s[] = 'DEFAULT CURRENT_TIMESTAMP';
		elseif(!is_null($this->default))
			$s[] = 'DEFAULT '.$this->type->mysql_data($db, $this->default);
		elseif(!($this->flags & DB_NOTNULL))
			$s[] = 'NULL';
		if($this->flags & DB_PRIMARY)
			$s[] = 'PRIMARY KEY';
		elseif($this->flags & DB_UNIQUE)
			$s[] = 'UNIQUE';
		return implode(' ',$s);
	}

	public function mysql_ref(database $db) {
		return '`'.$db->str($this->name).'`';
	}

	public function mysql_data(database $db, string $str) {
		return $this->type->mysql_data($db,$str);
	}

	public function mysql_assign(database $db, $value) {
		return $this->mysql_ref($db).' = '.$this->mysql_data($db, $value);
	}

	public function mysql_comp(database $db, $value, $sign=' = ') {
		$field = $this->mysql_ref($db);
		if(is_array($value)) {
			$vl = [];
			foreach($value as $v)
				$vl[] = $this->mysql_data($db, $v);
			switch(trim($sign)) {
			case '2':
				return "$field BETWEEN {$vl[0]} AND {$vl[1]}";
			case '!2':
				return "$field NOT BETWEEN {$v[0]} AND {$v[1]}";
			case '!':
				return "$field NOT IN (".implode(', '.$vl).')';
			default:
				return "$field IN (".implode(', '.$vl).')';
			}
		}
		if(is_null($value)) {
			switch(trim($sign)) {
			case '!':
				return "$field IS NOT NULL";
			default:
				return "$field IS NULL";
			}
		}
		if(is_bool($value)) {
			switch(trim($sign)) {
			case '!':
				return "$field IS NOT ".($value?'TRUE':'FALSE');
			default:
				return "$field IS ".($value?'TRUE':'FALSE');
			}
		}
		$data = $this->mysql_data($db, $value);
		switch(trim($sign)) {
		case '~':
			return "$field LIKE $data";
		case '!~':
			return "$field NOT LIKE $data";
		case '!=':
			$sign = '<>';
		case '<': case '<=':
		case '>': case '>=':
		case '=': case '<>':
		case '<=>':
			return $field.$sign.$data;
		default:
			return "$field = $data";
		}
	}
}

class db_reference extends db_column {
	public $table;
	public $key;

	public function __construct(string $name, string $table, db_column $column, $flags=DB_NOTNULL, $default=null) {
		db_column::__construct($name,$column->type->name,$flags,$default);
		$this->table = $table;
		$this->key = $column;
	}

	/* MySQL specifics */
	public function mysql_constrain(database $db) {
		return 'FOREIGN KEY ('.
			$this->mysql_ref($db).') REFERENCES '.
			'`'.$db->prefix.$db->str($this->table).'`('.
			$this->key->mysql_ref($db).')';
	}
}

class db_table {
	static $pool = [];
	public $name;
	public $columns = [];
	public $constrains = [];

	public function __construct($name) {
		$this->name = $name;
		db_table::$pool[$name] = $this;
	}

	public function add_column(db_column $column) {
		$this->columns[$column->name] = $column;
	}

	public function new_column(string $name, string $type, $flags=DB_NOTNULL, $default=null) {
		$this->columns[$name] = new db_column($name, $type, $flags, $default);
	}

	public function get_column(string $name) {
		return isset($this->columns[$name])? $this->columns[$name]: false;
	}

	public function column_exists(string $name) {
		return isset($this->columns[$name]);
	}

	public function new_reference(string $name, db_table $refers, string $key, $flags=DB_NOTNULL, $default=null) {
		$this->columns[$name] = $fk = new db_reference($name, $refers->name, $refers->columns[$key], $flags, $default);
		$this->constrains[] = ['FK', $fk];
	}

	public function set_pk($columns) {
		if(is_string($columns))
			$this->columns[$columns]->make_pk();
		elseif(is_array($columns))
			$this->constrains[] = ['PK', $columns];
	}

	public function set_unique(string $column) {
		$this->columns[$column]->make_unique();
	}

	/* MySQL specifics */
	public function mysql_ref(database $db) {
		return '`'.$db->prefix.$db->str($this->name).'`';
	}

	public function mysql_constrain(database $db,$constrain) {
		switch($constrain[0]) {
		case 'PK':
			$x=[];
			foreach($constrain[1] as $col)
				$x[] = $this->columns[$col]->mysql_ref($db);
			return 'PRIMARY KEY ('.implode(',',$x).')';
		case 'FK':
			return $constrain[1]->mysql_constrain($db);
		default:
			#return print_r($constrain,true);
		}
	}

	public function mysql_create(database $db) {
		$s = [];
		foreach($this->columns as $name=>$column) {
			$s[] = $column->mysql_def($db);
		}
		foreach($this->constrains as $constrain) {
			$s[] = $this->mysql_constrain($db,$constrain);
		}
		return "CREATE TABLE ".$this->mysql_ref($db)." (\n\t".
			implode(",\n\t",$s)."\n);\n";
	}

	public function mysql_insert(database $db,array $columns,array $values,$flags=0) {
		// $columns is an array of strings, representing the names of the columns
		// $values is an array of associative arrays
		$cl = [];
		$cls = [];
		foreach($columns as $col)
			if(isset($this->columns[$col])) {
				$cls[] = $col;
				$cl[] = $db->field($col);
			}
		if(empty($cl)) return false;
		$column_list = ' ('.implode(', ',$cl).')';
		$value_list = [];
		foreach($values as $touple) {
			$tl = [];
			foreach($cls as $c)
				$tl[] = isset($touple[$c]) && !is_null($touple[$c])?
					$this->columns[$c]->clean($touple[$c]):
					'NULL';
			$value_list[] = '('.implode(', ',$tl).')';
		}
		return "INSERT INTO ".$this->mysql_ref($db).
			$column_list."\nVALUES\n\t".
			implode(",\n\t",$value_list).";\n";
	}

	public function mysql_update(database $db, array $updates, array $where, $flags=0) {
		// $updates is an associative array of the form 'column_name'=>'value to update'
		// $where is an associative array of the form 'column_name'=>'value to match'
		$assigns = [];
		foreach($updates as $col=>$val) {
			if(!isset($this->columns[$col])) continue;
			$assigns[] = $this->columns[$col]->assign($db,$val);
		}
		$conditions = [];
		foreach($where as $col=>$val) {
			if(!isset($this->columns[$col])) continue;
			preg_match('/^(\W{0,2})(.*)/',$val,$m);
			$conditions[] = $this->columns[$col]->comp($db,$m[2],$m[1]);
		}
		return "UPDATE ".$this->mysql_ref($db)." SET\n\t".
			implode(",\n\t",$asigns)."\nWHERE\n\t".
			implode("\nAND\t",$conditions).";\n";
	}

	public function mysql_select(database $db, array $columns=null, array $where=[], array $orderby=[], $flags=0) {
		// $columns is a list of column names; or null or empty array for all columns.
		// $where is an associative array of the form 'column_name'=>'value to match'
		// $orderby is a list of column names, which might be preceded by a sign.
		if(empty($columns))
			$columns_list = '*';
		else {
			$cl = [];
			$cls = [];
			foreach($columns as $col)
				if(isset($this->columns[$col])) {
					$cls[] = $col;
					$cl[] = $db->field($col);
				}
			if(empty($cl)) return false;
			$column_list = ' ('.implode(', ',$cl).')';
		}
		$conditions = [];
		foreach($where as $col=>$val) {
			if(!isset($this->columns[$col])) continue;
			preg_match('/^([!~<=>]{0,3})(.*)/',$val,$m);
			$conditions[] = $this->columns[$col]->comp($db,$m[2],$m[1]);
		}
		$orders = [];
		foreach($orderby as $scol) {
			$col = trim($col,'+-');
			if(!isset($this->columns[$col])) continue;
			$f = $db->field($col);
			if(substr($scol,0,1)=='-') $f.=' DESC';
			if(substr($scol,0,1)=='+') $f.=' ASC';
			$orders[] = $f;
		}
		return "SELECT ".$column_list." FROM ".$this->mysql_ref($db).
			(empty($conditions)?'':"\nWHERE\n\t".implode("\nAND\t",$conditions)).
			(empty($orders)?'':"\nORDER BY\n\t".implode(",\n\t",$orders)).";\n";
	}
}

?>
