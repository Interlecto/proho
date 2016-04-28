<?php
/* mod/sec/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle user security.
 */

$t = microtime(true);
echo "\nINSTALLING user security.\n";
require_once 'mod/sec/db.php';

(new db_module('sec',3))->save();
$db->create('user_hash',true);
$db->create('user_status',true);
$db->create('user',true);
$db->create('user_level',true);
$db->create('user_permit',true);

$formats = "0:plain;1:md5;2:sha256;100:sha256-100";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('user_hash',$updates,['key','label']);

$formats = "0:inactive;1:reserved;2:blocked;3:active";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('user_status',$updates,['key','label']);

$formats = "0:forbiden;1:view_basic;2:view_all;3:edit_basic;4:edit_all;5:create;9:all";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('user_level',$updates,['key','label']);

echo 'Took '.(microtime(true)-$t)."s\n";
?>
