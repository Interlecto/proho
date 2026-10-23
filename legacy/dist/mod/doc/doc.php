<?php
/* mod/doc/doc.php
 * @author: Carlos Thompson
 *
 * Main implementation of the documents module.
 */

require_once 'mod/doc/db.php';

$ns_translations = [
	'ayuda'=>'help',
];

class page_document extends Page {
	function __construct($environ) {
		Page::__construct($environ);
		global $ns_translations;
		$p = $this->get('line/parts');
		$this->_ns = $ns = isset($ns_translations[$p[1]])? $ns_translations[$p[1]]: $p[1];
		if(preg_match('{(\w[/\w]*)\b\.?(\w*)}',$p[2],$m)) {
			$this->_doc = $m[1];
			$this->_ext = $m[2];
		} else
			$this->_doc = 'index';
		if(empty($this->_ext)) $this->_ext='html';
	}
	function prepare() {
		$this->_label = $label = $this->_ns.'_'.str_replace('/','_',$this->_doc);
		$this->_document = $doc = db_document::find_label($label);
		$this->set('title',$doc->get_label($label));
	}
	function content() {
		return $this->_document->get('body');
	}
}

?>
