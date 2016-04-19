<?php
/* mod/dsh/dsh_public.php
 * @author: Carlos Thompson
 * 
 * Dashboard for unregistered users.
 */

class page_home extends Page {
	function __construct($enviro) {
		Page::__construct($enviro);
		$this->set('title','ProHo');
		$this->set('skin/template','flat');
	}
	
	function content() {
		return "Escritorio Público\n".'<pre>'.print_r($this->line,true).'</pre>';
	}
}

?>
