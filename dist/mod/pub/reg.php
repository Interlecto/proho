<?php
/* mod/pub/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for publication module.
 */

$ph_uri_case[] = ['{noticias\b(.*)}',10,'mod/pub/pub.php','news'];
$ph_uri_case[] = ['{blog\b(.*)}',10,'mod/pub/pub.php','blog'];

?>
