<?php
/* mod/pub/pub.php
 * @author: Carlos Thompson
 *
 * Main implementation of the publications module.
 */

require_once 'mod/pub/db.php';

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
		$this->set('title','Blog');
		$this->_entries = db_blog_post::get_list();
	}

	function write_item($id, $item) {
		$db = $this->db;
		$author = db_person::retrieve($item->get('author'));
		$label = $item->label();
		$title = $item->get_label($label);
		$path = date('Y/m/d', $item->get('date'));
		$tags = $db->select_col('pub_tag','label',['pid'=>"=$id"]);
		$cats = $db->select_col('pub_cat','label',['pid'=>"=$id"]);

			ob_start()?>
			<section class=blog-item>
				<header class=blog-header>
					<h2><a href="/<?=$path?>/<?=$label?>"><?=$title?></a></h2>
				</header>
				<div class=blog-body>
					<?=$item->get('body')?>
				</div>
				<footer class=blog-footer>
					<p>Por <a href="/blog/por/<?=$author->label()?>"><?=$author->full_name()?></a>.</p>
					<p>Etiquetas: <?php
					$s=[];
					foreach($tags as $tag) {
						$name = db_obj::get_label($tag);
						$s[] = "<a href=\"/blog/tag/$tag\">#$name</a>";
					}
					echo empty($s)? 'ninguna': implode(', ',$s);
					?>.</p>
					<p>Categoría: <?php
					$s=[];
					foreach($cats as $cat) {
						$name = db_obj::get_label($cat);
						$s[] = "<a href=\"/blog/cat/$cat\">$name</a>";
					}
					echo empty($s)? 'ninguna': implode(', ',$s);
					?>.</p>
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
		foreach($entries as $id=>$item)
			if($this->include_entry($item)) {
				$s.= $this->write_item($id, $item);
				if(++$count >= 10) break;
			}
	/*	foreach(db_table::$pool as $tn=>$td)
			$s.= "<pre>\n".$td->mysql_create(db_mysql::$first)."</pre>\n";/**/
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
		$db = $this->db;
		$match = $this->_tag_match;

		switch($this->_tag_type) {
		case 'cat':
			$this->set('title', 'Blog: Categoría <code>'.$match.'</code>');
			$this->_cats = $db->select_col('pub_cat','pid',['label'=>$match]);
			break;
		case 'tag':
			$this->set('title', 'Blog: Etiqueta <code>'.$match.'</code>');
			$this->_tags = $db->select_col('pub_tag','pid',['label'=>$match]);
			break;
		case 'aut':
		case 'por':
			$this->_author = $author = db_person::find_label($match);
			$this->_author_id = $author->id();
			$this->set('title', 'Blog: entradas de '.$author->full_name().'');
			break;
		default:
		}
	}

	function include_entry($item) {
		switch($this->_tag_type) {
		case 'cat':
			return in_array($item->id(), $this->_cats);
		case 'tag':
			return in_array($item->id(), $this->_tags);
		case 'aut':
		case 'por':
			return $item->get('author') == $this->_author_id;
		default:
			return true;
		}
	}
};

class page_blogpost extends Page {
	function prepare() {
		$db = $this->db;
		$p = $this->get('line/parts');
		if(count($p)<5)
			$this->_item = $item = db_blog_post::retrieve($p[1]);
		else
			$this->_item = $item = db_news_item::find_label($p[4]);
		if(empty($item))
			set_status(404,"<p>Artículo <code>{$p[0]}</code> no encontrado.</p>");
		else
			$this->set('title',$item->get_label($item->label()));
	}

	function content() {
		$db = $this->db;
		$item = $this->_item;
		$author = db_person::retrieve($item->get('author'));
		ob_start()?>
			<div class=blog-body>
				<?=$item->get('body')?>
			</div>
			<footer class=blog-footer>
				Por <a href="/blog/por/<?=$author->label()?>"><?=$author->full_name()?></a>.
			</footer>
<?php	return ob_get_clean();
	}
};

class page_news extends page_blog {
	function prepare() {
		$this->set('title','Noticias');
		$this->_entries = db_news_item::get_list();
	}

	function write_item($id, $item) {
		$db = $this->db;
		$label = $item->label();
		$title = $item->get_label($label);
		$tags = $db->select_col('pub_tag','label',['pid'=>"=$id"]);
		$cats = $db->select_col('pub_cat','label',['pid'=>"=$id"]);
			ob_start()?>
			<section class=news-item>
				<header class=news-header>
					<h2><a href="/noticias/<?=$label?>"><?=$title?></a></h2>
				</header>
				<div class=news-body>
					<?=$item->get('body')?>
				</div>
				<footer class=news-footer>
					<p>Etiquetas: <?php
					$s=[];
					foreach($tags as $tag) {
						$name = db_obj::get_label($tag);
						$s[] = "<a href=\"/noticias/tag/$tag\">#$name</a>";
					}
					echo empty($s)? 'ninguna': implode(', ',$s);
					?>.</p>
					<p>Categoría: <?php
					$s=[];
					foreach($cats as $cat) {
						$name = db_obj::get_label($cat);
						$s[] = "<a href=\"/noticias/cat/$cat\">$name</a>";
					}
					echo empty($s)? 'ninguna': implode(', ',$s);
					?>.</p>
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
		$db = $this->db;
		$match = $this->_tag_match;

		switch($this->_tag_type) {
		case 'cat':
			$this->set('title', 'Noticias: Categoría <code>'.$match.'</code>');
			$this->_cats = $db->select_col('pub_cat','pid',['label'=>$match]);
			break;
		case 'tag':
			$this->set('title', 'Noticias: Etiqueta <code>'.$this->_tag_match.'</code>');
			$this->_tags = $db->select_col('pub_tag','pid',['label'=>$match]);
			break;
		default:
		}
	}

	function include_entry($item) {
		switch($this->_tag_type) {
		case 'cat':
			return in_array($item->id(), $this->_cats);
		case 'tag':
			return in_array($item->id(), $this->_tags);
		default:
			return true;
		}
	}
};

class page_newsitem extends page_blogpost {
	function prepare() {
		$db = $this->db;
		$p = $this->get('line/parts');
		if($p[1]=='/')
			$this->_item = $item = db_news_item::find_label($p[2]);
		else
			$this->_item = $item = db_news_item::retrieve($p[1]);
		if(empty($item))
			set_status(404,"<p>Artículo <code>{$p[0]}</code> no encontrado.</p>");
		else
			$this->set('title',$item->get_label($item->label()));
	}

	function content() {
		$item = $this->_item;
		ob_start()?>
			<div class=news-body>
				<?=$item->get('body')?>
			</div>
<?php	return ob_get_clean();
	}
};

?>
