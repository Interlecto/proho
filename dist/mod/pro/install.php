<?php
/* mod/pro/install.php
 * @author: Carlos Thompson
 *
 * Creates environment and database entries to handle user profiles.
 */

echo "INSTALLING user profiles.\n";
require_once 'mod/pro/db.php';

$db->create('person',true);

?>
