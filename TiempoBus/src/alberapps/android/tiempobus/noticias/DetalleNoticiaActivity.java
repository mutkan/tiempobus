/**
 *  TiempoBus - Informacion sobre tiempos de paso de autobuses en Alicante
 *  Copyright (C) 2012 Alberto Montiel
 * 
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package alberapps.android.tiempobus.noticias;

import com.google.android.gms.analytics.GoogleAnalytics;

import alberapps.android.tiempobus.MainActivity;
import alberapps.android.tiempobus.R;
import alberapps.android.tiempobus.actionbar.ActionBarBuscadorActivity;
import alberapps.android.tiempobus.tasks.LoadDetalleNoticiaAsyncTask;
import alberapps.android.tiempobus.tasks.LoadDetalleNoticiaAsyncTask.LoadDetalleNoticiaAsyncTaskResponder;
import alberapps.android.tiempobus.util.UtilidadesUI;
import alberapps.java.noticias.Noticias;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Detalle de la noticia
 */
@SuppressLint("NewApi")
public class DetalleNoticiaActivity extends ActionBarBuscadorActivity {

	private ProgressDialog dialog;

	WebView mWebView;

	Noticias noticia;

	String link;

	AsyncTask<String, Void, Noticias> taskDetalle = null;

	SharedPreferences preferencias = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detalle_noticia);

		

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		preferencias = PreferenceManager.getDefaultSharedPreferences(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		dialog = ProgressDialog.show(DetalleNoticiaActivity.this, "", getString(R.string.dialogo_espera), true);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {

			noticia = (Noticias) extras.get("NOTICIA_SELECCIONADA");

			int posicion = extras.getInt("POSICION_LINK");

			link = noticia.getLinks().get(posicion);

		}

		// Fondo
		setupFondoAplicacion();
		
		/**
		 * Sera llamado cuando la tarea de cargar las noticias
		 */
		LoadDetalleNoticiaAsyncTaskResponder loadDetalleNoticiaAsyncTaskResponder = new LoadDetalleNoticiaAsyncTaskResponder() {
			public void detalleNoticiaLoaded(Noticias noticia) {

				if (noticia != null) {

					cargarDetalle(noticia);
					dialog.dismiss();

				} else {
					// Error al recuperar datos

					cargarDetalleError();

					Toast.makeText(getApplicationContext(), getString(R.string.ko_noticia), Toast.LENGTH_SHORT).show();
					dialog.dismiss();

				}
			}
		};

		// Control de disponibilidad de conexion
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			taskDetalle = new LoadDetalleNoticiaAsyncTask(loadDetalleNoticiaAsyncTaskResponder).execute(link);
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.error_red), Toast.LENGTH_LONG).show();
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
		}

		
		
		
	}

	@Override
	protected void onDestroy() {

		detenerTareas();

		super.onDestroy();
	}

	public void detenerTareas() {

		if (taskDetalle != null && taskDetalle.getStatus() == Status.RUNNING) {

			taskDetalle.cancel(true);

			Log.d("NOTICIAS", "Cancelada task detalle");

		}

	}

	/**
	 * Cargar el detallet
	 * 
	 * @param noticia
	 */
	public void cargarDetalle(Noticias noticia) {

		TextView cabFecha = (TextView) findViewById(R.id.cabeceraFecha);
		TextView cabTitulo = (TextView) findViewById(R.id.cabeceraTitulo);
		TextView cabLinea = (TextView) findViewById(R.id.cabeceraLinea);

		cabFecha.setText(noticia.getFechaCabecera());
		cabTitulo.setText(noticia.getTituloCabecera());
		cabLinea.setText(noticia.getLineaCabecera());

		TextView accederNoticia = (TextView) findViewById(R.id.accederNoticia);

		accederNoticia.setLinksClickable(true);
		accederNoticia.setAutoLinkMask(Linkify.WEB_URLS);

		accederNoticia.setText(link);

		if (noticia.getContenidoHtml() != null) {
			mWebView = (WebView) findViewById(R.id.webViewDetalle);
			
			mWebView.getSettings().setLoadWithOverviewMode(true);
			mWebView.getSettings().setUseWideViewPort(true);
			//mWebView.getSettings().setBuiltInZoomControls(true);
			//mWebView.getSettings().setDisplayZoomControls(true);

			mWebView.loadData(noticia.getContenidoHtml(), "text/html", "ISO-8859-1");

		}

	}

	/**
	 * Cargar informacion minima en caso de error
	 */
	public void cargarDetalleError() {

		TextView cabFecha = (TextView) findViewById(R.id.cabeceraFecha);
		TextView cabTitulo = (TextView) findViewById(R.id.cabeceraTitulo);
		TextView cabLinea = (TextView) findViewById(R.id.cabeceraLinea);

		cabFecha.setText(noticia.getFecha());
		cabTitulo.setText(noticia.getNoticia());
		cabLinea.setText("");

		TextView accederNoticia = (TextView) findViewById(R.id.accederNoticia);

		accederNoticia.setLinksClickable(true);
		accederNoticia.setAutoLinkMask(Linkify.WEB_URLS);

		accederNoticia.setText(link);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sin_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			break;

		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	protected void onStart() {

		super.onStart();

		if (preferencias.getBoolean("analytics_on", true)) {
			// EasyTracker.getInstance(this).activityStart(this);
			GoogleAnalytics.getInstance(this).reportActivityStart(this);
		}

	}

	@Override
	protected void onStop() {

		if (preferencias.getBoolean("analytics_on", true)) {
			// EasyTracker.getInstance(this).activityStop(this);
			GoogleAnalytics.getInstance(this).reportActivityStop(this);
		}

		super.onStop();

	}

	/**
	 * Seleccion del fondo de la galeria en el arranque
	 */
	private void setupFondoAplicacion() {

		String fondo_galeria = preferencias.getString("image_galeria", "");

		View contenedor_principal = findViewById(R.id.main);

		UtilidadesUI.setupFondoAplicacion(fondo_galeria, contenedor_principal, this);

	}

}
