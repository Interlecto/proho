<?php
/* mod/pro/pro.php
 * @author: Carlos Thompson
 *
 * Main implementation of the profiles module.
 */

require_once 'mod/pro/db.php';

class page_profile extends Page {
	function __construct($enviro) {
		Page::__construct($enviro);
		$p = $this->get('line/parts');
		$user = strtolower($p[1]);
		check_redirect("~$user");
		$this->_user = $user;
		$this->_person = db_person::find_label($user);
	}

	function prepare() {
		$user = $this->_user;
		$person = $this->_person;

		if(empty($person) || $person->id()==0)
			set_status(404,"El usuario ‘{$user}’ no fue encontrado.");
		else
			$this->set('title',$person->full_name());
	}

	function content() {
		$s="\n<table>\n";
		foreach(get_object_vars($this->_person) as $i=>$j)
			$s.="\t<tr><th>$i</th><td>$j</td></tr>\n";
		$s.="</table>\n";
		return $s;
	}
}

class page_profile_current extends page_profile {
	function __construct($enviro) {
		Page::__construct($enviro,true);
		$this->_user = $user = isset($_SESSION['username'])? $_SESSION['username']: 'root';
		$this->_person = db_person::find_label($user);
	}
}

class page_profile_script extends Page {
	function __construct($enviro) {
		Page::__construct($enviro,true);
		$p = $this->get('line/parts');
		if($p[1]=='/')
			$user = isset($_SESSION['username'])? $_SESSION['username']: 'root';
		else
			$user = strtolower($p[1]);
		$this->_user = $user;
		$this->_uri = $p[2];
		$this->_person = db_person::find_label($user);
	}

	function prepare() {
		$user = $this->_user;
		$person = $this->_person;

		if(empty($person) || $person->id()==0)
			set_status(404,"El usuario ‘{$user}’ no fue encontrado.");
		else
			$this->set('title',$person->full_name());
	}
}

class page_profile_mf extends Page {
	function prepare() {
		set_status(404,'Esquema <code>'.$this->get('line/line').'</code> malformado.');
	}
}

?>
