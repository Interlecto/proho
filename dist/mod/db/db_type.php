<?php
/* mod/db/db_type.php
 * @author: Carlos Thompson
 *
 * Implementation for database elements.
 */

require_once "mod/db/db.php";

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
	$s = preg_replace(['{^\W+|\W+$}','{\W+}'],['','_'],strtolower($input));
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
}

?>
