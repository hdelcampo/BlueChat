package asimov.uva.es.bluechat.controladoresVistas;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import asimov.uva.es.bluechat.R;
import asimov.uva.es.bluechat.serviciosConexion.EnvioMensajesPendientes;
import asimov.uva.es.bluechat.serviciosConexion.ServidorBluetooth;
import io.fabric.sdk.android.Fabric;

import static asimov.uva.es.bluechat.R.id.container;

/**
 * Actividad principal de la aplicación
 * Encargada de comprobar la disponibilidad del bluetooth,
 * pedir los permisos pertinentes y activar el mismo.
 * @author David Robles Gallardo
 * @author Silvia Arias Herguedas
 * @author Hector Del Campo Pando
 * @author Alberto Gutierrez Perez
 */
public class ActivityPrincipal extends AppCompatActivity{

    /**
     * Resultado de la solicitud del permiso de localización
     */
    private final int PERMISO_LOCALIZACION = 2;

    /**
     * Resultado de la solicitud de visibilidad del bluetooth
     */
    private final int BLUETOOTH_VISIBLE = 2;

    /**
     * Constante para iniciar la actividad en un tab concreto
     */
    public static final String CHATS = "Chats";

    /**
     * Adaptador bluetooth del dispositivo
     */
    private BluetoothAdapter adaptadorBluetooth;

    /**
     * El dispositivo es compatible con el bluetooth
     */
    private boolean esCompatibleBluetooth;

    /**
     * Tag para debug
     */
    private static final String TAG = "BLUETOOTH";

    /**
     * Tab que muestra los dispositivos encontrados mediante bluetooth
     */
    private TabDescubrir tab_descubrir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        /*
      The {@link android.support.v4.view.PagerAdapter} that will provide
      fragments for each of the sections. We use a
      {@link FragmentPagerAdapter} derivative, which will keep every
      loaded fragment in memory. If this becomes too memory intensive, it
      may be best to switch to a
      {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        /*
      The {@link ViewPager} that will host the section contents.
     */
        ViewPager mViewPager = (ViewPager) findViewById(container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        comprobarBluetooth();
        if( esCompatibleBluetooth ) {
            comprobarPermisos();
        }

        // Abre la tab del historial si se accede por notificacion
        if (null != getIntent().getAction() && getIntent().getAction().equals(CHATS)) {
            mViewPager.setCurrentItem(mViewPager.getAdapter().getCount() - 1);
        }


    }

    /**
     * Comprueba si el disposivo dispone de bluetooth
     */
    private void comprobarBluetooth() {
        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        esCompatibleBluetooth = true;
        if (adaptadorBluetooth == null) {
            Log.d(TAG, "BLUETOOTH NO DISPONIBLE");
            esCompatibleBluetooth = false;
            Toast.makeText(this, R.string.dispositivo_sin_bluetooth, Toast.LENGTH_SHORT).show();
        }else
            if(adaptadorBluetooth.isEnabled()){
                startService(new Intent(this, ServidorBluetooth.class));
                startService(new Intent(this, EnvioMensajesPendientes.class));
            }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != tab_descubrir) {
            getSupportFragmentManager().putFragment(outState, "TabDescubrir", tab_descubrir);
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tab_descubrir = (TabDescubrir) getSupportFragmentManager().getFragment(savedInstanceState, "TabDescubrir");
    }

    /**
     * Comprueba si la aplicación posee los permisos necesarios para poder funcionar
     * De no ser así le pide dichos permisos al usuario
     */
    private void comprobarPermisos() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(ActivityPrincipal.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISO_LOCALIZACION);
        else
            activarBluetooth();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISO_LOCALIZACION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    activarBluetooth();
                else
                    //El usuario no proporciona permisos
                    //mostramos un mensaje indicando que son necesarios
                    Toast.makeText(this, R.string.permisos_denegados, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Pide permiso al usuario para activar el Bluetooth del dispositivo
     */
    private void activarBluetooth(){

        if( BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE != adaptadorBluetooth.getScanMode()) {
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(discoverableIntent, BLUETOOTH_VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case BLUETOOTH_VISIBLE :
                if(resultCode == RESULT_CANCELED)
                    Toast.makeText(this, R.string.info_bluetooth_visible, Toast.LENGTH_SHORT).show();
                else {
                    adaptadorBluetooth.startDiscovery();
                    startService(new Intent(this, ServidorBluetooth.class));
                    startService(new Intent(this, EnvioMensajesPendientes.class));
                }
                break;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(adaptadorBluetooth != null)
            adaptadorBluetooth.cancelDiscovery();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(ActivityPrincipal.this, EnvioMensajesPendientes.class));
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case R.id.action_crear_grupo:
                Intent dialogo = new Intent(this, ActivityCrearGrupo.class);
                startActivity(dialogo);
                break;

            case (R.id.action_settings):
                Intent intentAjustes= new Intent(this, ActivityAjustes.class);
                startActivity(intentAjustes);
                break;

            case (R.id.action_bluetooth):
                Log.d(TAG,"Refrescar");
                if(esCompatibleBluetooth) {
                    comprobarPermisos();
                    adaptadorBluetooth.startDiscovery();
                }else
                    Toast.makeText(this,
                                    R.string.dispositivo_sin_bluetooth,
                                    Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_ayuda:
                Intent intentAyuda = new Intent(this, ActivityAyuda.class);
                startActivity(intentAyuda);
                break;
            default:
                Log.e(TAG, "Elemento de menu desconocido");
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Un {@link FragmentPagerAdapter} que devuelve el fragment correspondiente a una de las tabs
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        /**
         * Número de tabs a mostrar
         */
        private static final int NUM_TABS = 2;

        /**
         * Crea un nuevo {@link SectionsPagerAdapter}
         * @param fm El fragment manager
         */
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
           switch (position){
               case 0:
                   tab_descubrir = new TabDescubrir();
                   return tab_descubrir;
               case 1:
                   return new TabChats();
               default:
                   Log.d(TAG,"Error seleccionando tag");
                   return null;
           }
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_descubrir);
                case 1:
                    return getString(R.string.tab_chats);
            }
            return null;
        }
    }
}
