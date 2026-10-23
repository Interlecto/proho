<?php
/* lib/api.php
 * @author: Carlos Thompson
 * 
 */

$server = include "server.php";

class API {
};

$cl = isset($server['line']['class'])? $server['line']['class']: null;
if(class_exists($class="api_$cl"))
	return new $class($server);

return include "server.php";
?>
