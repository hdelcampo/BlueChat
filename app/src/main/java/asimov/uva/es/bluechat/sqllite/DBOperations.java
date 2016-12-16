package asimov.uva.es.bluechat.sqllite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import asimov.uva.es.bluechat.Dominio.Chat;
import asimov.uva.es.bluechat.Dominio.Contacto;
import asimov.uva.es.bluechat.Dominio.Mensaje;

/**
 * Define las operaciones necesarias de la base de datos.
 * @author David Robles Gallardo
 * @author Silvia Arias Herguedas
 * @author Hector Del Campo Pando
 * @author Alberto Gutierrez Perez
 */

public class DBOperations {

    /*Representación de la base de datos*/
    private static DBHelper baseDatos = null;

    /*Consultas a realizar por el gestor de bases de datos*/
    private static final String SQL_READ_MESSAGES = "SELECT * FROM Mensaje WHERE idChat = ? ORDER BY cast(idMensaje as unsigned);";
    private static final String SQL_READ_CONTACT = "SELECT * FROM Contacto WHERE mac = ?;";
    private static final String SQL_READ_ALL_CONTACTS = "SELECT * FROM Contacto;";
    private static final String SQL_READ_CHAT = "SELECT * FROM Chat WHERE idChat = ?;";
    private static final String SQL_READ_ALL_CHATS = String.format("SELECT * FROM %s", DBContract.Chat.TABLE_NAME);
    private static final String SQL_READ_ALL_GRUPOS = String.format("SELECT * FROM %s", DBContract.ChatGrupal.TABLE_NAME);
    private static final String SQL_READ_ALL_PARTICIPANTES_GRUPO = String.format("SELECT * FROM %s WHERE %s = ?", DBContract.ParticipantesGrupo.TABLE_NAME, DBContract.ParticipantesGrupo.COLUMN_NAME_ID_CHAT);
    private static final String SQL_READ_PENDING_CHATS = "SELECT * FROM Chat WHERE idChat IN(SELECT DISTINCT idChat FROM Mensaje m JOIN MensajePendiente mp GROUP BY m.idMensaje)";
    private static final String SQL_READ_PENDING_MESSAGES_CHAT = "SELECT * FROM MensajePendiente JOIN Mensaje USING(idMensaje) WHERE idChat = ? GROUP BY idMensaje ";
    private static final String SQL_GET_CHAT_BY_MAC = "SELECT * FROM Chat WHERE idContacto = ?";
    private static final String SQL_GET_NUM_CHATS = "SELECT COUNT(*) FROM Chat";
    private static final String SQL_GET_NUM_GRUPOS = String.format("SELECT COUNT(*) FROM %s", DBContract.ChatGrupal.TABLE_NAME);
    private static final String SQL_GET_NUM_MSG = "SELECT COUNT(*) FROM Mensaje";

    private static final DBOperations instancia = new DBOperations();

    private DBOperations (){}

    /**
     * Permite obtener instancias de la base de datos
     * @param contexto El contexto de acceso al servicio de base de datos
     */
    public static DBOperations obtenerInstancia(Context contexto) {
        if (baseDatos == null) {
            try {
                baseDatos = new DBHelper(contexto);
            } catch (ExceptionInInitializerError e) {
                Log.e("excepcion", e.getCause().toString());
            }
        }
        return instancia;
    }

    /**
     * Inserta un mensaje en la base de datos
     * @param mensaje Mensaje que se va a insertar
     * @param chat al que pertenece
     */
    public void insertMessage(Mensaje mensaje, Chat chat, boolean pendiente){
        int num = getNumMensajes();
        String imagen;
        if (null == mensaje.getImagen()) {
            imagen = "";
        } else {
            imagen = mensaje.getImagen();
        }

        ContentValues values = new ContentValues();
        values.put(DBContract.Mensaje.COLUMN_NAME_ID, num +1);
        values.put(DBContract.Mensaje.COLUMN_NAME_CONTENT, mensaje.getContenido());
        values.put(DBContract.Mensaje.COLUMN_NAME_IMAGEN, imagen);
        values.put(DBContract.Mensaje.COLUMN_NAME_EMISOR, mensaje.getEmisor().getDireccionMac());
        values.put(DBContract.Mensaje.COLUMN_NAME_FECHA, mensaje.getFecha().toString());
        values.put(DBContract.Mensaje.COLUMN_NAME_ID_CHAT,chat.getIdChat());

        if(pendiente) {
            for (Contacto contacto : chat.getParticipantes()) {
                ContentValues values1 = new ContentValues();
                values1.put(DBContract.MensajePendiente.COLUMN_NAME_ID_MENSAJE, num + 1);
                values1.put(DBContract.MensajePendiente.COLUMN_NAME_ID_CONTACTO, contacto.getDireccionMac());
                getDb().insert(DBContract.MensajePendiente.TABLE_NAME, null, values1);
            }
        }

        /*Inserta la nueva fila*/
        getDb().insert(DBContract.Mensaje.TABLE_NAME, null, values);
    }

