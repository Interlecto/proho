<?php 
/* <default skin>/front.html.php
 * @author: Carlos Thompson
 * 
 */
ob_start()?><!DOCTYPE html>
<html>
	<head>
		<meta charmap=utf-8>
		<title>{title}</title>
		<link rel=stylesheet href="{dir:skin}/css/common.css">
		<link rel=icon href="{dir:skin}/favicon.ico">
	</head>
	<body class="{skin:template}">
		<section id=body>
			{content}
		</section>
		<footer id=footer>
		</footer>
		
		<script src="{dir:skin}/js/common.js"></script>
	</body>
</html>
<?php
return ob_get_clean();
?>
