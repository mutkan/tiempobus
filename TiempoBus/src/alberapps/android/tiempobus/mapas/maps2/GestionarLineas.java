/**
 *  TiempoBus - Informacion sobre tiempos de paso de autobuses en Alicante
 *  Copyright (C) 2013 Alberto Montiel
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
package alberapps.android.tiempobus.mapas.maps2;

import java.util.ArrayList;
import java.util.List;

import alberapps.android.tiempobus.R;
import alberapps.android.tiempobus.infolineas.InfoLineasTabsPager;
import alberapps.android.tiempobus.principal.DatosPantallaPrincipal;
import alberapps.android.tiempobus.tasks.LoadDatosMapaV3AsyncTask;
import alberapps.android.tiempobus.tasks.LoadDatosMapaV3AsyncTask.LoadDatosMapaV3AsyncTaskResponder;
import alberapps.java.tam.UtilidadesTAM;
import alberapps.java.tam.mapas.DatosMapa;
import alberapps.java.tam.mapas.PlaceMark;
import alberapps.java.tram.UtilidadesTRAM;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * 
 * Gestion carga de lineas, etc en el mapa
 * 
 */
public class GestionarLineas {

	private MapasMaps2Activity context;

	private SharedPreferences preferencias;

	public GestionarLineas(MapasMaps2Activity contexto, SharedPreferences preferencia) {

		context = contexto;

		preferencias = preferencia;

	}