    /**
     * Inserta un contacto en la base de datos
     * @param contacto Contacto que se va a insertar
     */
    public void insertContact(Contacto contacto){
        Cursor cursor = getContact(contacto.getDireccionMac());
        cursor.moveToFirst();
        if (cursor.getCount() != 0) {
            cursor.close();
            updateContacto(contacto);
        }

        ContentValues values = new ContentValues();
        values.put(DBContract.Contacto.COLUMN_NAME_MAC, contacto.getDireccionMac());
        values.put(DBContract.Contacto.COLUMN_NAME_NOMBRE, contacto.getNombre());
        values.put(DBContract.Contacto.COLUMN_NAME_IMAGE, contacto.getImagen());
        /*Inserta la nueva fila*/
        getDb().insert(DBContract.Contacto.TABLE_NAME, null, values);
    }

    /**
     * Inserta un contacto en la base de datos
     * @param chat El chat que se va a insertar
     */
    public void insertChat(Chat chat){
        int num = getNumChats();
        chat.setIdChat(String.valueOf(num + 1));
        ContentValues values = new ContentValues();
        values.put(DBContract.Chat.COLUMN_NAME_ID_CHAT, num + 1);
        values.put(DBContract.Chat.COLUMN_NAME_ID_CONTACTO, chat.getPar().getDireccionMac());
        values.put(DBContract.Chat.COLUMN_NAME_NOMBRE, chat.getNombre());
        /*Inserta una nueva fila*/
        getDb().insert(DBContract.Chat.TABLE_NAME, null, values);
    }

    /**
     * Inserta un nuevo chat grupal en la base de datos
     * @param chat a insertar
     */
    public void insertarGrupo (Chat chat) {
        int id = getNumGrupos() + Contacto.getSelf().getDireccionMac().hashCode(); //TODO jeje yoqueselaverdadxdxd
        chat.setIdChat(String.valueOf(id));

        ContentValues values = new ContentValues();
        values.put(DBContract.ChatGrupal.COLUMN_NAME_ID_CHAT, id);
        values.put(DBContract.ChatGrupal.COLUMN_NAME_NOMBRE, chat.getNombre());

        getDb().insert(DBContract.ChatGrupal.TABLE_NAME, null, values);
    }

    /**
     * Asocia un grupo a un contacto
     * @param chat a asociar
     * @param contacto a asociar
     */
    public void insertarContactoEnGrupo (Chat chat, Contacto contacto) {
        ContentValues values = new ContentValues();
        values.put(DBContract.ParticipantesGrupo.COLUMN_NAME_ID_CHAT, chat.getIdChat());
        values.put(DBContract.ParticipantesGrupo.COLUMN_NAME_ID_CONTACTO, contacto.getDireccionMac());

        getDb().insert(DBContract.ParticipantesGrupo.TABLE_NAME, null, values);
    }

    public int getNumGrupos() {
        int num = 0;

        Cursor cursor = getDb().rawQuery(SQL_GET_NUM_GRUPOS,null);
        if(null != cursor && cursor.moveToFirst()){
            num = cursor.getInt(0);
        }

        return num;
    }

    public int getNumChats() {
        int num = 0;

        Cursor cursor = getDb().rawQuery(SQL_GET_NUM_CHATS,null);
        if(null != cursor && cursor.moveToFirst()){
            num = cursor.getInt(0);
        }

        return num;
    }

