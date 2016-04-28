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
	<body class={if:page:class}"{page:class} {skin:template}"{else}{skin:template}{fi}>
		{area:clean-top}
		<div class=wrapper>
			{area:alerts}
			{area:header}
			<section id=body>
				{content}
			</section>
			{area:footer}
		</div>
		<script src="{dir:skin}/js/common.js"></script>
	</body>
</html>
<?php
return ob_get_clean();
?>
