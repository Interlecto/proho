<?php
/* mod/dsh/dsh.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the dasboard module.
 */

if(is_public()) {
	require 'mod/dsh/dsh_public.php';
} else {
	require 'mod/dsh/dsh_protected.php';
}

?>
