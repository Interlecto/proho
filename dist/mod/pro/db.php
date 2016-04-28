<?php
/* mod/pro/db.php
 * @author: Carlos Thompson
 *
 * Database definitions for profile module.
 */

require_once 'mod/db/db_obj.php';

$db_person_table = new db_table('person');
$db_person_table->new_reference('id',$db_obj_table,'oid',DB_PRIM_KEY);
$db_person_table->new_column('email','email',DB_CLEAR);
$db_person_table->set_unique('email');
$db_person_table->new_column('gname','text64',DB_NOTNULL);
$db_person_table->new_column('sname','text64',DB_CLEAR);

class db_person extends db_obj {
	static function retrieve($pid) {
		$person = new self();
		db_obj::_retrieve($person,$pid);
		$person->load($pid);
		return $person;
	}

	static function find_label($label) {
		$person = new self();
		db_obj::_find_label($person,$label);
		$person->load();
		return $person;
	}

	static function create($label,...$params) {
		$person = new self();
		db_obj::_create($person,$label,'person');
		foreach(['email','gname','sname'] as $i=>$field) {
			if(isset($params[$i]))
				$person->set($field,$params[$i]);
			else
				$person->set($field,null);
		}
		return $person;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('person','id',$where,$orderby);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_person::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['person'] = db_table::$pool['person'];
		db_obj::__construct();
	}

	function load($pid=null,...$p) {
		$id = (int)(is_null($pid)? $this->id(): $pid);
		$a = db_obj::$db->select_first('person','*',['id'=>"=$id"]);
		if(empty($a))
			return db_obj::log('person::load',"'$id'","No person yet with ID '$id'");
		$this->set('email', $a['email']);
		$this->set('gname', $a['gname']);
		$this->set('sname', $a['sname']);
	}

	function save() {
		db_obj::save();
		$where = ['id'=>(int)$this->id()];
		$update = [];
		foreach(['email','gname','sname'] as $field)
			$update[$field] = $this->$field;
		if(db_obj::$db->select_count('person',$where))
			return db_obj::$db->update('person',$update,$where);
		else
			return db_obj::$db->insert('person',array_merge($where,$update));
	}

	function remove() {
		$where = ['id'=>(int)$this->id()];
		return db_obj::$db->delete('person',$where) &&
			db_obj::remove();
	}

	function full_name() {
		if($this->sname) return $this->gname.' '.$this->sname;
		return $this->gname;
	}
}

?>
