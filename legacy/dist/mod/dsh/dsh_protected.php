<?php
/* mod/dsh/dsh_public.php
 * @author: Carlos Thompson
 * 
 * Dashboard for logged in users.
 */

class user_dash extends page_dashboard {
	function __construct($enviro) {
		Page::__construct($enviro);
		$this->set('title','ProHo - Escritorio');
	}

	function content() {
		return "Escritorio privado\n".'<pre>'.print_r($this->line,true).'</pre>';
	}
}

?>
