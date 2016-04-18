<?php

/* List of installed additional extensions. If extensions are added to the list manually
	make sure they have unique and so far never used extension_ids as a keys,
	and $next_extension_id is also updated. More about format of this file yo will find in 
	FA extension system documentation.
*/

$next_extension_id = 3; // unique id for next installed extension

$installed_extensions = array (
  0 => 
  array (
    'name' => 'US COA for a nonprofit company',
    'package' => 'chart_en_US-nonprofit',
    'version' => '2.3.0-3',
    'type' => 'chart',
    'active' => false,
    'path' => 'sql',
    'sql' => 'en_US-nonprofit.sql',
  ),
  1 => 
  array (
    'name' => 'Swedish 4 digits COA - New',
    'package' => 'chart_sv_SE-general',
    'version' => '2.3.0-6',
    'type' => 'chart',
    'active' => false,
    'path' => 'sql',
    'sql' => 'sv_SE-new2.sql',
  ),
  2 => 
  array (
    'name' => 'Spanish COA - ISO',
    'package' => 'chart_es_ES-general',
    'version' => '2.3.0-7',
    'type' => 'chart',
    'active' => false,
    'path' => 'sql',
    'sql' => 'es_ES-iso.sql',
  ),
);
?>