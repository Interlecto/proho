<?php
/* mod/sec/sec.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the security module.
 */

$sec_convs = [
	'{^reg}' => 'registro',
];
$sec_convx = [
	'{^pl}' => 'pl',
	'{^x?htm}' => 'html',
	'{^(cg|exe|asp)}' => 'cgi',
	'{.*}' => 'pl',
];

class sec_page extends Page {
	function __construct($enviro) {
		global $sec_convs, $sec_convx;
		$line = $enviro['line']['line'];
		$parts = $enviro['line']['parts'];
		foreach($sec_convs as $pat=>$conv) {
			if(preg_match($pat,$parts[1])) {
				$prog = $conv;
				break;
			}
		}
		foreach($sec_convx as $pat=>$conv) {
			if(preg_match($pat,$parts[3])) {
				$ext = $conv;
				break;
			}
		}
		if(!isset($ext)) $ext = $parts[3];
		if(!isset($prog)) $prog = $parts[1];
		check_redirect("login/$prog.$ext",$line);
		Page::__construct($enviro);
		$this->_program = $prog;
	}
};

class page_login extends sec_page {
	function go() {
		set_status(404, 'Secure login program not found.');
		sec_page::go();
	}
};

$sec_next = [
	'in' => ['home.pl', 'login/login.pl'],
	'out' => ['', 'login/logout.pl'],
];
class page_command extends sec_page {
	function go() {
		global $sec_next;
		$prog = $this->_program;
		if(array_key_exists($prog,$sec_next)) {
			$_SESSION['level'] = $prog!='out';
			redirect(ltensure($sec_next[$prog][0]));
		}
		set_status(404, "<p>Secure login program <code>$prog</code> not found.</p>");
		sec_page::go();
	}
};

$sec_content = [
	'registro' => "<p>Formulario de registro.</p>",
	'recuperar' => "<p>Formulario para recuperación de contraseña.</p>",
];

class page_form extends sec_page {
	function go() {
		global $sec_content;
		$form = $this->_program;
		if(array_key_exists($form,$sec_content))
			$this->set('content',$sec_content[$form]);
		else
			set_status(404, "<p>Secure login form <code>$form</code> not found.</p>");
		sec_page::go();
	}
};

?>
