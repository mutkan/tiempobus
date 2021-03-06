/**
 *  TiempoBus - Informacion sobre tiempos de paso de autobuses en Alicante
 *  Copyright (C) 2012 Alberto Montiel
 * 
 *  based on code by The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.analytics.GoogleAnalytics;

import alberapps.android.tiempobus.MainActivity;
import alberapps.android.tiempobus.PreferencesFromXml;
import alberapps.android.tiempobus.R;
import alberapps.android.tiempobus.actionbar.ActionBarActivityFragments;
import alberapps.android.tiempobus.tasks.LoadNoticiasAsyncTask;
import alberapps.android.tiempobus.tasks.LoadNoticiasAsyncTask.LoadNoticiasAsyncTaskResponder;
import alberapps.android.tiempobus.tasks.LoadNoticiasRssAsyncTask;
import alberapps.android.tiempobus.tasks.LoadNoticiasRssAsyncTask.LoadNoticiasRssAsyncTaskResponder;
import alberapps.android.tiempobus.tasks.LoadTwitterAsyncTask;
import alberapps.android.tiempobus.tasks.LoadTwitterAsyncTask.LoadTwitterAsyncTaskResponder;
import alberapps.android.tiempobus.util.Notificaciones;
import alberapps.android.tiempobus.util.UtilidadesUI;
import alberapps.java.noticias.Noticias;
import alberapps.java.noticias.rss.NoticiaRss;
import alberapps.java.noticias.tw.TwResultado;
import alberapps.java.tam.BusLinea;
import alberapps.java.tam.mapas.DatosMapa;
import alberapps.java.tam.webservice.estructura.GetLineasResult;
import alberapps.java.tram.UtilidadesTRAM;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI that
 * switches between tabs and also allows the user to perform horizontal flicks
 * to move between the tabs.
 */
@SuppressLint("NewApi")
public class NoticiasTabsPager extends ActionBarActivityFragments {
	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	BusLinea linea = null;

	String sentidoIda = "";
	String sentidoVuelta = "";

	GetLineasResult lineasMapas = null;

	DatosMapa datosIda = null;
	DatosMapa datosVuelta = null;

	SharedPreferences preferencias = null;

	private ListView noticiasView;

	List<Noticias> noticiasRecuperadas;

	NoticiasAdapter noticiasAdapter;

	private ListView listTwWiew;

	private ListView noticiasRssView;

	List<TwResultado> avisosRecuperados;

	List<NoticiaRss> noticiasRss;

	TwAdapter twAdapter;

	NoticiasRssAdapter noticiasRssAdapter;

	private ProgressDialog dialog;

	AsyncTask<Object, Void, List<Noticias>> loadNoticiasTask = null;

	AsyncTask<Object, Void, List<TwResultado>> loadTwTask = null;

	AsyncTask<Object, Void, List<NoticiaRss>> loadNoticiasRssTask = null;

	public BusLinea getLinea() {
		return linea;
	}

