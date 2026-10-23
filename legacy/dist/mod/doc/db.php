<?php
/* mod/doc/db.php
 * @author: Carlos Thompson
 *
 * Database definitions for documents module.
 */

require_once 'mod/db/db_obj.php';

$db_status_table = new db_table('status');
$db_status_table->new_column('code','smallint',DB_PRIM_KEY);
$db_status_table->new_column('label','label40',DB_NOTNULL);

$http_status = [
	100 => 'Continue',
	101 => 'Switching Protocols',
	102 => 'Processing',
	200 => 'OK',
	201 => 'Created',
	202 => 'Accepted',
	203 => 'Non-Authoritative Information',
	204 => 'No Content',
	205 => 'Reset Content',
	206 => 'Partial Content',
	207 => 'Multi-Status',
	208 => 'Already Reported',
	226 => 'IM Used',
	300 => 'Multiple Choices',
	301 => 'Moved Permanently',
	302 => 'Found',
	303 => 'See Other',
	304 => 'Not Modified',
	305 => 'Use Proxy',
	306 => 'Switch Proxy',
	307 => 'Temporary Redirect',
	308 => 'Permanent Redirect',
	400 => 'Bad Request',
	401 => 'Unauthorized',
	402 => 'Payment Required',
	403 => 'Forbidden',
	404 => 'Not Found',
	405 => 'Method Not Allowed',
	406 => 'Not Acceptable',
	407 => 'Proxy Authentication Required',
	408 => 'Request Timeout',
	409 => 'Conflict',
	410 => 'Gone',
	411 => 'Length Required',
	412 => 'Precondition Failed',
	413 => 'Payload Too Large',
	414 => 'URI Too Long',
	415 => 'Unsupported Media Type',
	416 => 'Range Not Satisfiable',
	417 => 'Expectation Failed',
	418 => 'I\'m a teapot',
	421 => 'Misdirected Request',
	422 => 'Unprocessable Entity',
	423 => 'Locked',
	424 => 'Failed Dependency',
	426 => 'Upgrade Required',
	428 => 'Precondition Required',
	429 => 'Too Many Requests',
	431 => 'Request Header Fields Too Large',
	451 => 'Unavailable For Legal Reasons',
	500 => 'Internal Server Error',
	501 => 'Not Implemented',
	502 => 'Bad Gateway',
	503 => 'Service Unavailable',
	504 => 'Gateway Timeout',
	505 => 'HTTP Version Not Supported',
	506 => 'Variant Also Negotiates',
	507 => 'Insufficient Storage',
	508 => 'Loop Detected',
	510 => 'Not Extended',
	511 => 'Network Authentication Required',
];

$db_docscp_table = new db_table('doc_scope');
$db_docscp_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_docscp_table->new_column('label','label20',DB_UNIKEY);

$db_docfmt_table = new db_table('doc_format');
$db_docfmt_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_docfmt_table->new_column('label','label20',DB_UNIKEY);

$db_docns_table = new db_table('doc_ns');
$db_docns_table->new_reference('id',$db_obj_table,'oid',DB_PRIM_KEY);
$db_docns_table->new_reference('scope',$db_docscp_table,'key',DB_NOTNULL);

$db_doc_table = new db_table('document');
$db_doc_table->set_pk(['id','lang']);
$db_doc_table->new_reference('id',$db_obj_table,'oid',DB_NOTNULL);
$db_doc_table->new_reference('lang',$db_lang_table,'code',DB_NOTNULL);
$db_doc_table->new_reference('ns',$db_docns_table,'id',DB_DEFNULL);
$db_doc_table->new_column('body','vartext',DB_CLEAR);
$db_doc_table->new_reference('format',$db_docfmt_table,'key',DB_CLEAR,1);

$db_doctag_table = new db_table('doc_tag');
$db_doctag_table->set_pk(['pid','label']);
$db_doctag_table->new_reference('pid',$db_doc_table,'id',DB_NOTNULL);
$db_doctag_table->new_column('label','label40',DB_NOTNULL);

