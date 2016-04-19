<?php
/* lib/tributer.php
 * @author: Carlos Thompson
 * 
 */

define('SET_REPLACE', 0);
define('SET_EMPTY', 1);
define('SET_UNSET', 2);
define('DEF_EMPTY', 1);
define('DEF_UNSET', 2);

function array_set(array &$array,$key,$value,$how=SET_REPLACE) {
	if($how==SET_REPLACE)
		$array[$key] = $value;
	elseif(!array_key_exists($key,$array))
		$array[$key] = $value;
	elseif(empty($array[$key]) && $how==SET_EMPTY)
		$array[$key] = $value;
}

function array_get(array $array,$key,$default,$how=DEF_UNSET) {
	if(!isset($array[$key])) return $default;
	if(empty($array[$key]) && $how==DEF_EMPTY) return $default;
	return $array[$key];
}

class tributer {
	public $_var = [];
	
	function __construct($vars) {
		foreach($vars as $k=>$v) {
			if(empty($k)) $this->_var = $v;
			else $this->$k = $v;
		}
	}
	
	function get($key,$default=null,$how = DEF_UNSET) {
		$k = explode('/',$key);
		$f = array_shift($k);
		if(method_exists($this,$m="get_$f")) {
			return $this->$m(implode(':',$k),$default,$how);
		}
		if(empty($k)) {
			if(array_key_exists($f,$this->_var))
				return array_get($this->_var,$f,$default,$how);
			if(method_exists($this,$f))
				return $this->$f();
			return $default;
		} elseif(isset($this->$f)) {
			if(is_array($this->$f)) {
				return array_get($this->$f,implode(':',$k),$default,$how);
			}
			if(is_a($this->$f,'tributer'))
				return $this->$f->get(implode('/',$k),$how);
			$g = array_shift($k);
			$s = implode(':',$k);
			if(method_exists($this->$f,$m = 'get_'.$g))
				return $this->$f->$m($s,$default,$how);
			if(method_exists($this->$f,$g))
				return $this->$f->$g($s);
			return $default;
		} else {
			$s = implode(':',$k);
			if(method_exists($this,$m='get_'.$f))
				return $this->$m($s,$default,$how);
			if(method_exists($this,$f))
				return $this->$f($s);
		}
		return $default;
	}

	function set($key,$value,$how = SET_REPLACE) {
		$k = explode('/',$key);
		$f = array_shift($k);
		if(empty($k)) {
			if($how == SET_REPLACE ||
				($how == SET_EMPTY && empty($this->_var[$f])) ||
				($how == SET_UNSET && !isset($this->_var[$f])))
				$this->_var[$f] = $value;
		} else {
			$s = implode(':',$k);
			if(!isset($this->$f)) {
				$this->$f = [$s=>$value];
			}
			else {
				array_set($this->$f,$s,$value,$how);
			}
		}
	}

}
?>
