<?php
/* mod/pub/db.php
 * @author: Carlos Thompson
 *
 * Database definitions for publications module.
 */

require_once 'mod/pro/db.php';

$db_pubfmt_table = new db_table('pub_format');
$db_pubfmt_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_pubfmt_table->new_column('label','label20',DB_UNIKEY);

$db_pubsts_table = new db_table('pub_status');
$db_pubsts_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_pubsts_table->new_column('label','label20',DB_UNIKEY);

$db_pubscp_table = new db_table('pub_scope');
$db_pubscp_table->new_column('key','tinyint',DB_PRIM_KEY);
$db_pubscp_table->new_column('label','label20',DB_UNIKEY);

$db_pub_table = new db_table('publication');
$db_pub_table->set_pk(['id','lang']);
$db_pub_table->new_reference('id',$db_obj_table,'oid',DB_NOTNULL);
$db_pub_table->new_reference('lang',$db_lang_table,'code');
$db_pub_table->new_column('body','vartext',DB_CLEAR);
$db_pub_table->new_reference('format',$db_pubfmt_table,'key',DB_CLEAR,1);
$db_pub_table->new_reference('author',$db_person_table,'id',DB_DEFNULL);
$db_pub_table->new_column('date','timestamp',DB_CURRENT);
$db_pub_table->new_reference('status',$db_pubsts_table,'key',DB_CLEAR,0);
$db_pub_table->new_reference('scope',$db_pubscp_table,'key',DB_CLEAR,1);

$db_pubhst_table = new db_table('pub_history');
$db_pubhst_table->set_pk(['pid','lang','mark']);
$db_pubhst_table->new_reference('pid',$db_pub_table,'id',DB_NOTNULL);
$db_pubhst_table->new_reference('lang',$db_lang_table,'code');
$db_pubhst_table->new_column('mark','timestamp',DB_CURRENT);
$db_pubhst_table->new_column('edit','vartext',DB_CLEAR);
$db_pubhst_table->new_reference('format',$db_pubfmt_table,'key',DB_CLEAR,1);
$db_pubhst_table->new_column('diffetential','boolean',DB_NOTNULL,false);
$db_pubhst_table->new_reference('editor',$db_person_table,'id',DB_DEFNULL);

$db_pubnews_table = new db_table('news_item');
$db_pubnews_table->set_pk(['pid','lang']);
$db_pubnews_table->new_reference('pid',$db_pub_table,'id',DB_NOTNULL);
$db_pubnews_table->new_reference('lang',$db_lang_table,'code');
$db_pubnews_table->new_column('pub_date','timestamp',DB_CURRENT);
$db_pubnews_table->new_column('exp_date','timestamp',DB_CLEAR);
$db_pubnews_table->new_column('priority','tinyint',DB_NOTNULL,0);

$db_pubblog_table = new db_table('blog_post');
$db_pubblog_table->set_pk(['pid','lang']);
$db_pubblog_table->new_reference('pid',$db_pub_table,'id',DB_NOTNULL);
$db_pubblog_table->new_reference('lang',$db_lang_table,'code');
$db_pubblog_table->new_column('priority','tinyint',DB_NOTNULL,0);

$db_pubtag_table = new db_table('pub_tag');
$db_pubtag_table->set_pk(['pid','label']);
$db_pubtag_table->new_reference('pid',$db_pub_table,'id',DB_NOTNULL);
$db_pubtag_table->new_column('label','label40',DB_NOTNULL);

$db_pubcat_table = new db_table('pub_cat');
$db_pubcat_table->set_pk(['pid','label']);
$db_pubcat_table->new_reference('pid',$db_pub_table,'id',DB_NOTNULL);
$db_pubcat_table->new_column('label','label40',DB_NOTNULL);

class db_publication extends db_obj {
	static function retrieve($pid) {
		$publication = new self();
		db_obj::_retrieve($publication,$pid);
		$publication->load($pid);
		return $publication;
	}

	static function find_label($label) {
		$publication = new self();
		db_obj::_find_label($publication,$this,$label);
		$publication->load();
		return $publication;
	}

	static function create($label,...$params) {
		$publication = new self();
		db_obj::_create($publication,$label,'publication');
		return $publication;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('publication','id',$where,$orderby);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_publication::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['publication'] = db_table::$pool['publication'];
		db_obj::__construct();
	}

	function load($pid=null,...$p) {
		$id = (int)(is_null($pid)? $this->id(): $pid);
		if(isset($p[0]))
			$lang = $p[0];
		else {
			global $obj;
			$lang = isset($obj['site']['lang'])? $obj['site']['lang']: null;
		}
		$a = db_obj::$db->select_key('publication','lang',['id'=>"=$id"]);
		if(empty($a))
			return db_obj::log('publication::load',"'$id'","No publication yet with ID '$id'");
		if($lang) {
			if(empty($a[$lang]))
				return db_obj::log('publication::load',"'$id'","No publication yet with ID '$id' and language '$lang'");
			foreach(array_keys($this->fields['publication']->columns) as $field)
				$this->set($field, $a[$lang][$field]);
		}
		$lang = array_keys($a)[0];
		foreach(array_keys($this->fields['publication']->columns) as $field)
			$this->set($field, $a[$lang][$field]);
	}

