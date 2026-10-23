<?php
/* mod/sec/db.php
 * @author: Carlos Thompson
 *
 * Database definitions for security module.
 */

require_once 'mod/pro/db.php';

$db_userhash_table = new db_table('user_hash');
$db_userhash_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_userhash_table->new_column('label','label20',DB_UNIKEY);

$db_usersts_table = new db_table('user_status');
$db_usersts_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_usersts_table->new_column('label','label20',DB_UNIKEY);

$db_user_table = new db_table('user');
$db_user_table->new_reference('id',$db_person_table,'id',DB_PRIM_KEY);
$db_user_table->new_column('hash','binary32',DB_CLEAR);
$db_user_table->new_reference('hash_type',$db_userhash_table,'key',DB_NOTNULL,1);
$db_user_table->new_reference('status',$db_usersts_table,'key',DB_NOTNULL,0);

$db_userlvl_table = new db_table('user_level');
$db_userlvl_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_userlvl_table->new_column('label','label20',DB_UNIKEY);

$db_userpermit_table = new db_table('user_permit');
$db_userpermit_table->set_pk(['id','on']);
$db_userpermit_table->new_reference('id',$db_user_table,'id',DB_NOTNULL);
$db_userpermit_table->new_reference('on',$db_obj_table,'oid',DB_NOTNULL,0);
$db_userpermit_table->new_reference('level',$db_userlvl_table,'key',DB_NOTNULL,1);

class db_user extends db_person {
	protected $_permits = [];

	static function retrieve($pid) {
		$user = new self();
		db_obj::_retrieve($user,$pid);
		$user->load($pid);
		return $user;
	}

	static function find_label($label) {
		$user = new self();
		db_obj::_find_label($user,$this,$label);
		$user->load();
		return $user;
	}

	static function create($label,...$params) {
		$user = new self();
		db_obj::_create($user,$label,'user');
		foreach([1=>'email','gname','sname'] as $i=>$field) {
			if(isset($params[$i]))
				$user->set($field,$params[$i]);
			else
				$user->set($field,null);
		}
		$user->_permits[0] = isset($params[0])? $params[0]: 1;
		return $user;
	}

	static function upgrade(db_person $person,$level=1) {
		$pid = $person->id();
		$user = new self();
		db_obj::_retrieve($user,$pid);
		$user->set('email',$person->get('email'));
		$user->set('gname',$person->get('gname'));
		$user->set('sname',$person->get('sname'));
		$user->change_table('user');
		$user->_permits[0] = $level;
		return $user;
	}

	function __construct() {
		$this->fields['user'] = db_table::$pool['user'];
		db_person::__construct();
	}

	function load($pid=null, ...$p) {
		db_person::load($pid);
		$id = (int)(is_null($pid)? $this->id(): $pid);
		$o = db_obj::$db->select_first('user','HEX(`hash`) hx,*',['id'=>$id]);
		if(empty($o))
			return db_obj::log('user::load',"'$id'","No user yet with ID '$id'");
		$this->set('hash', $o->hx);
		$this->set('hash_type', $o->hash_type);
		$this->set('status', $o->sname);
		$this->_permits = db_obj::$db->select_pairs('user_permit','on','level',['id'=>$id]);
	}

	function save() {
		db_person::save();
		$id = (int)$this->id();
		$where = ['id'=>$id];
		$update = [];
		if(isset($this->hash))
			$update['hash'] = '0x'.$this->hash;
		foreach(['hash_type','status'] as $field)
			if(isset($this->$field))
				$update[$field] = $this->$field;
		if(db_obj::$db->select_count('user',$where))
			db_obj::$db->update('user',$update,$where);
		else
			db_obj::$db->insert('user',array_merge($where,$update));
		$pp = db_obj::$db->select_pairs('user_permit','on','level',$where);
		$inserts = [];
		foreach($this->_permits as $on=>$level) {
			if(isset($pp[$on]) && $pp[$on] != $level)
				db_obj::$db->update('user_permit',['level'=>$level],['id'=>$id,'on'=>$on]);
			else
				$inserts[] = [$id,$on,$level];
		}
		if(!empty($inserts))
			db_obj::$db->insert('user_permit',$inserts,['id','on','level']);
	}

	function remove() {
		$where = ['id'=>(int)$this->id()];
		return db_obj::$db->delete('user',$where) &&
			db_person::remove();
	}

	function hash($password, $hash_type) {
		$label = $this->label();
		switch($hash_type) {
		case '0': // plain
			return bin2hex($password);
		case '1': // md5
			return md5("[$label//$password]");
		case '2': // sha-256
			return hash('sha256',"[$label][$password]");
		default: // sha-256 repeated $hash_type times
			$ans = $password;
			for($i=0;$i<$hash_type;$i++) {
				$ans = hash('sha256',"<$ans><$label>");
			}
			return $ans;
		}
	}

	function set_password($password, $hash_type=null) {
		if(is_null($hash_type)) {
			$hash_type = $this->get('hash_type');
			if(is_null($hash_type))
				$hash_type = 1;
		}
		$hash = $this->hash($password, $hash_type);
		$this->set('hash',$hash);
		$this->set('hash_type',$hash_type);
	}

	function check_password($password) {
		$hash = $this->get('hash');
		$hash_type = $this->get('hash_type');
		$comp = $this->hash($password, $hash_type);
		return $hash==$comp;
	}

	function set_status(string $status) {
		$a = db_obj::$db->select_pairs('user_status','label','key');
		if(isset($a[$status]))
			$this->set('status', $a[$status]);
		else
			db_obj::log('db_user::set_status',"'$status'","User status '$status' not recognized");
	}

	function get_status() {
		$a = db_obj::$db->select_pairs('user_status','key','label');
		return $a[$this->get('status')];
	}

	static function add_status(string $status) {
		$a = db_obj::$db->select_pairs('user_status','key','label');
		$M = max(array_keys($a));
		return db_obj::$db->insert('user_status',['key'=>++$M,'label'=>$status]);
	}

	function set_level($on, int $level) {
		$id = $this->id();
		$oid = is_numeric($on)? (int)$on: db_obj::get_oid($on);
		$this->_permits[$oid] = $level;
	}

	function get_level($on, $strict=false) {
		$permits = $this->_permits;
		$oid = is_numeric($on)? (int)$on: db_obj::get_oid($on);
		return isset($permits[$oid])? $permits[$oid]: ($strict? false: $permits[0]);
	}
}

?>