    public int getNumMensajes() {
        int num = 0;

        Cursor cursor = getDb().rawQuery(SQL_GET_NUM_MSG,null);
        if(null != cursor && cursor.moveToFirst()){
            num = cursor.getInt(0);
        }

        return num;
    }

    /**
     * Devuelve el contacto asociado a una MAC concreta
     * @param mac Mac del contacto
     * @return cursor El cursor al contacto
     */
    public Cursor getContact(String mac){
        String[] args = new String[] {mac};
        return getDb().rawQuery(SQL_READ_CONTACT, args);
    }

    /**
     * Devuelve los mensajes de un chat
     * @return cursor El cursor a los últimos mensajes
     */
    public Cursor getMensajes(Chat chat){
        String[] args = new String[] {chat.getIdChat()};
        Cursor cursor = getDb().rawQuery(SQL_READ_MESSAGES, args);
        return cursor;
    }

    /**
     * Devuelve todos los contactos
     * @return cursor El cursor a los contactos
     */
    public Cursor getAllContacts(){
        Cursor cursor = getDb().rawQuery(SQL_READ_ALL_CONTACTS, null);
        return cursor;
    }

    /**
     * Devuelve un chat
     * @param idChat El identificador del chat
     * @return cursor El cursor al chat
     */
    public Cursor getChat(String idChat){
        String[] args = new String[] {idChat};
        Cursor cursor = getDb().rawQuery(SQL_READ_CHAT, args);
        return cursor;
    }

    public Cursor getChatPorMac(String mac){
        String[] args = new String[] {mac};
        Cursor cursor = getDb().rawQuery(SQL_GET_CHAT_BY_MAC, args);
        return cursor;
    }

    /**
     * Devuelve todos los chats
     * @return cursor El cursor a los chats.
     */
    public Cursor getAllChats (){
        Cursor cursor = getDb().rawQuery(SQL_READ_ALL_CHATS, null);
        return cursor;
    }

    /**
     * Devuelve todos los chats grupales
     * @return cursor El cursor a los chats grupales.
     */
    public Cursor getGrupos () {
        Cursor cursor = getDb().rawQuery(SQL_READ_ALL_GRUPOS, null);
        return cursor;
    }

    /**
     * Devuelve un cursor a todos los participantes de un grupo
     * @return cursor a los participantes
     */
    public Cursor getParticipantesGrupo (String id) {
        String[] args = {id};
        Cursor cursor = getDb().rawQuery(SQL_READ_ALL_PARTICIPANTES_GRUPO, args);
        return cursor;
    }

    public Cursor getChatsPendientes(){
        Cursor cursor = getDb().rawQuery(SQL_READ_PENDING_CHATS, null);
        cursor.moveToFirst();
        return cursor;
    }

    public Cursor getMensajesPendientes(String idChat){
        String[] args = new String[] {idChat};
        Cursor cursor = getDb().rawQuery(SQL_READ_PENDING_MESSAGES_CHAT, args);
        return cursor;
    }

    /**
     * Devuelve la base de datos en modo escritura
     * @return baseDatos La base de datos en modo escritura
     */
    private SQLiteDatabase getDb() {
        return baseDatos.getWritableDatabase();
    }

    /**
     * Marca un mensaje como enviado
     * @param idMensaje del mensaje
     * @param idContacto del contacto
     */
    public void marcarEnviado(String idMensaje, String idContacto) {
        String[] args = new String[] {idMensaje, idContacto};
        getDb().delete(DBContract.MensajePendiente.TABLE_NAME,"idMensaje = ? AND idContacto = ?", args);
    }

    /**
     * Actualiza un contacto con la nueva informacion
     * @param contacto a actualizar
     */
    public void updateContacto(Contacto contacto){
        String nombre = contacto.getNombre();
        String mac = contacto.getDireccionMac();
        String imagen = contacto.getImagen();
        String[] args = new String[]{mac};
        ContentValues values = new ContentValues();
        values.put(DBContract.Contacto.COLUMN_NAME_NOMBRE, nombre);
        values.put(DBContract.Contacto.COLUMN_NAME_IMAGE, imagen);
        getDb().update(DBContract.Contacto.TABLE_NAME,values, "mac = ?", args);

    }
}
