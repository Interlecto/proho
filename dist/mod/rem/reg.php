<?php
/* mod/rem/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for comments module.
 */

$ph_uri_case[] = ['{()comentario\b(.*)}',10,'mod/rem/rem.php','comment'];
$ph_uri_case[] = ['{~(\w+)/comentario\b(.*)}',11,'mod/rem/rem.php','comment'];

?>