	public void setLinea(BusLinea linea) {
		this.linea = linea;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		preferencias = PreferenceManager.getDefaultSharedPreferences(this);

		if (UtilidadesTRAM.ACTIVADO_TRAM) {
			setContentView(R.layout.noticias_contenedor);
		} else {
			setContentView(R.layout.noticias_contenedor_2);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		if (!UtilidadesUI.pantallaTabletHorizontal(this)) {

			mTabHost = (TabHost) findViewById(android.R.id.tabhost);
			mTabHost.setup();

			mViewPager = (ViewPager) findViewById(R.id.pager);

			mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

			mTabsAdapter.addTab(mTabHost.newTabSpec("noticias").setIndicator(getString(R.string.tab_noticias)), FragmentNoticias.class, null);

			if (UtilidadesTRAM.ACTIVADO_TRAM) {
				mTabsAdapter.addTab(mTabHost.newTabSpec("rss").setIndicator(getString(R.string.rss_tram)), FragmentNoticiasRss.class, null);
			}

			mTabsAdapter.addTab(mTabHost.newTabSpec("alberapps").setIndicator(getString(R.string.tab_tw)), FragmentTwitter.class, null);

			if (savedInstanceState != null) {
				mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
			}

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!UtilidadesUI.pantallaTabletHorizontal(this) && mTabHost != null) {
			outState.putString("tab", mTabHost.getCurrentTabTag());
		}
	}

	@Override
	protected void onDestroy() {

		// Se cancelan las tareas en caso de volver sin terminar

		if (loadNoticiasTask != null && loadNoticiasTask.getStatus() == Status.RUNNING) {

			loadNoticiasTask.cancel(true);

			Log.d("noticias", "Cancelada task noticias");

		}

		if (loadTwTask != null && loadTwTask.getStatus() == Status.RUNNING) {

			loadTwTask.cancel(true);

			Log.d("noticias", "Cancelada task twitter");

		}

		if (loadNoticiasRssTask != null && loadNoticiasRssTask.getStatus() == Status.RUNNING) {

			loadNoticiasRssTask.cancel(true);

			Log.d("noticias", "Cancelada task rss");

		}

		super.onDestroy();
	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */
	public static class TabsAdapter extends FragmentPagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final TabHost mTabHost;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final String tag;
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				clss = _class;
				args = _args;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			public DummyTabFactory(Context context) {
				mContext = context;
			}

			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}

		public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mTabHost = tabHost;
			mViewPager = pager;
			mTabHost.setOnTabChangedListener(this);
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
			tabSpec.setContent(new DummyTabFactory(mContext));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, clss, args);
			mTabs.add(info);
			mTabHost.addTab(tabSpec);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(), info.args);
		}

		public void onTabChanged(String tabId) {
			int position = mTabHost.getCurrentTab();
			mViewPager.setCurrentItem(position);
		}

		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		public void onPageSelected(int position) {
			// Unfortunately when TabHost changes the current tab, it kindly
			// also takes care of putting focus on it when not in touch mode.
			// The jerk.
			// This hack tries to prevent this from pulling focus out of our
			// ViewPager.
			TabWidget widget = mTabHost.getTabWidget();
			int oldFocusability = widget.getDescendantFocusability();
			widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			mTabHost.setCurrentTab(position);
			widget.setDescendantFocusability(oldFocusability);
		}

		public void onPageScrollStateChanged(int state) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_noticias, menu);

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

		case R.id.menu_refresh:

			recargarNoticias(false, false);

			break;

		case R.id.menu_preferencias:
			showPreferencias();
			break;

		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Quitar notificacion
		// Get a reference to the notification manager
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(ns);
		mNotificationManager.cancel(Notificaciones.NOTIFICACION_NOTICIAS);
		mNotificationManager.cancel(Notificaciones.NOTIFICACION_NOTICIAS_TRAM);

		recargarNoticias(false, true);

	}

	/**
	 * Lanza la subactivididad de preferencias
	 */
	private void showPreferencias() {
		Intent i = new Intent(this, PreferencesFromXml.class);

		startActivity(i);

	}

	/**
	 * Recarga de noticias
	 */
	private void recargarNoticias(boolean bloqueo, boolean usarCache) {

		if (bloqueo) {
			dialog.show();

			dialog.setMessage(getString(R.string.carga_noticias_msg));

		} else {
			getActionBarHelper().setRefreshActionItemState(true);
		}

		/**
		 * Sera llamado cuando la tarea de cargar las noticias
		 */
		LoadNoticiasAsyncTaskResponder loadNoticiasAsyncTaskResponder = new LoadNoticiasAsyncTaskResponder() {
			public void noticiasLoaded(List<Noticias> noticias) {

				if (noticias != null && !noticias.isEmpty()) {
					noticiasRecuperadas = noticias;
					cargarListado(noticias, true);

				} else {

					noticiasRecuperadas = null;
					// Error al recuperar datos
					cargarListado(noticias, false);

				}

				// Quitar barra progreso inicial
				ProgressBar lpb = (ProgressBar) findViewById(R.id.progreso_noticias);
				if (lpb != null) {
					lpb.clearAnimation();
					lpb.setVisibility(View.INVISIBLE);
				}

				if (noticias == null || noticias.isEmpty()) {
					TextView vacio = (TextView) findViewById(R.id.vacio_noticias);
					noticiasView.setEmptyView(vacio);
				}

				// Inicia carga twitter
				recargarTw();

			}
		};

		// Control de disponibilidad de conexion
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			loadNoticiasTask = new LoadNoticiasAsyncTask(loadNoticiasAsyncTaskResponder).execute(usarCache);

		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.error_red), Toast.LENGTH_LONG).show();

			getActionBarHelper().setRefreshActionItemState(false);

			if (dialog != null && dialog.isShowing()) {

				dialog.dismiss();

			}

		}

	}

	/**
	 * Carga del listado
	 * 
	 * @param noticiasList
	 * @param ok
	 */
	public void cargarListado(List<Noticias> noticiasList, boolean ok) {

		try {

			noticiasAdapter = new NoticiasAdapter(this, R.layout.noticias_item);

			
			
			if (ok) {

				noticiasAdapter.addAll(noticiasList);

				noticiasAdapter.notifyDataSetChanged();
			}

			// Listado noticias
			noticiasView = (ListView) findViewById(R.id.lista_noticias);
			noticiasView.setOnItemClickListener(noticiasClickedHandler);
			noticiasView.setAdapter(noticiasAdapter);
			View emptyView = findViewById(R.id.vacio_noticias);
			noticiasView.setEmptyView(emptyView);

			cargarHeaderNoticias();
			
		} catch (Exception e) {

			// Para evitar fallos si se intenta volver antes de terminar

			e.printStackTrace();

		}

	}

	
	/**
	 * Cargar cabecera listado
	 */
	public void cargarHeaderNoticias() {

		if (noticiasView != null && noticiasView.getHeaderViewsCount() == 0) {

			LayoutInflater li2 = LayoutInflater.from(this);

			View vheader = li2.inflate(R.layout.noticias_header, null);

			TextView texto = (TextView) vheader.findViewById(R.id.txt_noticias_header);

			StringBuffer textoHeader = new StringBuffer();

			textoHeader.append(getString(R.string.aviso_noticias));
			textoHeader.append("\n");
			textoHeader.append(FragmentNoticias.noticiasURL);
			textoHeader.append("\n");
			textoHeader.append(getString(R.string.noticias_instrucciones));

			texto.setLinksClickable(true);
			texto.setAutoLinkMask(Linkify.WEB_URLS);
			
			texto.setText(textoHeader.toString());

			noticiasView = (ListView) findViewById(R.id.lista_noticias);

			noticiasView.addHeaderView(vheader);

		}

	}
	
	/**
	 * Listener encargado de gestionar las pulsaciones sobre los items
	 */
	private OnItemClickListener noticiasClickedHandler = new OnItemClickListener() {

		/**
		 * @param l
		 *            The ListView where the click happened
		 * @param v
		 *            The view that was clicked within the ListView
		 * @param position
		 *            The position of the view in the list
		 * @param id
		 *            The row id of the item that was clicked
		 */
		public void onItemClick(AdapterView<?> l, View v, final int position_inicial, long id) {

			if(position_inicial == 0){
				return;
			}
			
			//Para descartar la cabecera
			final int position = position_inicial-1;
			
			if (noticiasRecuperadas.get(position).getLinks() != null && !noticiasRecuperadas.get(position).getLinks().isEmpty()) {

				if (noticiasRecuperadas.get(position).getLinks().size() > 1) {

					AlertDialog.Builder builder = new AlertDialog.Builder(NoticiasTabsPager.this);
					builder.setTitle(R.string.noticias_links);

					int size = noticiasRecuperadas.get(position).getDescLink().size();
					CharSequence[] items = new CharSequence[size];

					for (int i = 0; i < noticiasRecuperadas.get(position).getDescLink().size(); i++) {

						items[i] = noticiasRecuperadas.get(position).getDescLink().get(i);

					}

					builder.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {

							Intent i = new Intent(NoticiasTabsPager.this, DetalleNoticiaActivity.class);
							i.putExtra("NOTICIA_SELECCIONADA", noticiasRecuperadas.get(position));
							i.putExtra("POSICION_LINK", item);
							startActivity(i);

						}
					});

					AlertDialog alert = builder.create();

					alert.show();

				} else {

					Intent i = new Intent(NoticiasTabsPager.this, DetalleNoticiaActivity.class);
					i.putExtra("NOTICIA_SELECCIONADA", noticiasRecuperadas.get(position));
					i.putExtra("POSICION_LINK", 0);
					startActivity(i);

				}

			}
		}
	};

	/**
	 * Recarga de datos twitter
	 */
	private void recargarTw() {

		if (dialog != null && dialog.isShowing()) {
			dialog.setMessage(getString(R.string.carga_tw_msg));
		}

		/**
		 * Sera llamado cuando la tarea de cargar las noticias
		 */
		LoadTwitterAsyncTaskResponder loadTwitterAsyncTaskResponder = new LoadTwitterAsyncTaskResponder() {
			public void TwitterLoaded(List<TwResultado> mensajes) {

				if (mensajes != null && !mensajes.isEmpty()) {
					avisosRecuperados = mensajes;

					cargarListadoTw();

				} else {

					avisosRecuperados = null;
					// Error al recuperar datos

					cargarListadoTw();

				}

				if (UtilidadesTRAM.ACTIVADO_TRAM) {

					recargarRss();

				} else {

					getActionBarHelper().setRefreshActionItemState(false);

					if (dialog != null && dialog.isShowing()) {

						dialog.dismiss();

					}

				}

				if (listTwWiew != null) {
					// Quitar barra progreso inicial
					ProgressBar lpb = (ProgressBar) findViewById(R.id.tiempos_progreso_tw);
					lpb.clearAnimation();
					lpb.setVisibility(View.INVISIBLE);

					if (mensajes == null || mensajes.isEmpty()) {
						TextView vacio = (TextView) findViewById(R.id.vacio_tw);
						listTwWiew.setEmptyView(vacio);
					}
				}

			}
		};

		//Opcion de desactivar twitter
		if(!preferencias.getBoolean("tw_activar", true)){
			
			getActionBarHelper().setRefreshActionItemState(false);

			if (dialog != null && dialog.isShowing()) {

				dialog.dismiss();

			}
			
			listTwWiew = (ListView) findViewById(R.id.listatw);
			
			if (listTwWiew != null) {
				// Quitar barra progreso inicial
				ProgressBar lpb = (ProgressBar) findViewById(R.id.tiempos_progreso_tw);
				lpb.clearAnimation();
				lpb.setVisibility(View.INVISIBLE);
				
				TextView vacio = (TextView) findViewById(R.id.vacio_tw);
				listTwWiew.setEmptyView(vacio);
				
			}
			avisosRecuperados = null;
			
			cargarListadoTw();
			
			return;
		}
		
		
		// Control de disponibilidad de conexion
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {

			// Cargar lista de elementos a consultar
			List<Boolean> listaTW = new ArrayList<Boolean>();

			listaTW.add(preferencias.getBoolean("tw_2", true));
			listaTW.add(preferencias.getBoolean("tw_3", true));
			listaTW.add(preferencias.getBoolean("tw_4", true));
			listaTW.add(preferencias.getBoolean("tw_5", true));
			listaTW.add(preferencias.getBoolean("tw_6", true));

			String cantidad = preferencias.getString("tweets_maximos_v11", "3");

			loadTwTask = new LoadTwitterAsyncTask(loadTwitterAsyncTaskResponder).execute(listaTW, cantidad);
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.error_red), Toast.LENGTH_LONG).show();

			getActionBarHelper().setRefreshActionItemState(false);

			if (dialog != null && dialog.isShowing()) {

				dialog.dismiss();

			}
		}

	}

	/**
	 * Carga el listado
	 */
	public void cargarListadoTw() {

		try {

			listTwWiew = (ListView) findViewById(R.id.listatw);

			if (listTwWiew != null) {
				
				//listTwWiew.setDrawingCacheEnabled(false);
				
				cargarHeaderTwitter();

				twAdapter = new TwAdapter(this, R.layout.avisostw_item);

				if (avisosRecuperados != null) {

					twAdapter.addAll(avisosRecuperados);
					twAdapter.notifyDataSetChanged();

				} else {

					twAdapter.clear();
					twAdapter.notifyDataSetChanged();

				}

				// lineasView.setOnItemClickListener(twClickedHandler);

				listTwWiew.setAdapter(twAdapter);

			}

		} catch (Exception e) {

			// Para evitar fallos en caso de volver antes de terminar

			e.printStackTrace();

		}

	}

	/**
	 * Cargar cabecera listado
	 */
	public void cargarHeaderTwitter() {

		if (listTwWiew != null && listTwWiew.getHeaderViewsCount() == 0) {

			LayoutInflater li2 = LayoutInflater.from(this);

			View vheader = li2.inflate(R.layout.noticias_header, null);

			TextView texto = (TextView) vheader.findViewById(R.id.txt_noticias_header);

			StringBuffer textoHeader = new StringBuffer();

			textoHeader.append(getString(R.string.dato_tw));
			textoHeader.append("\n");
			textoHeader.append(getString(R.string.tw1));
			textoHeader.append("\n");
			textoHeader.append(getString(R.string.twitter4j));

			texto.setText(textoHeader.toString());

			listTwWiew = (ListView) findViewById(R.id.listatw);

			listTwWiew.addHeaderView(vheader);

		}

	}

	/**
	 * Listener encargado de gestionar las pulsaciones sobre los items
	 */
	private OnItemClickListener twClickedHandler = new OnItemClickListener() {

		/**
		 * @param l
		 *            The ListView where the click happened
		 * @param v
		 *            The view that was clicked within the ListView
		 * @param position
		 *            The position of the view in the list
		 * @param id
		 *            The row id of the item that was clicked
		 */
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {

			Toast.makeText(getApplicationContext(), getString(R.string.error_red), Toast.LENGTH_LONG).show();

			String url = avisosRecuperados.get(position).getUrl();

			Intent i = new Intent(Intent.ACTION_VIEW);

			i.setData(Uri.parse(url));
			startActivity(i);

		}
	};

	// ///////RSS

	/**
	 * Recarga de datos twitter
	 */
	private void recargarRss() {

		if (dialog != null && dialog.isShowing()) {
			dialog.setMessage(getString(R.string.carga_rss_tram_msg));
		}

		/**
		 * Sera llamado cuando la tarea de cargar las noticias
		 */
		LoadNoticiasRssAsyncTaskResponder loadNoticiasRssAsyncTaskResponder = new LoadNoticiasRssAsyncTaskResponder() {
			public void noticiasRssLoaded(List<NoticiaRss> noticias) {

				if (noticias != null && !noticias.isEmpty()) {
					noticiasRss = noticias;
					cargarListadoRss();

				} else {

					noticiasRss = null;
					// Error al recuperar datos
					cargarListadoRss();

				}

				getActionBarHelper().setRefreshActionItemState(false);

				if (dialog != null && dialog.isShowing()) {

					dialog.dismiss();

				}

				if (noticiasRssView != null) {
					// Quitar barra progreso inicial
					ProgressBar lpb = (ProgressBar) findViewById(R.id.progreso_rss);
					lpb.clearAnimation();
					lpb.setVisibility(View.INVISIBLE);

					if (noticias == null || noticias.isEmpty()) {
						TextView vacio = (TextView) findViewById(R.id.vacio_noticias_rss);
						noticiasRssView.setEmptyView(vacio);
					}
				}

			}
		};

		// Control de disponibilidad de conexion
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {

			loadNoticiasRssTask = new LoadNoticiasRssAsyncTask(loadNoticiasRssAsyncTaskResponder).execute();
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.error_red), Toast.LENGTH_LONG).show();

			getActionBarHelper().setRefreshActionItemState(false);

			if (dialog != null && dialog.isShowing()) {

				dialog.dismiss();

			}
		}

	}

	/**
	 * Carga el listado
	 */
	public void cargarListadoRss() {

		try {

			noticiasRssAdapter = new NoticiasRssAdapter(this, R.layout.noticias_rss_item);

			if (noticiasRss != null) {

				noticiasRssAdapter.addAll(noticiasRss);
				noticiasRssAdapter.notifyDataSetChanged();

			}

			noticiasRssView = (ListView) findViewById(R.id.noticias_rss);

			TextView vacio = (TextView) findViewById(R.id.vacio_noticias_rss);
			noticiasRssView.setEmptyView(vacio);

			// lineasView.setOnItemClickListener(twClickedHandler);

			noticiasRssView.setAdapter(noticiasRssAdapter);

		} catch (Exception e) {

			// Para evitar fallos en caso de volver antes de terminar
			e.printStackTrace();

		}

	}

	/**
	 * Listener encargado de gestionar las pulsaciones sobre los items
	 */
	private OnItemClickListener noticiaRssClickedHandler = new OnItemClickListener() {

		/**
		 * @param l
		 *            The ListView where the click happened
		 * @param v
		 *            The view that was clicked within the ListView
		 * @param position
		 *            The position of the view in the list
		 * @param id
		 *            The row id of the item that was clicked
		 */
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {

		}
	};

	@Override
	protected void onStart() {

		super.onStart();

		if (preferencias.getBoolean("analytics_on", true)) {
			//EasyTracker.getInstance(this).activityStart(this);
			GoogleAnalytics.getInstance(this).reportActivityStart(this);
		}

	}

	@Override
	protected void onStop() {

		if (preferencias.getBoolean("analytics_on", true)) {
			//EasyTracker.getInstance(this).activityStop(this);
			GoogleAnalytics.getInstance(this).reportActivityStop(this);
		}
		
		super.onStop();
		

	}

}
