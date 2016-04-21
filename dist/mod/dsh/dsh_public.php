<?php
/* mod/dsh/dsh_public.php
 * @author: Carlos Thompson
 * 
 * Dashboard for unregistered users.
 */

class public_dash extends page_dashboard {
	function __construct($enviro) {
		Page::__construct($enviro);
		$this->set('title','ProHo');
		$this->set('skin/template','clean');
	}
	
	function content() {
		ob_start() ?>
		
				<p>Dashboard con información pública, incluyendo blog, tienda y otros elementos que estén disponibles para usuarios no registrados.</p>
<?php
		return ob_get_clean();
	}
}

class entry_page extends page_dashboard {
	function __construct($enviro) {
		Page::__construct($enviro);
		$this->set('title','ProHo');
		$this->set('skin/template','flat');
	}
	
	function content() {
		ob_start() ?>

				<form class="block form-signin" action="login/in.cgi">
					<h2 class=form-signin-heading>Entrada</h2>
					<label for=inputUser class=sr-only>Nombre de usuario</label>
					<input type=text id=inputEmail class=form-control placeholder="Nombre de usuario" required autofocus>
					<label for=inputPassword class=sr-only>Contraseña</label>
					<input type=password id=inputPassword class=form-control placeholder=Contraseña required>
					<label class="indent checkbox">
						<input type=checkbox value=remember-me> Recordarme
					</label>
					<button class="indent btn btn-lg btn-primary btn-block" type=submit>Entrar</button>
					<p class=centering>
						  <a class="btn btn-default btn-sm" href="login/registro.pl">Registrarme</a>
						  <a class="btn btn-default btn-sm" href="login/recuperar.pl">Olvidé mi contraseña</a>
					</p>
				</form>

				<div class=widg-block>
					<p class=centering>Hay 2 unidades disponibles para arriendo y 1 para venta.<br><a class="btn btn-success" href="home.pl">Ver contenido público</a></p>
				</div>
<?php
		return ob_get_clean();
	}
}

?>
