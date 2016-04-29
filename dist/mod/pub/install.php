<?php
/* mod/pub/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle publications.
 */

$t = microtime(true);
echo "\nINSTALLING publications.\n";
require_once 'mod/pub/db.php';

(new db_module('pub',4))->save();
(db_namespace::create('blog',2))->save();
(db_namespace::create('news',1))->save();

//$db->create('pub_format',true);
$db->create('pub_status',true);
//$db->create('pub_scope',true);
$db->create('publication',true);
$db->create('pub_history',true);
$db->create('news_item',true);
$db->create('blog_post',true);
//$db->create('pub_tag',true);
$db->create('pub_cat',true);
/*
$formats = "0:plain_text;1:markdown;2:html;3:html+braces;4:ilm;5:mediawiki;6:wordpress";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('pub_format',$updates,['key','label']);
/**/
$pstatus = "0:unplublished;1:pending_review;2:published;3:outdated;4:retired";
$updates = explode(';',$pstatus);
array_walk($updates,'_explode');
$db->insert('pub_status',$updates,['key','label']);
/*
$scope = "0:private;1:protected;2:public";
$updates = explode(';',$scope);
array_walk($updates,'_explode');
$db->insert('pub_scope',$updates,['key','label']);
/**/
echo 'Took '.(microtime(true)-$t)."s\n";
?>
