<?php
/* mod/pub/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for publication module.
 */

$ph_uri_case[] = ['{^(noticias)\b([^?]*)(\?|$)}',10,'mod/pub/pub.php','news'];
$ph_uri_case[] = ['{^noticias/(\d+)(\?|$)}',12,'mod/pub/pub.php','newsitem'];
$ph_uri_case[] = ['{^noticias(/)([-\w]+)(\?|$)}',11,'mod/pub/pub.php','newsitem'];
$ph_uri_case[] = ['{^noticias/(tag|cat\w+)\b([^?]*)(\?|$)}',12,'mod/pub/pub.php','newstag'];
$ph_uri_case[] = ['{^(blog)\b([^?]*)(\?|$)}',10,'mod/pub/pub.php','blog'];
$ph_uri_case[] = ['{^blog/(\d+)(\?|$)}',11,'mod/pub/pub.php','blogpost'];
$ph_uri_case[] = ['{^(\d{4})(?:[-/](\d{2}))?(?:[-/](\d{2}))?/([-\w]+)(\?|$)}',11,'mod/pub/pub.php','blogpost'];
$ph_uri_case[] = ['{^blog/(tag|cat\w*|por|aut\w*)\b([^?]*)(\?|$)}',11,'mod/pub/pub.php','blogtag'];

?>
