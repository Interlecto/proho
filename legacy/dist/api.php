<?php
/* api.php
 * @author: Carlos Thompson
 * 
 * This is the entry point for the HTTP API. Requests to this API
 * are forwarded to the respective Engine (module), and after checking
 * for propper credentials, the response is provided, usually as a
 * JSON object.
 */

// Development mode, shall be removed for production 
ini_set('display_errors', true);
error_reporting(E_ALL);

// API configuration
if(file_exists("config/api.php"))
	require_once "config/api.php";

$response = include 'lib/api.php';
if(empty($response)) {
	header("HTTP/1.1 204 No Content");
} else {
	if(preg_match('/(?i)msie [5-8]/',$_SERVER['HTTP_USER_AGENT']))
		header('Content-Type: text/javascript');
	else
		header('Content-Type: application/json');
	echo json_encode($response,JSON_PRETTY_PRINT);
}

?>
