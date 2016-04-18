<?php
/* lib/page.php
 * @author: Carlos Thompson
 * 
 */

$server = include 'server.php';

class Page {
	function __construct($enviro) {
		$this->enviro = $enviro;
	}
	
	function go() {
		echo "<pre>";
		print_r($this);
		echo "</pre>\n";
	}
	
	function close() {
	}
};

return new Page($server);
?>