class db_namespace extends db_obj {
	static $ns_pool = [];

	static function retrieve($pid) {
		$ns = new self();
		db_obj::_retrieve($ns,$pid);
		$ns->load($pid);
		return $ns;
	}

	static function find_label($label) {
		$ns = new self();
		db_obj::_find_label($ns,$this,$label);
		$ns->load();
		return $ns;
	}

	static function create($label,...$params) {
		$ns = new self();
		db_obj::_create($ns,$label,'doc_ns');
		$ns->set('scope',isset($params[0])?$params[0]:null);
		return $ns;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('doc_ns','id',$where,$orderby);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_namespace::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['doc_ns'] = db_table::$pool['doc_ns'];
		db_obj::__construct();
	}

	function load($nid=null,...$p) {
		$id = (int)(is_null($nid)? $this->id(): $nid);
		$a = db_obj::$db->select_first('doc_ns','*',['id'=>"=$id"]);
		if(empty($a))
			return db_obj::log('db_namespace::load',"'$id'","No namespace yet with ID '$id'");
		$ns->set('scope', $a['scope']);
	}

	function save() {
		db_obj::save();
		$where = ['id'=>(int)$this->id()];
		$update = ['scope'=>$this->get('scope')];

		if(db_obj::$db->select_count('doc_ns',$where))
			return db_obj::$db->update('doc_ns',$update,$where);
		else
			return db_obj::$db->insert('doc_ns',array_merge($where,$update));
	}

	function remove() {
		$where = ['id'=>(int)$this->id()];
		return db_obj::$db->delete('doc_ns',$where) &&
			db_obj::remove();
	}
}

class db_document extends db_obj {
	static function retrieve($pid) {
		$document = new self();
		db_obj::_retrieve($document,$pid);
		$document->load($pid);
		return $document;
	}

	static function find_label($label) {
		$document = new self();
		db_obj::_find_label($document,$label);
		$document->load();
		return $document;
	}

	static function create($label,...$params) {
		$document = new self();
		db_obj::_create($document,$label,'document');
		$document->set_params(...$params);
		return $document;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('document','id',$where,$orderby);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_document::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['document'] = db_table::$pool['document'];
		db_obj::__construct();
	}

	function set_params(...$params) {
		if(isset($params[0]))
			$this->set('ns',db_obj::get_oid($params[0]));
		if(isset($params[1]))
			$this->set('lang',$params[1]);
	}

	function load($pid=null,...$p) {
		$id = (int)(is_null($pid)? $this->id(): $pid);
		if(isset($p[0]))
			$lang = $p[0];
		else {
			global $obj;
			$lang = isset($obj['site']['lang'])? $obj['site']['lang']: null;
		}
		$a = db_obj::$db->select_key('document','lang',['id'=>"=$id"]);
		if(empty($a))
			return db_obj::log('db_document::load',"'$id'","No document yet with ID '$id'");
		if($lang) {
			if(empty($a[$lang]))
				return db_obj::log('db_document::load',"'$id'","No document yet with ID '$id' and language '$lang'");
			foreach(array_keys($this->fields['document']->columns) as $field)
				$this->set($field, $a[$lang][$field]);
		}
		$lang = array_keys($a)[0];
		foreach(array_keys($this->fields['document']->columns) as $field)
			$this->set($field, $a[$lang][$field]);
	}

	function save() {
		db_obj::save();
		$where = ['id'=>(int)$this->id(),'lang'=>$this->lang];
		$update = [];
		foreach(array_keys($this->fields['document']->columns) as $field)
			if(!in_array($field,array_keys($where)) && isset($this->$field))
				$update[$field] = $this->$field;
		if(db_obj::$db->select_count('document',$where))
			return db_obj::$db->update('document',$update,$where);
		else
			return db_obj::$db->insert('document',array_merge($where,$update));
	}

	function remove() {
		$where = ['id'=>(int)$this->id(),'lang'=>$this->lang];
		return db_obj::$db->delete('document',$where) &&
			db_obj::remove();
	}
}

?>
