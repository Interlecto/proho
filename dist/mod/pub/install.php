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

$db->create('pub_status',true);
$db->create('publication',true);
$db->create('pub_history',true);
$db->create('news_item',true);
$db->create('blog_post',true);
$db->create('pub_cat',true);

$pstatus = "0:unplublished;1:pending_review;2:published;3:outdated;4:retired";
$updates = explode(';',$pstatus);
array_walk($updates,'_explode');
$db->insert('pub_status',$updates,['key','label']);

echo 'Took '.(microtime(true)-$t)."s\n";
?>
