<?php
/* mod/pro/reg.php
 * @author: Carlos Thompson
 *
 * Entry points for profiles module.
 */


///  profile/user/script.ext >> script
///  profile/script.ext >> script (applies to current user)
///  profile/user >> user profile
///  profile/ >> current user profile
///  profile/me.ext >> current user profile
///  ~user >> user profile
///  ~user/script.ext >> script

$ph_uri_case[] = ['{^(?:perfil|profile)/?(\?|$)}',11,'mod/pro/pro.php','profile_current'];
$ph_uri_case[] = ['{^(?:perfil|profile)/(me|yo|index)\.(\w+)(\?|$)}',12,'mod/pro/pro.php','profile_current'];
$ph_uri_case[] = ['{^(?:perfil|profile)(/)(\w+\.\w+)(\?|$)}',11,'mod/pro/pro.php','profile_script'];
$ph_uri_case[] = ['{^(?:perfil|profile)/(\w+)(\?|$)}',12,'mod/pro/pro.php','profile'];
$ph_uri_case[] = ['{^(?:perfil|profile)/(\w+)/([^?]*)(\?|$)}',11,'mod/pro/pro.php','profile_script'];
$ph_uri_case[] = ['{^(?:perfil|profile)\b([^?]*)(\?|$)}',10,'mod/pro/pro.php','profile_mf'];
$ph_uri_case[] = ['{^~(\w+)(\?|$)}',10,'mod/pro/pro.php','profile'];
$ph_uri_case[] = ['{^~(\w+)/([^?]*)(\?|$)}',10,'mod/pro/pro.php','profile_script'];
$ph_uri_case[] = ['{^unidad\b([^?]*)(\?|$)}',10,'mod/pro/pro.php','flat'];

?>
