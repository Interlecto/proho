<?php
/* mod/pmd/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for markdown module.
 */

$ph_uri_case[] = ['{(.*?)\bmd\b(?:\.(\w+))?/?([^?]*)(\?|$)}',10,'mod/pmd/pmd.php','mdviewer'];

?>
