<?php
/* lib/page.php
 * @author: Carlos Thompson
 * 
 */

require_once 'lib/lib.php';
require_once 'lib/tributer.php';
class Page extends tributer {
	static $first;
	
	function __construct($enviro) {
		tributer::__construct($enviro);
		#$this->enviro = $enviro;
		$this->set('skin/name','default',SET_REPLACE);
		$this->set('skin/template', is_public()? 'clean': 'normal',SET_UNSET);
		$this->set('skin/format','html',SET_UNSET);
		$this->set('skin/version',5,SET_UNSET);
		Page::$first = $this;
	}
	
	function go() {
		$s = $this->get_template($k,$t,$f,$v);
		$this->set('dir/skin',"/skins/$k");
		$this->$f($s,$v);
	}
	
	function empty_template() {
		return <<<BLOQUE
<!DOCTYPE html>
<html>
<head>
<meta charset=utf-8>
<title>ProHo</title>
</head>
<body>
{content}
</body>
</html>
BLOQUE;
	}
	
	function get_template(&$skin=null, &$template=null, &$format=null, &$version=null) {
		if(is_null($skin)) $skin = $this->get('skin/name');
		if(is_null($template)) $template = $this->get('skin/template');
		if(is_null($format)) $format = $this->get('skin/format');
		if(is_null($version)) $version = $this->get('skin/version');

		$root = $this->get('dir/root');
		$d="{$root}skins/$skin";
		if(!is_dir($d)) {
			if($skin=='default')
				return $this->empty_template();
			$skin = 'default';
			return $this->get_template($skin, $template, $format, $version);
		}
		
		if(file_exists($fn="$d/$template.$format.php"))
			return include $fn;
		if(file_exists($fn="$d/$template.$format"))
			return file_get_contents($fn);
		if(file_exists($fn="$d/$template.$format.html")) {
			$format = html;
			return file_get_contents($fn);
		}
		
		if($template=='normal') {
			if(file_exists($fn="$d/$format.php"))
				return include $fn;
			if(file_exists($fn="$d/index.$format"))
				return file_get_contents($fn);
			if(file_exists($fn="$d/$format.html")) {
				$format = html;
				return file_get_contents($fn);
			}
			return $this->empty_template();
		}
		
		$template = 'normal';
		return $this->get_template($skin, $template, $format, $version);
	}

	function html($template, $version) {
		echo temp2html($template, $version);
		#echo str_replace('{content}',$this->content(),$template);
	}
	
	function content() {
		ob_start() ?>
			<h2><?=ucwords($this->line['class'])?></h2>
<pre><strong>Line:</strong> <?php print_r($this->line)?></pre>
<pre><strong>Session:</strong> <?php print_r($this->session)?></pre>
<?php
		return ob_get_clean();
	}
	
	function close() {
	}
	
	function get_title($wc=null,$def=null,$how=DEF_UNSET) {
		if(isset($this->_var['title']))
			return $this->_var['title'];
		return $this->get('site/name',$this->get('server/name',$def,$how),$how);
	}
	
	function get_area($area, $def='', $how=DEF_UNSET) {
		$skin = $this->get('skin/name');
		$root = rtensure($this->get('dir/root'),'/');
		
		$paths = ["skins/$skin/areas", "plugs/areas", "skins/$skin", "plugs"];
		foreach($paths as $p) {
			if(file_exists($fn="$root$p/$area.php"))
				return include $fn;
			if(file_exists($fn="$root$p/$area.html"))
				return temp2html(file_get_contents($fn));
		}
		return $def;
	}
};

$server = include 'server.php';

function ph_get($key, $def=null, $how=DEF_EMPTY) { return Page::$first->get($key, $def, $how); }
function ph_set($key, $val, $how=SET_REPLACE) { return Page::$first->set($key, $val, $how); }
function ph_empty($key) { return Page::$first->isempty($key); }
function ph_isset($key) { return Page::$first->exits($key); }

if(isset($server['line']['module']))
	require_once $server['line']['module'];

if(isset($ph_NAV))
	return;

$cl = isset($server['line']['class'])? $server['line']['class']: null;
if(class_exists($class="page_$cl"))
	return new $class($server);

return new Page($server);
?>
