<?php
/* index.php
 * @author: Carlos Thompson
 * 
 * This is the entry point for the Web App. Requests to this App
 * are forwarded to the respective Engine (module), and after checking
 * for propper credentials, the response is provided, as a formated
 * HTML page using a skin and a template.
 */

// Development mode, shall be removed for production 
ini_set('display_errors', true);
error_reporting(E_ALL);

// API configuration
if(file_exists("config/site.php"))
	require_once "config/site.php";

// check if content is catched
require "lib/catches.php";

// generate content
$page = require "lib/nav.php";
$page->go();
$page->close();
?>
