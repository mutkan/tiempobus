/**
 *  TiempoBus - Informacion sobre tiempos de paso de autobuses en Alicante
 *  Copyright (C) 2012 Alberto Montiel
 * 
 *  based on code by ZgzBus Copyright (C) 2010 Francho Joven
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
package alberapps.android.tiempobus.favoritos;

import com.google.android.gms.analytics.GoogleAnalytics;

import alberapps.android.tiempobus.MainActivity;
import alberapps.android.tiempobus.R;
import alberapps.android.tiempobus.actionbar.ActionBarActivity;
import alberapps.android.tiempobus.data.TiempoBusDb;
import alberapps.android.tiempobus.favoritos.drive.FavoritoDriveActivity;
import alberapps.android.tiempobus.tasks.BackupAsyncTask;
import alberapps.android.tiempobus.tasks.BackupAsyncTask.BackupAsyncTaskResponder;
import alberapps.android.tiempobus.util.UtilidadesUI;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * Muestra los favoritos guardados
 * 
 * 
 */
@SuppressLint("NewApi")
public class FavoritosActivity extends ActionBarActivity {

	public static final int SUB_ACTIVITY_REQUEST_DRIVE = 1100;

	public static final String[] PROJECTION = new String[] { TiempoBusDb.Favoritos._ID, // 0
			TiempoBusDb.Favoritos.POSTE, // 1
			TiempoBusDb.Favoritos.TITULO, // 2
			TiempoBusDb.Favoritos.DESCRIPCION, // 3
	};

	private static final int MENU_BORRAR = 2;

	private static final int MENU_MODIFICAR = 3;

	private ListView favoritosView;

	SimpleCursorAdapter adapter;

	SharedPreferences preferencias = null;

	String orden = "";

	private ProgressDialog dialog;

