<?php
/* mod/dsh/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for dashboard module.
 */

$ph_uri_case[] = ['{escritorio\b(.*)}',10,'mod/dsh/dsh.php','dashboard'];
$ph_uri_case[] = ['{publico\b(.*)}',10,'mod/dsh/dsh.php','public'];

?>
