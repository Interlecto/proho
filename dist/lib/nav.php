<?php
/* lib/page.php
 * @author: Carlos Thompson
 * 
 */

require_once 'lib/page.php';

$ph_ext_alias = [
	'js'=>'javascript',
	'jpg'=>'jpeg',
	];
foreach(['css','png','jpeg','ico','md'] as $ext)
	$ph_ext_alias[$ext] = $ext;
class NavPage extends Page {
	function go() {
		if(is_public())
			$this->set('skin/template','clean');
			
		$this->_root = $root = rtrim($this->get('dir/root'),'/');
		$this->_path = $path = trim($this->get('line/line'),'/');
		if(is_dir($d="$root/$path")) {
			$site = $this->get('site/name',$this->get('server/name'));
			$this->set('title',"$site /$path");
			
			$this->_dir = dir($d);
		} else {
			set_status(404,"<p>Archivo <code>$d</code> no encontrado.</p>");
		}

		Page::go();
		
		if(isset($this->_dir))
			$this->_dir->close();
	}
	function content() {
		global $ph_ext_alias;

		if(!isset($this->_dir)) return '<p>Archivo no existente</p>';
		$root = $this->_root;
		$path = $this->_path;
		$s = "<ul>\n";
		$e = [-1=>"<li class=parent><a href=\"/$path/..\">.. (subir)</a></li>"];
		while(false !== ($entry = $this->_dir->read())) {
			if(substr($entry,0,1)=='.') continue;
			if(is_dir("$root/$path/$entry"))
				$e["0-$entry"] = "<li class=dir><a href=\"/$path/$entry/\">$entry/</a></li>\n";
			else {
				$pi = pathinfo($entry);
				$ext = strtolower($pi['extension']);
				if(isset($ph_ext_alias[$ext])) {
					$cl = $ph_ext_alias[$ext];
					$e["1-$entry"] = "<li class=$cl><a href=\"/$path/$entry\">$entry</a></li>\n";
				}
			}
		}
		ksort($e);
		$s.= implode("\n",$e);
		$s.= "</ul>\n";
		return $s;
	}
}

return new NavPage($server);
?>