	/**
	 * kml de carga
	 */
	public void loadDatosMapaV3() {

		String url = UtilidadesTAM.getKMLParadasV3(context.lineaSeleccionada);

		String urlRecorrido = UtilidadesTAM.getKMLRecorridoV3(context.lineaSeleccionada);

		// Control de disponibilidad de conexion
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			context.taskDatosMapaV3 = new LoadDatosMapaV3AsyncTask(loadDatosMapaV3AsyncTaskResponderIda).execute(url, urlRecorrido);
		} else {
			Toast.makeText(context.getApplicationContext(), context.getString(R.string.error_red), Toast.LENGTH_LONG).show();
			if (context.dialog != null && context.dialog.isShowing()) {
				context.dialog.dismiss();
			}
		}

	}

	/**
	 * Se llama cuando las paradas hayan sido cargadas
	 */
	LoadDatosMapaV3AsyncTaskResponder loadDatosMapaV3AsyncTaskResponderIda = new LoadDatosMapaV3AsyncTaskResponder() {
		public void datosMapaV3Loaded(DatosMapa[] datos) {

			if (datos != null) {
				context.datosMapaCargadosIda = datos[0];
				context.datosMapaCargadosVuelta = datos[1];

				cargarMapa();

				context.gestionVehiculos.loadDatosVehiculos();

				context.dialog.dismiss();

			} else {

				Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.aviso_error_datos), Toast.LENGTH_SHORT);
				toast.show();
				context.finish();

				context.dialog.dismiss();

			}

		}
	};

	/**
	 * Cargar el mapa con las paradas de la linea
	 * 
	 */
	public void cargarMapa() {

		// Cargar datos cabecera
		String cabdatos = context.lineaSeleccionadaDesc;

		context.datosLinea.setText(cabdatos);

		if (context.modoRed == InfoLineasTabsPager.MODO_RED_TRAM_OFFLINE) {
			context.drawableIda = R.drawable.tramway;
		} else {
			context.drawableIda = R.drawable.busstop_blue;
		}

		context.drawableVuelta = R.drawable.busstop_green;
		context.drawableMedio = R.drawable.busstop_medio;
		context.markersIda = new ArrayList<MarkerOptions>();
		context.markersVuelta = new ArrayList<MarkerOptions>();
		context.markersMedio = new ArrayList<MarkerOptions>();

		// -0.510017579,38.386057662,0
		// 38.386057662,-0.510017579

		final List<LatLng> listaPuntos = new ArrayList<LatLng>();

		/**
		 * 38.344820, -0.483320‎ +38° 20' 41.35", -0° 28' 59.95"
		 * 38.34482,-0.48332
		 * 
		 * long: -0,510018 lati: 38,386058 PRUEBAS‎
		 * 
		 */

		// Carga de puntos del mapa

		LatLng point = null;

		// Recorrido ida
		if (context.datosMapaCargadosIda != null && context.datosMapaCargadosIda.getRecorrido() != null && !context.datosMapaCargadosIda.getRecorrido().equals("")) {

			// Recorrido
			drawPath(context.datosMapaCargadosIda, Color.parseColor("#157087"));

		}

		MarkerOptions posicionSelecionada = null;

		// Datos IDA
		if (context.datosMapaCargadosIda != null && !context.datosMapaCargadosIda.getPlacemarks().isEmpty()) {

			// Recorrido
			drawPath(context.datosMapaCargadosIda, Color.parseColor("#157087"));

			for (int i = 0; i < context.datosMapaCargadosIda.getPlacemarks().size(); i++) {

				String[] coordenadas = context.datosMapaCargadosIda.getPlacemarks().get(i).getCoordinates().split(",");

				double lat = Double.parseDouble(coordenadas[1]); // 38.386058;
				double lng = Double.parseDouble(coordenadas[0]); // -0.510018;
				// int glat = (int) (lat * 1E6);
				// int glng = (int) (lng * 1E6);

				// 19240000,-99120000
				// 38337176
				// -491890

				// point = new GeoPoint(glat, glng);
				// GeoPoint point = new GeoPoint(19240000,-99120000);

				point = new LatLng(lat, lng);

				String descripcionAlert = context.getResources().getText(R.string.share_2) + " ";

				if (context.datosMapaCargadosIda.getPlacemarks().get(i).getSentido() != null && !context.datosMapaCargadosIda.getPlacemarks().get(i).getSentido().trim().equals("")) {
					descripcionAlert += context.datosMapaCargadosIda.getPlacemarks().get(i).getSentido().trim();
				} else {
					descripcionAlert += "Ida";
				}

				descripcionAlert += "\n" + context.getResources().getText(R.string.lineas) + " ";

				if (context.datosMapaCargadosIda.getPlacemarks().get(i).getLineas() != null) {
					descripcionAlert += context.datosMapaCargadosIda.getPlacemarks().get(i).getLineas().trim();
				}

				descripcionAlert += "\n" + context.getResources().getText(R.string.observaciones) + " ";

				if (context.datosMapaCargadosIda.getPlacemarks().get(i).getObservaciones() != null) {
					descripcionAlert += context.datosMapaCargadosIda.getPlacemarks().get(i).getObservaciones().trim();
				}

				context.markersIda.add(new MarkerOptions().position(point)
						.title("[" + context.datosMapaCargadosIda.getPlacemarks().get(i).getCodigoParada().trim() + "] " + context.datosMapaCargadosIda.getPlacemarks().get(i).getTitle().trim()).snippet(descripcionAlert)
						.icon(BitmapDescriptorFactory.fromResource(context.drawableIda)));

				listaPuntos.add(point);

				
				if (context.paradaSeleccionadaEntrada != null && context.paradaSeleccionadaEntrada.equals(context.datosMapaCargadosIda.getPlacemarks().get(i).getCodigoParada().trim())) {

					posicionSelecionada = context.markersIda.get(context.markersIda.size()-1);

				}

			}

			if (context.markersIda != null && context.markersIda.size() > 0) {

				cargarMarkers(context.markersIda, posicionSelecionada);

			} else {
				avisoPosibleError();
			}

		}

		boolean coincide = false;

		// Recorrido vuelta
		if (context.datosMapaCargadosVuelta != null && context.datosMapaCargadosVuelta.getRecorrido() != null && !context.datosMapaCargadosVuelta.getRecorrido().equals("")) {

			// Recorrido
			drawPath(context.datosMapaCargadosVuelta, Color.parseColor("#6C8715"));

		}

		// Datos VUELTA
		if (context.datosMapaCargadosVuelta != null && !context.datosMapaCargadosVuelta.getPlacemarks().isEmpty()) {

			for (int i = 0; i < context.datosMapaCargadosVuelta.getPlacemarks().size(); i++) {

				String[] coordenadas = context.datosMapaCargadosVuelta.getPlacemarks().get(i).getCoordinates().split(",");

				double lat = Double.parseDouble(coordenadas[1]); // 38.386058;
				double lng = Double.parseDouble(coordenadas[0]); // -0.510018;
				// int glat = (int) (lat * 1E6);
				// int glng = (int) (lng * 1E6);

				// 19240000,-99120000

				// point = new GeoPoint(glat, glng);
				// GeoPoint point = new GeoPoint(19240000,-99120000);

				point = new LatLng(lat, lng);

				String direc = "";

				coincide = false;

				if (context.datosMapaCargadosIda.getPlacemarks().contains(context.datosMapaCargadosVuelta.getPlacemarks().get(i))) {

					String ida = context.datosMapaCargadosIda.getCurrentPlacemark().getSentido().trim();

					if (ida.equals("")) {
						ida = "Ida";
					}

					direc = ida + " " + context.getResources().getText(R.string.tiempo_m_3) + " " + context.datosMapaCargadosVuelta.getPlacemarks().get(i).getSentido();

					coincide = true;

				} else {
					direc = context.datosMapaCargadosVuelta.getCurrentPlacemark().getSentido().trim();

					coincide = false;
				}

				if (direc == null || (direc != null && direc.trim().equals(""))) {
					direc = "Vuelta";
				}

				String descripcionAlert = context.getResources().getText(R.string.share_2) + " ";

				if (direc != null) {
					descripcionAlert += direc;
				}

				descripcionAlert += "\n" + context.getResources().getText(R.string.lineas) + " ";

				if (context.datosMapaCargadosVuelta.getPlacemarks().get(i).getLineas() != null) {
					descripcionAlert += context.datosMapaCargadosVuelta.getPlacemarks().get(i).getLineas().trim();
				}

				descripcionAlert += "\n" + context.getResources().getText(R.string.observaciones) + " ";

				if (context.datosMapaCargadosVuelta.getPlacemarks().get(i).getObservaciones() != null) {
					descripcionAlert += context.datosMapaCargadosVuelta.getPlacemarks().get(i).getObservaciones().trim();
				}

				if (coincide) {

					context.markersMedio.add(new MarkerOptions().position(point)
							.title("[" + context.datosMapaCargadosVuelta.getPlacemarks().get(i).getCodigoParada().trim() + "] " + context.datosMapaCargadosVuelta.getPlacemarks().get(i).getTitle().trim())
							.snippet(descripcionAlert).icon(BitmapDescriptorFactory.fromResource(context.drawableMedio)));

				} else {

					context.markersVuelta.add(new MarkerOptions().position(point)
							.title("[" + context.datosMapaCargadosVuelta.getPlacemarks().get(i).getCodigoParada().trim() + "] " + context.datosMapaCargadosVuelta.getPlacemarks().get(i).getTitle().trim())
							.snippet(descripcionAlert).icon(BitmapDescriptorFactory.fromResource(context.drawableVuelta)));

				}

				listaPuntos.add(point);

				// Si hay seleccion pero no estaba en la ida
				if (posicionSelecionada == null && context.paradaSeleccionadaEntrada != null && context.paradaSeleccionadaEntrada.equals(context.datosMapaCargadosVuelta.getPlacemarks().get(i).getCodigoParada().trim())) {

					posicionSelecionada = context.markersVuelta.get(context.markersVuelta.size()-1);

				}

			}

			if (context.markersMedio != null && context.markersMedio.size() > 0 && context.datosMapaCargadosIda != null && !context.datosMapaCargadosIda.getPlacemarks().isEmpty()) {

				cargarMarkers(context.markersMedio, posicionSelecionada);

			}

			if (context.markersVuelta.size() > 0) {

				cargarMarkers(context.markersVuelta, posicionSelecionada);

			} else {
				avisoPosibleError();
			}

		}

		if (listaPuntos != null && !listaPuntos.isEmpty()) {

			// Pan to see all markers in view.
			// Cannot zoom to bounds until the map has a size.
			final View mapView = context.getSupportFragmentManager().findFragmentById(R.id.map).getView();
			if (mapView.getViewTreeObserver().isAlive()) {
				mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@SuppressWarnings("deprecation")
					// We use the new method when supported
					@SuppressLint("NewApi")
					// We check which build version we are using.
					public void onGlobalLayout() {

						Builder ltb = new LatLngBounds.Builder();

						for (int i = 0; i < listaPuntos.size(); i++) {
							ltb.include(listaPuntos.get(i));
						}

						LatLngBounds bounds = ltb.build();

						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						} else {
							mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						}
						context.mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
					}
				});
			}

		}

		// Limpiar para modo normal
		posicionSelecionada = null;
		context.paradaSeleccionadaEntrada = null;

	}

	public void avisoPosibleError() {

		Toast.makeText(context, context.getString(R.string.mapa_posible_error), Toast.LENGTH_LONG).show();

	}

	/**
	 * Inicializar el mapa
	 */
	public void inicializarMapa() {

		// Control de modo de red
		context.modoRed = context.getIntent().getIntExtra("MODO_RED", 0);

		if (context.getIntent().getExtras() == null || (context.getIntent().getExtras() != null && !context.getIntent().getExtras().containsKey("MODO_RED"))) {

			context.modoRed = preferencias.getInt("infolinea_modo", 0);

		}

		context.primeraCarga = true;

		context.datosLinea = (TextView) context.findViewById(R.id.datos_linea);

		// Si viene de la seleccion de la lista
		if (context.getIntent().getExtras() != null && context.getIntent().getExtras().containsKey("LINEA_MAPA")) {

			int lineaPos = -1;

			// tram
			if (DatosPantallaPrincipal.esLineaTram(context.getIntent().getExtras().getString("LINEA_MAPA"))) {
				lineaPos = UtilidadesTRAM.getIdLinea(context.getIntent().getExtras().getString("LINEA_MAPA"));
			} else {
				lineaPos = UtilidadesTAM.getIdLinea(context.getIntent().getExtras().getString("LINEA_MAPA"));
			}

			Log.d("mapas", "linea: " + lineaPos + "l: " + context.getIntent().getExtras().getString("LINEA_MAPA"));

			if (lineaPos > -1) {

				if (DatosPantallaPrincipal.esLineaTram(context.getIntent().getExtras().getString("LINEA_MAPA"))) {

					// context.lineaSeleccionada =
					// UtilidadesTRAM.LINEAS_CODIGO_KML[lineaPos];
					context.lineaSeleccionadaDesc = UtilidadesTRAM.DESC_LINEA[lineaPos];

					context.lineaSeleccionadaNum = UtilidadesTRAM.LINEAS_NUM[lineaPos];

				} else {

					context.lineaSeleccionada = UtilidadesTAM.LINEAS_CODIGO_KML[lineaPos];
					context.lineaSeleccionadaDesc = UtilidadesTAM.LINEAS_DESCRIPCION[lineaPos];

					context.lineaSeleccionadaNum = UtilidadesTAM.LINEAS_NUM[lineaPos];

				}
				
				// Control parada seleccionada al entrar
				context.paradaSeleccionadaEntrada = context.getIntent().getExtras().getString("LINEA_MAPA_PARADA");
				

				context.dialog = ProgressDialog.show(context, "", context.getString(R.string.dialogo_espera), true);

				if (DatosPantallaPrincipal.esLineaTram(context.lineaSeleccionadaNum)) {
					context.modoRed = InfoLineasTabsPager.MODO_RED_TRAM_OFFLINE;
					context.mapasOffline.loadDatosMapaTRAMOffline();
				} else {
					context.modoRed = InfoLineasTabsPager.MODO_RED_SUBUS_OFFLINE;
					context.mapasOffline.loadDatosMapaOffline();
				}

				context.gestionVehiculos.loadDatosVehiculos();

			} else {

				Toast.makeText(context, context.getResources().getText(R.string.aviso_error_datos), Toast.LENGTH_LONG).show();

			}

			

		} else if (context.getIntent().getExtras() != null && context.getIntent().getExtras().containsKey("LINEA_MAPA_FICHA")) {

			String lineaPos = context.getIntent().getExtras().getString("LINEA_MAPA_FICHA");

			context.lineaSeleccionada = context.getIntent().getExtras().getString("LINEA_MAPA_FICHA_KML");
			context.lineaSeleccionadaDesc = context.getIntent().getExtras().getString("LINEA_MAPA_FICHA_DESC");

			context.lineaSeleccionadaNum = lineaPos;

			// Control parada seleccionada al entrar
			context.paradaSeleccionadaEntrada = context.getIntent().getExtras().getString("LINEA_MAPA_PARADA");
			
			context.dialog = ProgressDialog.show(context, "", context.getString(R.string.dialogo_espera), true);

			if (DatosPantallaPrincipal.esLineaTram(context.lineaSeleccionadaNum)) {
				context.modoRed = InfoLineasTabsPager.MODO_RED_TRAM_OFFLINE;
				context.mapasOffline.loadDatosMapaTRAMOffline();
			} else {

				if (context.getIntent().getExtras().containsKey("LINEA_MAPA_FICHA_ONLINE")) {
					context.modoRed = InfoLineasTabsPager.MODO_RED_SUBUS_ONLINE;
					loadDatosMapaV3();

				} else {
					context.modoRed = InfoLineasTabsPager.MODO_RED_SUBUS_OFFLINE;
					context.mapasOffline.loadDatosMapaOffline();
				}
			}

			context.gestionVehiculos.loadDatosVehiculos();

		}

		else {

			context.selectorLinea.cargarDatosLineasModal();

		}

		// Combo de seleccion de datos
		final Spinner spinner = (Spinner) context.findViewById(R.id.spinner_datos);

		ArrayAdapter<CharSequence> adapter = null;

		if (UtilidadesTRAM.ACTIVADO_TRAM) {
			adapter = ArrayAdapter.createFromResource(context, R.array.spinner_datos, android.R.layout.simple_spinner_item);
		} else {
			adapter = ArrayAdapter.createFromResource(context, R.array.spinner_datos_b, android.R.layout.simple_spinner_item);
		}

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spinner.setAdapter(adapter);

		// Seleccion inicial
		int infolineaModo = preferencias.getInt("infolinea_modo", 0);
		spinner.setSelection(infolineaModo);

		// Seleccion
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				// Solo en caso de haber cambiado
				if (preferencias.getInt("infolinea_modo", 0) != arg2) {

					// Guarda la nueva seleciccion
					SharedPreferences.Editor editor = preferencias.edit();
					editor.putInt("infolinea_modo", arg2);
					editor.commit();

					// cambiar el modo de la actividad
					if (arg2 == 0) {

						Intent intent2 = context.getIntent();
						intent2.putExtra("MODO_RED", InfoLineasTabsPager.MODO_RED_SUBUS_ONLINE);

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA")) {
							intent2.getExtras().remove("LINEA_MAPA");
							intent2.removeExtra("LINEA_MAPA");

						}

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA_FICHA")) {

							intent2.getExtras().remove("LINEA_MAPA_FICHA");
							intent2.removeExtra("LINEA_MAPA_FICHA");

						}

						context.finish();
						context.startActivity(intent2);

					} else if (arg2 == 1) {

						Intent intent2 = context.getIntent();
						intent2.putExtra("MODO_RED", InfoLineasTabsPager.MODO_RED_SUBUS_OFFLINE);

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA")) {
							intent2.getExtras().remove("LINEA_MAPA");
							intent2.removeExtra("LINEA_MAPA");

						}

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA_FICHA")) {

							intent2.getExtras().remove("LINEA_MAPA_FICHA");
							intent2.removeExtra("LINEA_MAPA_FICHA");

						}

						context.finish();
						context.startActivity(intent2);

					} else if (arg2 == 2) {

						Intent intent2 = context.getIntent();
						intent2.putExtra("MODO_RED", InfoLineasTabsPager.MODO_RED_TRAM_OFFLINE);

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA")) {
							intent2.getExtras().remove("LINEA_MAPA");
							intent2.removeExtra("LINEA_MAPA");

						}

						if (intent2.getExtras() != null && intent2.getExtras().containsKey("LINEA_MAPA_FICHA")) {

							intent2.getExtras().remove("LINEA_MAPA_FICHA");
							intent2.removeExtra("LINEA_MAPA_FICHA");

						}

						context.finish();
						context.startActivity(intent2);

					}

				}

			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});

		// Control de boton vehiculos
		final ToggleButton botonVehiculos = (ToggleButton) context.findViewById(R.id.mapasVehiculosButton);

		boolean vehiculosPref = preferencias.getBoolean("mapas_vehiculos", true);

		if (vehiculosPref) {
			botonVehiculos.setChecked(true);
		}

		botonVehiculos.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if (botonVehiculos.isChecked()) {

					SharedPreferences.Editor editor = preferencias.edit();
					editor.putBoolean("mapas_vehiculos", true);
					editor.commit();

					context.gestionVehiculos.loadDatosVehiculos();
				} else {

					SharedPreferences.Editor editor = preferencias.edit();
					editor.putBoolean("mapas_vehiculos", false);
					editor.commit();

					if (context.markersVehiculos != null) {

						if (context.timer != null) {
							context.timer.cancel();
						}

						if (context.gestionVehiculos.markersVehiculos != null && !context.gestionVehiculos.markersVehiculos.isEmpty()) {
							context.gestionarLineas.quitarMarkers(context.gestionVehiculos.markersVehiculos);
						}

					}
				}

			}
		});

	}

	/**
	 * Cargar marcadores sin guardar
	 * 
	 * @param markers
	 */
	public void cargarMarkers(List<MarkerOptions> markers, MarkerOptions posicionSeleccionada) {

		Log.d("mapas", "selecciondada: " + posicionSeleccionada);
		
		if (markers != null) {
			for (int i = 0; i < markers.size(); i++) {

				Marker marker = context.mMap.addMarker(markers.get(i));

				// Mostrar informacion de la seleccionada
				if (posicionSeleccionada != null && markers.get(i).equals(posicionSeleccionada)) {

					marker.showInfoWindow();

				}

			}
		}

	}

	/**
	 * Cargar marcadores y guardarlos
	 * 
	 * @param markers
	 * @return
	 */
	public List<Marker> cargarMarkersCtr(List<MarkerOptions> markers) {

		List<Marker> listaMarker = new ArrayList<Marker>();

		if (markers != null) {
			for (int i = 0; i < markers.size(); i++) {
				listaMarker.add(context.mMap.addMarker(markers.get(i)));
			}
		}

		return listaMarker;

	}

	/**
	 * Eliminar marcadores
	 * 
	 * @param markers
	 */
	public void quitarMarkers(List<Marker> markers) {

		if (markers != null) {
			for (int i = 0; i < markers.size(); i++) {

				markers.get(i).remove();

			}

			markers.clear();
		}

	}

	/**
	 * Recorrido
	 * 
	 * @param navSet
	 * @param color
	 */
	public void drawPath(DatosMapa navSet, int color) {

		if (context.mMap == null) {
			return;
		}

		// color correction for dining, make it darker
		if (color == Color.parseColor("#add331"))
			color = Color.parseColor("#6C8715");

		String path = navSet.getRecorrido();

		if (path != null && path.trim().length() > 0) {
			String[] pairs = path.trim().split(" ");

			String[] lngLat = pairs[0].split(","); // lngLat[0]=longitude
													// lngLat[1]=latitude
													// lngLat[2]=height

			if (lngLat.length < 3)
				lngLat = pairs[1].split(","); // if first pair is not
												// transferred completely, take
												// seconds pair //TODO

			try {

				LatLng startGP = new LatLng(Double.parseDouble(lngLat[1]), Double.parseDouble(lngLat[0]));

				LatLng gp1;
				LatLng gp2 = startGP;

				for (int i = 1; i < pairs.length; i++) // the last one would be
														// crash
				{
					lngLat = pairs[i].split(",");

					gp1 = gp2;

					if (gp1 != null && gp2 != null && lngLat.length >= 2) {
						gp2 = new LatLng(Double.parseDouble(lngLat[1]), Double.parseDouble(lngLat[0]));

						context.mMap.addPolyline(new PolylineOptions().add(gp1, gp2).width(5).color(color));

					}

				}

				context.mMap.addPolyline(new PolylineOptions().add(gp2, gp2).width(5).color(color));

			} catch (Exception e) {

				e.printStackTrace();

			}
		}

	}

	/**
	 * Mostrar y ocultar el recorrido de ida
	 */
	public void cargarOcultarIda() {

		if (context.datosMapaCargadosIda != null) {

			if (!context.datosMapaCargadosIda.getPlacemarks().isEmpty()) {
				context.datosMapaCargadosIdaAux = new DatosMapa();
				context.datosMapaCargadosIdaAux.setPlacemarks(context.datosMapaCargadosIda.getPlacemarks());
				context.datosMapaCargadosIda.setPlacemarks(new ArrayList<PlaceMark>());
			} else if (context.datosMapaCargadosIdaAux != null) {
				context.datosMapaCargadosIda.setPlacemarks(context.datosMapaCargadosIdaAux.getPlacemarks());
			}

			// Limpiar lista anterior para nuevas busquedas
			if (context.mMap != null) {
				context.mMap.clear();
			}

			cargarMapa();

			context.gestionVehiculos.loadDatosVehiculos();
		}
	}

	/**
	 * Mostrar y ocultar el recorrido de vuelta
	 */
	public void cargarOcultarVuelta() {

		if (context.datosMapaCargadosVuelta != null) {

			if (!context.datosMapaCargadosVuelta.getPlacemarks().isEmpty()) {
				context.datosMapaCargadosVueltaAux = new DatosMapa();
				context.datosMapaCargadosVueltaAux.setPlacemarks(context.datosMapaCargadosVuelta.getPlacemarks());
				context.datosMapaCargadosVuelta.setPlacemarks(new ArrayList<PlaceMark>());
			} else if (context.datosMapaCargadosVueltaAux != null) {
				context.datosMapaCargadosVuelta.setPlacemarks(context.datosMapaCargadosVueltaAux.getPlacemarks());
			}

			// Limpiar lista anterior para nuevas busquedas
			if (context.mMap != null) {
				context.mMap.clear();
			}

			cargarMapa();

			context.gestionVehiculos.loadDatosVehiculos();

		}
	}

}
