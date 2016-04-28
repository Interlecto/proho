<?php
/* mod/sec/sec.php
 * @author: Carlos Thompson
 *
 * Main implementation of the security module.
 */

require_once 'mod/sec/db.php';

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
	function prepare() {
		set_status(404, 'Secure login program not found.');
	}
};

$sec_next = [
	'in' => ['home.pl', 'login/login.pl'],
	'out' => ['', 'login/logout.pl'],
];
class page_command extends sec_page {
	function prepare() {
		global $sec_next;
		$prog = $this->_program;
		if(array_key_exists($prog,$sec_next)) {
			switch($prog) {
			case 'in':
				$user = $_POST['username'];
				$person = db_person::find_label($user);
				if($person->id()) {
					$_SESSION['level'] = 1;
					$_SESSION['username'] = $user;
					$_SESSION['full_name'] = $person->full_name();
					$_SESSION['given_name'] = $person->get('gname');
					$next = $sec_next[$prog][0];
				} else {
					$_SESSION['level'] = 0;
					$_SESSION['warning'] = "Usuario <code>$user</code> no encontrado.";
					$next = $sec_next[$prog][1];
				}
				break;
			case 'out':
				foreach($_SESSION as $key=>$val)
					unset($_SESSION[$key]);
				$_SESSION['level'] = 0;
				$next = $sec_next[$prog][0];
				break;
			default:
			}
			redirect(ltensure($next));
		} else
			set_status(404, "<p>Secure login program <code>$prog</code> not found.</p>");
	}
};

$sec_content = [
	'login' => "<p>Formulario de entrada.</p>",
	'registro' => "<p>Formulario de registro.</p>",
	'recuperar' => "<p>Formulario para recuperación de contraseña.</p>",
];

class page_form extends sec_page {
	function prepare() {
		global $sec_content;
		$form = $this->_program;
		if(array_key_exists($form,$sec_content))
			$this->set('content',$sec_content[$form]);
		else
			set_status(404, "<p>Secure login form <code>$form</code> not found.</p>");
	}
};

?>
