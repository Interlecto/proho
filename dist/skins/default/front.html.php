<?php 
/* <default skin>/front.html.php
 * @author: Carlos Thompson
 * 
 */
ob_start()?><!DOCTYPE html>
<html>
	<head>
		<title>{title}</title>
	</head>
	<body class="{skin:template}">
		{content}
	</body>
</html>
<?php
return ob_get_clean();
?>
