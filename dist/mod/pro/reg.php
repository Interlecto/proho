<?php
/* mod/pro/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for profiles module.
 */

$ph_uri_case[] = ['{^perfil\b([^?]*)(\?|$)}',10,'mod/pro/pro.php','profile'];
$ph_uri_case[] = ['{^~(\w+)\b([^?]*)(\?|$)}',10,'mod/pro/pro.php','profile'];
$ph_uri_case[] = ['{^unidad\b([^?]*)(\?|$)}',10,'mod/pro/pro.php','flat'];

?>
