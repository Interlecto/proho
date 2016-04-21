<?php
/* mod/sec/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for security module.
 */

$ph_uri_case[] = ['{^login/?(\w+)()(?:\.(cgi|exe|php|aspx?))(\?|$)}',11,'mod/sec/sec.php','command'];
$ph_uri_case[] = ['{^login/?(\w+)()(?:\.(html|pl))(\?|$)}',11,'mod/sec/sec.php','form'];
$ph_uri_case[] = ['{^login\b\W*(\w+)?([^.]*)\.*(\w+)?(.*)(\?|$)}',10,'mod/sec/sec.php','login'];

?>
