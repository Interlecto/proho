<?php
/* mod/pro/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle user profiles.
 */

$t = microtime(true);
echo "\nINSTALLING user profiles.\n";
require_once 'mod/pro/db.php';

(new db_module('pro',2))->save();
$db->create('person',true);

echo 'Took '.(microtime(true)-$t)."s\n";
?>
