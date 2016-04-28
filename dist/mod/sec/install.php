<?php
/* mod/sec/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle user security.
 */

echo "INSTALLING user security.\n";
require_once 'mod/sec/db.php';

$db->create('user_hash',true);
$db->create('user_status',true);
$db->create('user',true);

$formats = "0:plain;1:md5;2:sha256;100:sha256-100";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('user_hash',$updates,['key','label']);

$formats = "0:inactive;1:reserved;2:blocked;3:active";
$updates = explode(';',$formats);
array_walk($updates,'_explode');
$db->insert('user_status',$updates,['key','label']);

?>
