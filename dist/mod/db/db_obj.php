<?php
/* mod/db/db_obj.php
 * @author: Carlos Thompson
 *
 * Implementation for database objects.
 */

require_once "mod/db/db_type.php";

$db_log_table = new db_table('log');
$db_log_table->new_column('idx','int',DB_AUTO_KEY);
$db_log_table->new_column('date','timestamp',DB_CURRENT);
$db_log_table->new_column('query','text200');
$db_log_table->new_column('errno','int',0,0);
$db_log_table->new_column('error','text200');

$db_obj_table = new db_table('obj');
$db_obj_table->new_column('oid','unsigned',DB_PRIM_KEY);
$db_obj_table->new_column('label','label40',DB_UNIKEY);
$db_obj_table->new_column('table','label40',DB_CLEAR);

$db_lang_table = new db_table('lang');
$db_lang_table->new_column('code','label10',DB_PRIM_KEY);
$db_lang_table->new_column('label','label40',DB_NOTNULL);

$db_i18n_table = new db_table('i18n');
$db_i18n_table->set_pk(['lang','label']);
$db_i18n_table->new_reference('lang',$db_lang_table,'code');
$db_i18n_table->new_column('label','label40',DB_NOTNULL);
$db_i18n_table->new_column('phrase','text200');

class db_obj {
	static $db;
	static function set_db($db) { db_obj::$db = $db; }

	private $_oid, $_label, $_table;
	protected $fields = [];

	function table_name() {
		return $this->fields[0]->name;
	}

	static function log($function,$params,$error) {
		return db_obj::$db->log("$function($params)",-1,"$function: $error.");
	}

	function __construct() {
		$this->fields['obj'] = db_table::$pool['obj'];
		$this->_oid = 0;
		$this->_label = '';
		$this->_table = '';
	}

	static function retrieve($oid) {
		return db_obj::_retrieve(null,$oid);
	}
	static function _retrieve(&$object,$oid) {
		if(empty($object))
			$object = new self();
		$read = db_obj::$db->select_first('obj','*',['oid'=>(int)$oid]);
		if(empty($read))
			return db_obj::log('db_obj::retrieve',"'$obj'","ID '$oid' not found.");

		$object->_oid = $read['oid'];
		$object->_label = $read['label'];
		$object->_table = $read['table'];
		return $object;
	}

	static function find_label($label) {
		return db_obj::_find_label(null,$label);
	}
	static function _find_label(&$object,$label) {
		if(empty($object))
			$object = new self();
		$read = db_obj::$db->select_first('obj','*',['label'=>$label]);
		if(empty($read))
			return db_obj::log('db_obj::find_label',"'$label'","Label '$label' not found.");

		$object->_oid = $read['oid'];
		$object->_label = $read['label'];
		$object->_table = $read['table'];
		return $object;
	}

	static function create($label,...$params) {
		return db_obj::_create(null,$label,$params[0]);
	}
	static function _create(&$object,$label,$table) {
		if(empty($object))
			$object = new self();

		$object->_oid = $id = db_obj::new_oid();
		$object->_label = $label;
		$object->_table = $table;
		return $object;
	}

	static function get_list($where=null,$orderby=null) {
		return [];
	}

	static function new_oid($seed='') {
		$hx = substr(md5($seed.microtime()),7,8);
		#if(false!==($n=strpos('89abcedf',substr($hx,0,1))))
		#	$hx= $n.substr($hx,1);
		$id = hexdec($hx);
		$n = db_obj::$db->select_count('obj',['oid'=>(int)$id]);
		return $n? db_obj::new_oid($id): $id;
	}

	function set($column, $value) {
		foreach($this->fields as $table=>$field)
			if($col_desc = $field->get_column($column))
				break;
		if(empty($col_desc))
			return db_obj::log(get_class($this).'::set',"'$column','$value'","Column '$column' not defined");
		return $this->$column = is_null($value)? null: $col_desc->clean($value);
	}

	function get($column, $default=null) {
		return isset($this->$column)? $this->$column: $default;
	}

	function load($id=null,...$p) {}

	function save() {
		$where = ['oid'=>(int)$this->_oid];
		$update = ['label'=>$this->_label, 'table'=>$this->_table];

		if(db_obj::$db->select_count('obj',$where))
			return db_obj::$db->update('obj',$update,$where);
		else
			return db_obj::$db->insert('obj',array_merge($where,$update));
	}

	function remove() {
		$where = ['oid'=>(int)$this->_oid];
		return db_obj::$db->delete("obj", $where);
	}

	function id() { return $this->_oid; }
	function label() { return $this->_label; }
	function table() { return $this->_table; }

	static function get_label($label, $lang=null) {
		global $obj;
		if(!$lang && isset($obj['site']['lang']))
			$lang = $obj['site']['lang'];

		$r = db_obj::$db->select_pairs('i18n','lang','phrase',['label'=>$label]);
		if(empty($r)) return $label;
		if(isset($r[$lang])) return $r[$lang];
		if(isset($r['en'])) return $r['en'];
		if(isset($r['es'])) return $r['es'];
		return array_values($r)[0];
	}
	static function get_from_oid($oid, $lang=null) {
		$lab = db_obj::$db->select_one('obj','label',['oid'=>(int)$oid]);
		return db_obj::get_label($label, $lang);
	}
}

?>
