package asimov.uva.es.bluechat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import asimov.uva.es.bluechat.Dominio.Chat;

/**
 * Created by Guti on 31/10/2016.
 */

public class Tab_chats extends Fragment implements View.OnClickListener {

    /**
     * Lista donde añadir tarjetas de personas
     */
    private LinearLayout lista;

    /**
     * Lista de chats con contactos
     */
    private List<Chat> historial;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chats, container, false);

        lista = (LinearLayout) rootView.findViewById(R.id.lista_chats);
        historial = Chat.getChats();

        //TODO Stub
        for(int i = 0; i < historial.size(); i++) {
            View v = inflater.inflate(R.layout.tarjeta_contacto, null);
            ((TextView)v.findViewById(R.id.nombre_contacto)).setText(historial.get(i).getPar().getNombre());
            lista.addView(v, 0);
            v.setOnClickListener(this);
        }

        return rootView;
    }

    @Override
    public void onClick(View v) {
        Log.e("rubia", "Vista " + lista.indexOfChild(v));
    }
}