	/**
	 * On Create
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		preferencias = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.favoritos);
		// setTitle(R.string.tit_favoritos);

		// Fondo
		setupFondoAplicacion();

		// Orden de favoritos
		orden = preferencias.getString("orden_favoritos", TiempoBusDb.Favoritos.DEFAULT_SORT_ORDER);
		consultarDatos(orden);

	}

	private void consultarDatos(String orden) {

		/*
		 * Si no ha sido cargado con anterioridad, cargamos nuestro
		 * "content provider"
		 */
		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(TiempoBusDb.Favoritos.CONTENT_URI);
		}

		/*
		 * Query "managed": la actividad se encargará de cerrar y volver a
		 * cargar el cursor cuando sea necesario
		 */
		Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null, orden);

		/*
		 * Mapeamos las querys SQL a los campos de las vistas
		 */
		String[] camposDb = new String[] { TiempoBusDb.Favoritos.POSTE, TiempoBusDb.Favoritos.TITULO, TiempoBusDb.Favoritos.DESCRIPCION };
		int[] camposView = new int[] { R.id.poste, R.id.titulo, R.id.descripcion };

		adapter = new SimpleCursorAdapter(this, R.layout.favoritos_item, cursor, camposDb, camposView);

		setListAdapter(adapter);

		/*
		 * Preparamos las acciones a realizar cuando pulsen un favorito
		 */

		favoritosView = (ListView) findViewById(android.R.id.list);
		favoritosView.setOnItemClickListener(favoritoClickedHandler);
		registerForContextMenu(favoritosView);

	}

	/**
	 * Si no hay favoritos cerramos la actividad
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		/*
		 * if (adapter.getCount() == 0) { Toast.makeText(FavoritosActivity.this,
		 * R.string.no_favs, Toast.LENGTH_SHORT).show();
		 * 
		 * finish(); }
		 */
	}

	/**
	 * Listener encargado de gestionar las pulsaciones sobre los items
	 */
	private OnItemClickListener favoritoClickedHandler = new OnItemClickListener() {

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
			Cursor c = (Cursor) l.getItemAtPosition(position);
			int poste = c.getInt(c.getColumnIndex(TiempoBusDb.Favoritos.POSTE));

			Intent intent = new Intent();
			Bundle b = new Bundle();
			b.putInt("POSTE", poste);
			intent.putExtras(b);
			setResult(MainActivity.SUB_ACTIVITY_RESULT_OK, intent);
			finish();

		}
	};

	/**
	 * Menu contextual
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		menu.setHeaderTitle(R.string.menu_contextual);

		menu.add(0, MENU_BORRAR, 0, getResources().getText(R.string.menu_borrar));

		menu.add(0, MENU_MODIFICAR, 1, getResources().getText(R.string.menu_modificar));

	}

	/**
	 * Gestionamos la pulsacion de un menu contextual
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
		case MENU_BORRAR:
			Uri miUri = ContentUris.withAppendedId(TiempoBusDb.Favoritos.CONTENT_URI, info.id);

			getContentResolver().delete(miUri, null, null);

			Toast.makeText(this, getResources().getText(R.string.info_borrar), Toast.LENGTH_SHORT).show();

			return true;

		case MENU_MODIFICAR:
			Uri miUriM = ContentUris.withAppendedId(TiempoBusDb.Favoritos.CONTENT_URI, info.id);

			launchModificarFavorito(info);

			return true;

		default:
			// return super.onContextItemSelected(item);
		}
		return false;
	}

	/**
	 * Lanza la subactividad para modificar el favorito seleccionado
	 */
	private void launchModificarFavorito(AdapterContextMenuInfo info) {

		Uri miUriM = ContentUris.withAppendedId(TiempoBusDb.Favoritos.CONTENT_URI, info.id);

		String[] camposDb = new String[] { TiempoBusDb.Favoritos.POSTE, TiempoBusDb.Favoritos.TITULO, TiempoBusDb.Favoritos.DESCRIPCION };

		String mSelectionClause = null;
		String[] mSelectionArgs = { "" };
		String mSortOrder = null;

		Cursor mCursor = getContentResolver().query(miUriM, PROJECTION, null, null, TiempoBusDb.Favoritos.DEFAULT_SORT_ORDER);

		String posteN = "";
		String titulo = "";
		String descripcion = "";

		if (mCursor != null) {

			while (mCursor.moveToNext()) {

				posteN = mCursor.getString(mCursor.getColumnIndex(TiempoBusDb.Favoritos.POSTE));
				titulo = mCursor.getString(mCursor.getColumnIndex(TiempoBusDb.Favoritos.TITULO));
				descripcion = mCursor.getString(mCursor.getColumnIndex(TiempoBusDb.Favoritos.DESCRIPCION));

			}

			mCursor.close();

		} else {

		}

		int poste = Integer.parseInt(posteN);

		Intent i = new Intent(FavoritosActivity.this, FavoritoModificarActivity.class);

		Bundle extras = new Bundle();
		extras.putInt("POSTE", poste); // Pasamos el poste actual

		extras.putString("TITULO", titulo);

		extras.putString("DESCRIPCION", descripcion);

		// Uri de modificacion
		extras.putLong("ID_URI", info.id);

		i.putExtras(extras);
		startActivityForResult(i, MainActivity.SUB_ACTIVITY_REQUEST_ADDFAV);
	}

	/**
	 * Seleccion del fondo de la galeria en el arranque
	 */
	private void setupFondoAplicacion() {

		String fondo_galeria = preferencias.getString("image_galeria", "");

		View contenedor_principal = findViewById(R.id.contenedor_favoritos);

		UtilidadesUI.setupFondoAplicacion(fondo_galeria, contenedor_principal, this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.favoritos, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		SharedPreferences.Editor editor = preferencias.edit();

		switch (item.getItemId()) {
		case R.id.menu_ordenar_parada:

			if (orden.equals(TiempoBusDb.Favoritos.NUM_A_SORT_ORDER)) {

				orden = TiempoBusDb.Favoritos.NUM_D_SORT_ORDER;

				consultarDatos(orden);

				// Guardar
				editor.putString("orden_favoritos", TiempoBusDb.Favoritos.NUM_D_SORT_ORDER);
				editor.commit();

			} else {

				orden = TiempoBusDb.Favoritos.NUM_A_SORT_ORDER;

				consultarDatos(orden);

				// Guardar
				editor.putString("orden_favoritos", TiempoBusDb.Favoritos.NUM_A_SORT_ORDER);
				editor.commit();

			}

			break;
		case R.id.menu_ordenar_nombre:

			if (orden.equals(TiempoBusDb.Favoritos.NAME_A_SORT_ORDER)) {

				orden = TiempoBusDb.Favoritos.NAME_D_SORT_ORDER;

				consultarDatos(orden);

				// Guarda
				editor.putString("orden_favoritos", TiempoBusDb.Favoritos.NAME_D_SORT_ORDER);
				editor.commit();
			} else {

				orden = TiempoBusDb.Favoritos.NAME_A_SORT_ORDER;

				consultarDatos(orden);

				// Guarda
				editor.putString("orden_favoritos", TiempoBusDb.Favoritos.NAME_A_SORT_ORDER);
				editor.commit();

			}

			break;

		case R.id.menu_exportar:

			exportarDB();

			break;

		case R.id.menu_importar:

			importarDB();

			break;

		case R.id.menu_importar_drive:

			importarDriveDB();

			break;

		case R.id.menu_exportar_drive:

			Intent intent2 = new Intent(FavoritosActivity.this, FavoritoDriveActivity.class);

			Bundle b2 = new Bundle();
			b2.putString("MODO", FavoritoDriveActivity.MODO_EXPORTAR);
			intent2.putExtras(b2);

			startActivityForResult(intent2, SUB_ACTIVITY_REQUEST_DRIVE);

			break;

		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			break;

		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Exportar la base de datos
	 */
	private void exportarDB() {

		dialog = ProgressDialog.show(FavoritosActivity.this, "", getString(R.string.dialog_procesando), true);

		BackupAsyncTaskResponder backupAsyncTaskResponder = new BackupAsyncTaskResponder() {
			public void backupLoaded(Boolean resultado) {

				if (resultado != null && resultado) {

					if (dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}
					Toast.makeText(FavoritosActivity.this, getString(R.string.ok_exportar), Toast.LENGTH_LONG).show();

				} else {
					if (dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}
					Toast.makeText(FavoritosActivity.this, getString(R.string.error_exportar), Toast.LENGTH_SHORT).show();

				}
			}
		};

		new BackupAsyncTask(backupAsyncTaskResponder).execute("exportar");

	}

	/**
	 * Importar la base de datos
	 */
	private void importarDB() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.favoritos_pregunta)).setCancelable(false).setPositiveButton(getString(R.string.barcode_si), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				procederImportacionBD();

			}
		}).setNegativeButton(getString(R.string.barcode_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();

			}
		});
		AlertDialog alert = builder.create();

		alert.show();

	}

	/**
	 * Importar la base de datos
	 */
	private void importarDriveDB() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.favoritos_pregunta)).setCancelable(false).setPositiveButton(getString(R.string.barcode_si), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				Intent intent1 = new Intent(FavoritosActivity.this, FavoritoDriveActivity.class);

				Bundle b = new Bundle();
				b.putString("MODO", FavoritoDriveActivity.MODO_IMPORTAR);
				intent1.putExtras(b);

				startActivityForResult(intent1, SUB_ACTIVITY_REQUEST_DRIVE);

			}
		}).setNegativeButton(getString(R.string.barcode_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();

			}
		});
		AlertDialog alert = builder.create();

		alert.show();

	}

	/**
	 * Recuperar
	 */
	private void procederImportacionBD() {

		dialog = ProgressDialog.show(FavoritosActivity.this, "", getString(R.string.dialog_procesando), true);

		BackupAsyncTaskResponder backupAsyncTaskResponder = new BackupAsyncTaskResponder() {
			public void backupLoaded(Boolean resultado) {

				if (resultado != null && resultado) {

					consultarDatos(orden);
					if (dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}
					Toast.makeText(FavoritosActivity.this, getString(R.string.ok_importar), Toast.LENGTH_SHORT).show();

				} else {
					// Error al recuperar datos

					if (dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}

					Toast.makeText(FavoritosActivity.this, getString(R.string.error_importar), Toast.LENGTH_LONG).show();

				}
			}
		};

		new BackupAsyncTask(backupAsyncTaskResponder).execute("importar");

	}

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SUB_ACTIVITY_REQUEST_DRIVE:
			if (resultCode == RESULT_OK) {

				consultarDatos(orden);

			}

			break;

		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

}
