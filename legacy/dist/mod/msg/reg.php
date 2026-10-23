<?php
/* mod/msg/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for messaging module.
 */

$ph_uri_case[] = ['{^contacto\b([^?]*)(\?|$)}',10,'mod/rem/rem.php','contact'];
$ph_uri_case[] = ['{^mensaje\b([^?]*)(\?|$)}',10,'mod/rem/rem.php','mensaje'];

?>
