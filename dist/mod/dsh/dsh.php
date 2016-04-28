<?php
/* mod/dsh/dsh.php
 * @author: Carlos Thompson
 *
 * Main implementation of the dasboard module.
 */

class page_dashboard extends Page {
}

if(is_public()) {
	require 'mod/dsh/dsh_public.php';
	if(empty($line)) {
		class page_home extends entry_page {};
	} else {
		class page_home extends public_dash {};
	}
} else {
	require 'mod/dsh/dsh_protected.php';
	class page_home extends user_dash {};
}

?>
