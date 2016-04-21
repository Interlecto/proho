<?php
/* mod/pmd/pmd.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the markdown module.
 */

require_once 'mod/pmd/Parsedown.php';

class page_mdviewer extends Page {
	function __construct($enviro) {
		$p = $enviro['line']['parts'];
		if(count($p)<4) $p[3] = '';
		$can = 'cgi/md.cgi'.ltensure($p[3]);
		check_redirect($can);
		$this->_file = $p[3];
		Page::__construct($enviro);
	}

	function go() {
		$file = ltrim($this->_file);
		$root = rtensure($this->get('dir/root'));
		if(file_exists($fn=$root.$file) && !is_dir($fn)) {
			$PD = new Parsedown();
			$md = file_get_contents($fn);
			$this->_content = $PD->text($md);
			$this->set('title', $file);
		} else {
			set_status(404,"<p>El archivo <code>$file</code> no fue encontrado.</p>");
		}
		Page::go();
	}
	
	function content() {
		return $this->_content;
	}
}

?>
