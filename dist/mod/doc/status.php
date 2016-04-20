<?php
/* mod/doc/status.php
 * @author: Carlos Thompson
 * 
 * Main implementation of the documents module.
 */

$http_status = [
	100 => 'Continue',
	101 => 'Switching Protocols',
	102 => 'Processing',
	200 => 'OK',
	201 => 'Created',
	202 => 'Accepted',
	203 => 'Non-Authoritative Information',
	204 => 'No Content',
	205 => 'Reset Content',
	206 => 'Partial Content',
	207 => 'Multi-Status',
	208 => 'Already Reported',
	226 => 'IM Used',
	300 => 'Multiple Choices',
	301 => 'Moved Permanently',
	302 => 'Found',
	303 => 'See Other',
	304 => 'Not Modified',
	305 => 'Use Proxy',
	306 => 'Switch Proxy',
	307 => 'Temporary Redirect',
	308 => 'Permanent Redirect',
	400 => 'Bad Request',
	401 => 'Unauthorized',
	402 => 'Payment Required',
	403 => 'Forbidden',
	404 => 'Not Found',
	405 => 'Method Not Allowed',
	406 => 'Not Acceptable',
	407 => 'Proxy Authentication Required',
	408 => 'Request Timeout',
	409 => 'Conflict',
	410 => 'Gone',
	411 => 'Length Required',
	412 => 'Precondition Failed',
	413 => 'Payload Too Large',
	414 => 'URI Too Long',
	415 => 'Unsupported Media Type',
	416 => 'Range Not Satisfiable',
	417 => 'Expectation Failed',
	418 => 'I\'m a teapot',
	421 => 'Misdirected Request',
	422 => 'Unprocessable Entity',
	423 => 'Locked',
	424 => 'Failed Dependency',
	426 => 'Upgrade Required',
	428 => 'Precondition Required',
	429 => 'Too Many Requests',
	431 => 'Request Header Fields Too Large',
	451 => 'Unavailable For Legal Reasons',
	500 => 'Internal Server Error',
	501 => 'Not Implemented',
	502 => 'Bad Gateway',
	503 => 'Service Unavailable',
	504 => 'Gateway Timeout',
	505 => 'HTTP Version Not Supported',
	506 => 'Variant Also Negotiates',
	507 => 'Insufficient Storage',
	508 => 'Loop Detected',
	510 => 'Not Extended',
	511 => 'Network Authentication Required',
];

class page_status extends Page {
	function go() {
		global $http_status;
		$pp = $this->get('line/parts');
		$n = (int)$pp[1];
		set_status($n,null,false);
		Page::go();
	}
}

class page_404 extends Page {
	function go() {
		$line = $this->get('line/line');
		set_status(404,"<p>Recurso <code>$line</code> no encontrado.</p>");
		Page::go();
	}
}


function set_status($status, $message=null, $header=true) {
	global $http_status;
	if(!isset($http_status[$status])) {
		$message = "<p>Estado $status no encontrado.</p>\n$message";
		$status = 404;
		$header = true;
	}
	ph_set('title',$st=$status.' '.$http_status[$status]);
	if(empty($message))
		$message = "<p>$st</p>\n";
	ph_set('content',$message);
	if($header) {
		$protocol = empty($_SERVER['HTTPS'])? 'HTTP': 'HTTPS';
		header("$protocol/ $st");
	}
}

?>
