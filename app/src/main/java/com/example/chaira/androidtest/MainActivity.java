package com.example.chaira.androidtest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {
    SharedPreferences appData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        appData = getSharedPreferences("data", MODE_PRIVATE);
        String code = "";
        Intent intent = getIntent();
        //Obtenemos los datos del login por medio de los parámetros de la url
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            if (state != "error"){
                requestAccessToken(code);
            }else{
                Toast.makeText(getApplicationContext(), "El usuario no autorizó la aplicación", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Abrimos el navegador y redirigimos al usuario al logín oficial de Chairá
    public void login(View view) {
        Toast.makeText(getApplicationContext(), "Abriendo navegador", Toast.LENGTH_LONG).show();
        String urlLogin = "http://chaira.udla.edu.co/api/v0.1/oauth2/authorize.asmx/auth?response_type=code&client_id=17358389435&redirect_uri=http://myapp&state=xyz";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlLogin));
        startActivity(browserIntent);

    }

    //Cerrar sesión
    public void logout(View v){
        String url = "http://chaira.udla.edu.co/api/v0.1/oauth2/resource.asmx/logout";
        JSONObject js= new JSONObject();
        try {
            js.put("access_token", getAccessToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //enviamos la petición
        sendRequest(js, url);

        Toast.makeText(getApplicationContext(), "El usuario ha cerrado sesión", Toast.LENGTH_LONG).show();

        //Limpiamos los datos guardados
        SharedPreferences.Editor editor = appData.edit();
        editor.clear();
        editor.commit();

        Button login =  (Button) findViewById(R.id.loginBtn);
        login.setVisibility(View.VISIBLE);

        TextView txt = (TextView) findViewById(R.id.tc);
        txt.setVisibility(View.VISIBLE);

        Button logout=  (Button) findViewById(R.id.logoutBtn);
        logout.setVisibility(View.GONE);

        ImageView foto = (ImageView) findViewById(R.id.avatar);
        foto.setVisibility(View.GONE);

        TextView showText =  (TextView) findViewById(R.id.showScope);
        showText.setText("");

        Button show=  (Button) findViewById(R.id.showData);
        show.setVisibility(View.GONE);
    }

    //Solicitud access_token
    public void requestAccessToken(String code) {
        String url = "http://chaira.udla.edu.co/api/v0.1/oauth2/authorize.asmx/token";
        JSONObject js= new JSONObject();
        try {
            js.put("title", "authorization_code");
            js.put("code", code);
            js.put("redirect_uri", "http://myapp");
            js.put("client_id", "17358389435");
            js.put("client_secret", "6nyp4vkxg05n8y9jm7yxz9od137w8e");
            js.put("state", "xyz");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(js,url);

        Button login =  (Button) findViewById(R.id.loginBtn);
        login.setVisibility(View.GONE);

        TextView txt = (TextView) findViewById(R.id.tc);
        txt.setVisibility(View.GONE);

        Button show=  (Button) findViewById(R.id.showData);
        show.setVisibility(View.VISIBLE);

        //parseData();
    }

    //Parsea  los datos que se recibieron como respuesta
    public void parseData(View v){
        String scope = null;
        String state = null;
        String jsonObj = getResponse();
        JSONObject jsonData = null;
        System.out.println("json: "+jsonObj);
        try {
            jsonData = new JSONObject(jsonObj);
            setAccessToken(jsonData.getString("access_token"));
            setRefreshToken(jsonData.getString("refresh_token"));
            scope = jsonData.getString("scope");
            state = jsonData.getString("state");
        } catch (JSONException e) {
            e.printStackTrace();
            showScope(e.toString());
        }

        if (state == "error"){
            Toast.makeText(getApplicationContext(), "Ha ocurrido un error", Toast.LENGTH_LONG).show();
        }else{
            showData(scope);
        }
    }

    //Organiza los datos para ser mostrados más adelante
    public void showData(String scope){
        System.out.println(scope);
        scope = scope.substring(1, scope.length()-1);
        System.out.println(scope);
        String urlPic = null, name = null, lastname = null, genero = null, correo = null, rh = null, municipio= null, dpto = null, rol= null, activo= null;
        JSONObject jsonData = null;
        System.out.println("json: "+scope);
        try {
            jsonData = new JSONObject(scope);
            name = jsonData.getString("NOMBRES");
            lastname = jsonData.getString("APELLIDOS");
            correo = jsonData.getString("CORREO");
            genero = jsonData.getString("GENERO");
            rh = jsonData.getString("RH");
            municipio = jsonData.getString("MUNICIPIO");
            dpto = jsonData.getString("DEPARTAMENTO");
            rol = jsonData.getString("ROL");
            activo = jsonData.getString("ESTADO");
            urlPic = jsonData.getString("FOTO");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String dataUser = "<h2>" + name + " " + lastname + "</h2><br>" +
                "<b>Correo institucional: </b>" + correo + "<br>" +
                "<b> Género: </b> " + genero + "<br>" +
                "<b>Tipo de sangre: </b>" + rh + "<br>" +
                "<b>Departamento: </b>" + dpto + "<br>" +
                "<b>Municipio: </b>" + municipio + "<br>" +
                "<b>Rol dentro de la universidad: </b>" + rol + "<br>" +
                "<b>Estado de matrícula: </b>" + activo + "<br>";

        Button showData=  (Button) findViewById(R.id.showData);
        showData.setVisibility(View.GONE);

        ImageView img=  (ImageView) findViewById(R.id.avatar);
        img.setVisibility(View.VISIBLE);

        ImageView avatar=  (ImageView) findViewById(R.id.avatar);
        avatar.setImageBitmap(getFoto(urlPic));


        Button logout=  (Button) findViewById(R.id.logoutBtn);
        logout.setVisibility(View.VISIBLE);

        TextView show = (TextView) findViewById(R.id.showScope);
        show.setText(Html.fromHtml(dataUser));
    }

    //Muestra los datos en un Text View
    public void showScope(String scope){
        TextView show = (TextView) findViewById(R.id.showScope);
        show.setText(scope);
    }

    //Enviar la petición, recibe el Json con el contenido y la url por parámetros
    public void sendRequest(JSONObject content, String url) {
        new SendPostRequest().execute(url, content.toString());
    }

    //Descargar la imagen de usuario para mostrarlas
    private Bitmap getFoto(String url) {
        Bitmap imagen = null;
        try {
            URL imageUrl = new URL(url);
            URLConnection con = imageUrl.openConnection();
            con.connect();
            InputStream is = con.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            imagen = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  imagen;
    }

//Clase privada para realizar y recibir las peticiones

    private class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(String... params) {
            BufferedReader in = null;
            String baseUrl = params[0];
            String jsonObj = params[1];
            JSONObject jsonData = null;

            try {
                jsonData = new JSONObject(jsonObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println(jsonData);

            try {
                System.out.println("url: " + baseUrl);
                System.out.println("json: "+ String.valueOf(jsonData));
                URL uri = new URL(baseUrl);

                HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(jsonData));

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    System.out.println("petición correcta");
                    in = new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line = "";

                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return new String("false : " + responseCode);
                }
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }

        }

        @Override
        protected void onPostExecute(String response) {
            SharedPreferences.Editor editor = appData.edit();
            editor.putString("response", response);
            editor.commit();
        }
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }

    //Getters y setters para guardar los datos en SharedPreferences

    public String getResponse(){
        String str= appData.getString("response", "");
        System.out.println(str);
        return str;
    }

    public String getAccessToken() {
        String at = appData.getString("accessToken", "");
        return at;
    }

    public void setAccessToken(String token){
        SharedPreferences.Editor editor = appData.edit();
        editor.putString("accessToken", token);
        editor.commit();
    }
    public String getRefreshToken() {
        String at = appData.getString("refreshToken", "");
        return at;
    }

    public void setRefreshToken(String token){
        SharedPreferences.Editor editor = appData.edit();
        editor.putString("refreshToken", token);
        editor.commit();
    }


}