	function save() {
		db_obj::save();
		$where = ['id'=>(int)$this->id(),'lang'=>$this->lang];
		$update = [];
		foreach(array_keys($this->fields['publication']->columns) as $field)
			if(!in_array($field,array_keys($where)) && isset($this->$field))
				$update[$field] = $this->$field;
		if(db_obj::$db->select_count('publication',$where))
			return db_obj::$db->update('publication',$update,$where);
		else
			return db_obj::$db->insert('publication',array_merge($where,$update));
	}

	function remove() {
		$where = ['id'=>(int)$this->id(),'lang'=>$this->lang];
		return db_obj::$db->delete('publication',$where) &&
			db_obj::remove();
	}
}

class db_blog_post extends db_publication {
	static function retrieve($pid) {
		$blog_post = new self();
		db_obj::_retrieve($blog_post,$pid);
		$blog_post->load($pid);
		return $blog_post;
	}

	static function find_label($label) {
		$blog_post = new self();
		db_obj::_find_label($blog_post,$this,$label);
		$blog_post->load();
		return $blog_post;
	}

	static function create($label,...$params) {
		$blog_post = new self();
		db_obj::_create($blog_post,$label,'blog_post');
		return $blog_post;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('publication','id',$where,$orderby);
		$bids = db_obj::$db->select_col('blog_post','pid');
		$ids = array_intersect($ids,$bids);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_blog_post::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['blog_post'] = db_table::$pool['blog_post'];
		db_publication::__construct();
	}

	function load($pid=null,...$p) {
		$id = (int)(is_null($pid)? $this->id(): $pid);
		db_publication::load($id,...$p);

		$a = db_obj::$db->select_first('blog_post','*',['pid'=>"=$id"]);
		if(empty($a))
			return db_obj::log('blog_post::load',"'$id'","No blog post yet with ID '$id'");
		foreach(array_keys($this->fields['blog_post']->columns) as $field)
			$this->set($field, $a[$field]);
	}

	function save() {
		db_publication::save();
		$where = ['pid'=>(int)$this->id(),'lang'=>$this->lang];
		$update = [];
		foreach(array_keys($this->fields['blog_post']->columns) as $field)
			if(!in_array($field,array_keys($where)) && isset($this->$field))
				$update[$field] = $this->$field;
		if(db_obj::$db->select_count('blog_post',$where))
			return db_obj::$db->update('blog_post',$update,$where);
		else
			return db_obj::$db->insert('blog_post',array_merge($where,$update));
	}

	function remove() {
		$where = ['pid'=>(int)$this->id(),'lang'=>$this->lang];
		return db_obj::$db->delete('blog_post',$where) &&
			db_obj::remove();
	}
}

class db_news_item extends db_publication {
	static function retrieve($pid) {
		$news_item = new self();
		db_obj::_retrieve($news_item,$pid);
		$news_item->load($pid);
		return $news_item;
	}

	static function find_label($label) {
		$news_item = new self();
		db_obj::_find_label($news_item,$label);
		$news_item->load();
		return $news_item;
	}

	static function create($label,...$params) {
		$news_item = new self();
		db_obj::_create($news_item,$label,'news_item');
		return $news_item;
	}

	static function get_list($where=null,$orderby=null) {
		$ids = db_obj::$db->select_col('publication','id',$where,$orderby);
		$nids = db_obj::$db->select_col('news_item','pid');
		$ids = array_intersect($ids,$nids);
		$ans = [];
		foreach($ids as $id)
			$ans[$id] = db_news_item::retrieve($id);
		return $ans;
	}

	function __construct() {
		$this->fields['news_item'] = db_table::$pool['news_item'];
		db_publication::__construct();
	}

	function load($pid=null,...$p) {
		$id = (int)(is_null($pid)? $this->id(): $pid);
		db_publication::load($id,...$p);

		$a = db_obj::$db->select_first('news_item','*',['pid'=>"=$id"]);
		if(empty($a))
			return db_obj::log('news_item::load',"'$id'","No news item yet with ID '$id'");
		foreach(array_keys($this->fields['news_item']->columns) as $field)
			$this->set($field, $a[$field]);
	}

	function save() {
		db_publication::save();
		$where = ['pid'=>(int)$this->id(),'lang'=>$this->lang];
		$update = [];
		foreach(array_keys($this->fields['news_item']->columns) as $field)
			if(!in_array($field,array_keys($where)) && isset($this->$field))
				$update[$field] = $this->$field;
		if(db_obj::$db->select_count('news_item',$where))
			return db_obj::$db->update('news_item',$update,$where);
		else
			return db_obj::$db->insert('news_item',array_merge($where,$update));
	}

	function remove() {
		$where = ['pid'=>(int)$this->id(),'lang'=>$this->lang];
		return db_obj::$db->delete('news_item',$where) &&
			db_obj::remove();
	}
}

?>
