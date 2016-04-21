<?php
/* mod/pub/pub.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the publications module.
 */

$blogs = [
	1=>[
		'title'=>'La importancia del verde',
		'body'=>'Aun cuando la política del edificio...',
		'author'=>'Jairo, del 509',
		'author-tag'=>'jairo',
	],
	2=>[
		'title'=>'Un año de logros',
		'body'=>'Señores copropietarios. Les presento...',
		'author'=>'Administración',
		'author-tag'=>'admin',
	],
	3=>[
		'title'=>'Los cerramientos y el modelo de ciudad',
		'body'=>'En los últimos años hemos venido viendo cómo...',
		'author'=>'Esteban, del 105',
		'author-tag'=>'esteban',
	],
];
class page_blog extends Page {
	function __construct($enviro,$inherit=false) {
		if(!$inherit) {
			$p = $enviro['line']['parts'];
			if(preg_match('{^/?(index|home|inicio)?(?:\.\w+)?(\?|$)}',$p[2],$m))
				check_redirect(rtensure($p[1]));
			else
				$this->_status = [ 404, "<p>Recurso <code>{$p[2]}</code> no encontrado en {$p[1]}. Esquema mal formado.</p>" ];
		}
		Page::__construct($enviro);
	}
	
	function prepare() {
		global $blogs;
		if(empty($this->entries))
			$this->_entries = $blogs;
		$this->set('title','Blog');
	}
	
	function write_item($n, $item) {
			ob_start()?>
			<section class=blog-item>
				<header class=blog-header>
					<h2><a href="/blog/<?=$n?>"><?=$item['title']?></a></h2>
				</header>
				<div class=blog-body>
					<?=$item['body']?>
				</div>
				<footer class=blog-footer>
					Por <a href="/blog/por/<?=$item['author-tag']?>"><?=$item['author']?></a>.
				</footer>
			</section>
<?php		return ob_get_clean();
	}
	
	function include_entry($item) {
		return true;
	}
	
	function content() {
		$entries = $this->_entries;
		$s = '';
		$count = 0;
		foreach($entries as $n=>$item)
			if($this->include_entry($item)) {
				$s.= $this->write_item($n, $item);
				if(++$count >= 10) break;
			}
		return $s;
	}
};

class page_blogtag extends page_blog {
	function __construct($enviro) {
		$p = $enviro['line']['parts'];
		$this->_tag_type = substr($p[1],0,3);
		$this->_tag_match = trim($p[2],'/');
		page_blog::__construct($enviro,true);
	}
	
	function prepare() {
		page_blog::prepare();
		switch($this->_tag_type) {
		case 'cat':
			$this->set('title', 'Blog: Categoría <code>'.$this->_tag_match.'</code>');
			break;
		case 'tag':
			$this->set('title', 'Blog: Etiqueta <code>'.$this->_tag_match.'</code>');
			break;
		case 'aut':
		case 'por':
			$this->set('title', 'Blog: Por <em>'.$this->_tag_match.'</em>');
			break;
		default:
		}
	}

	function include_entry($item) {
		switch($this->_tag_type) {
		case 'cat':
			return $item['cat'] == $this->_tag_match;
		case 'tag':
			return in_array($this->_tag_match, $item['tags']);
		case 'aut':
		case 'por':
			return $item['author-tag'] == $this->_tag_match;
		default:
			return true;
		}
	}
};

class page_blogpost extends Page {
	function prepare() {
		global $blogs;
		$p = $this->get('line/parts');
		if(count($p)<5) {
			$n = (int)$p[1];
			if(isset($blogs[$n]))
				$this->_item = $blogs[$n];
		} else {
			$ref = $p[4];
			foreach($blogs as $n=>$item)
				if(isset($item['refs']) && in_array($ref, $item['refs'])) {
					$this->_item = $item;
					break;
				}
		}
		if(empty($this->_item))
			set_status(404,"<p>Artículo <code>{$p[0]}</code> no encontrado.</p>");
		else
			$this->set('title',$this->_item['title']);
	}
	
	function content() {
		$item = $this->_item;
		ob_start()?>
			<div class=blog-body>
				<?=$item['body']?>
			</div>
			<footer class=blog-footer>
				Por <a href="/blog/por/<?=$item['author-tag']?>"><?=$item['author']?></a>.
			</footer>
<?php	return ob_get_clean();
	}
};

$noticias = [
	1=>[
		'title'=>'Cuota extaordinaria',
		'body'=>'La asamblea aprobó una cuota extraordinaria $20.000.000, repartida por el coeficiente de copropiedad, para el arreglo de zonas comunes.',
		'tags'=>['plata', 'cuota'],
	],
	2=>[
		'title'=>'Reglamento sobre mascotas',
		'body'=>'El Concejo Distrital aprobó una nueva norma sobre mascotas en propiedad horizontal.',
		'tags'=>['mascotas'],
	],
	3=>[
		'title'=>'Nuevo revisor fiscal',
		'body'=>'En asamblea se nombró a la señora Pepa Pérez como nueva revisora fiscal.',
		'tags'=>['nombramientos','plata'],
	],
];
class page_news extends page_blog {
	function prepare() {
		global $noticias;
		if(empty($this->entries))
			$this->_entries = $noticias;
		$this->set('title','Noticias');
	}
	
	function write_item($n, $item) {
			ob_start()?>
			<section class=news-item>
				<header class=news-header>
					<h2><a href="/noticias/<?=$n?>"><?=$item['title']?></a></h2>
				</header>
				<div class=news-body>
					<?=$item['body']?>
				</div>
				<footer class=news-footer>
					Etiquetas: <?php
					$s=[];
					foreach($item['tags'] as $tag)
						$s[] = "<a href=\"/noticias/tag/$tag\">#$tag</a>";
					echo implode(', ',$s);
					?>
				</footer>
			</section>
<?php		return ob_get_clean();
	}
};

class page_newstag extends page_news {
	function __construct($enviro) {
		$p = $enviro['line']['parts'];
		$this->_tag_type = substr($p[1],0,3);
		$this->_tag_match = trim($p[2],'/');
		page_blog::__construct($enviro,true);
	}

	function prepare() {
		page_news::prepare();
		switch($this->_tag_type) {
		case 'cat':
			$this->set('title', 'Blog: Categoría <code>'.$this->_tag_match.'</code>');
			break;
		case 'tag':
			$this->set('title', 'Blog: Etiqueta <code>'.$this->_tag_match.'</code>');
			break;
		default:
		}
	}

	function include_entry($item) {
		switch($this->_tag_type) {
		case 'cat':
			return $item['cat'] == $this->_tag_match;
		case 'tag':
			return in_array($this->_tag_match, $item['tags']);
		default:
			return true;
		}
	}
};

class page_newsitem extends page_blogpost {
	function prepare() {
		global $noticias;
		$p = $this->get('line/parts');
		if($p[1]=='/') {
			$ref = $p[2];
			foreach($noticias as $n=>$item)
				if(isset($item['refs']) && in_array($ref, $item['refs'])) {
					$this->_item = $item;
					break;
				}
		} else {
			$n = (int)$p[1];
			if(isset($noticias[$n]))
				$this->_item = $noticias[$n];
		}
		if(empty($this->_item))
			set_status(404,"<p>Artículo <code>{$p[0]}</code> no encontrado.</p>");
		else
			$this->set('title',$this->_item['title']);
	}
	
	function content() {
		$item = $this->_item;
		ob_start()?>
			<div class=news-body>
				<?=$item['body']?>
			</div>
<?php	return ob_get_clean();
	}
};

?>
