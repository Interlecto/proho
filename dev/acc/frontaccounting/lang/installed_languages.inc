<?php

/* How to make new entries here for non-packaged languages:

-- 'code' should match the name of the directory for the language under \lang
.-- 'name' is the name that will be displayed in the language selection list (in Users and Display Setup)
-- 'rtl' only needs to be set for right-to-left languages like Arabic and Hebrew
-- 'encoding' used in translation file
-- 'version' always set to '' for manually installed languages.
-- 'path' installation path related to FA root (e.g. 'lang/en_US').
*/


$installed_languages = array (
  0 => 
  array (
    'code' => 'C',
    'name' => 'English',
    'encoding' => 'iso-8859-1',
  ),
  1 => 
  array (
    'name' => 'English (US)',
    'package' => 'en_US',
    'code' => 'en_US',
    'encoding' => 'iso-8859-1',
    'version' => '2.3.12-1',
    'path' => 'lang/en_US',
  ),
  2 => 
  array (
    'name' => 'Spanish (Colombia)',
    'package' => 'es_CO',
    'code' => 'es_CO',
    'encoding' => 'iso-8859-1',
    'version' => '2.3.0-4',
    'path' => 'lang/es_CO',
  ),
  3 => 
  array (
    'name' => 'Spanish (Spain)',
    'package' => 'es_ES',
    'code' => 'es_ES',
    'encoding' => 'utf-8',
    'version' => '2.3.12-1',
    'path' => 'lang/es_ES',
  ),
  4 => 
  array (
    'name' => 'Swedish',
    'package' => 'sv_SE',
    'code' => 'sv_SE',
    'encoding' => 'iso-8859-1',
    'version' => '2.3.0-4',
    'path' => 'lang/sv_SE',
  ),
);

$dflt_lang = 'es_CO';
?>
