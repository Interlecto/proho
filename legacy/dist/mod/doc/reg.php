<?php
/* mod/doc/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for documents module.
 */

$ph_uri_case[] = ['{^(ayuda|info|documento)\b([^?]*)(\?|$)}',10,'mod/doc/doc.php','document'];
$ph_uri_case[] = ['{^status/(\d\d\d)(\?|$)}',10,'mod/doc/status.php','status'];
$ph_uri_case[] = ['{^tea(\?|$)}',10,'mod/doc/status.php','418'];
require_once 'mod/doc/status.php';

?>
