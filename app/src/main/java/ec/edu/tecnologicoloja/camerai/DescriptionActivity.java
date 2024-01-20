package ec.edu.tecnologicoloja.camerai;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;


import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class DescriptionActivity extends AppCompatActivity {
    TextView txt_title,txt_type,txt_description;
    ImageView img_animal;
    Button btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        txt_title = findViewById(R.id.txt_title);
        txt_type = findViewById(R.id.txt_type);
        txt_description = findViewById(R.id.txt_description);
        img_animal = findViewById(R.id.imgAnimal);
        btn_back = findViewById(R.id.backButton);

        btn_back.setOnClickListener(v -> finish());


        String animal = getIntent().getStringExtra("TEXTO_EXTRA");

        Log.d("AnimalRecibido","******* El animal es: "+animal+" ******* ");

        String json = loadJSONFromResource();

        try {
            // Convierte la cadena JSON en un array JSON
            JSONArray jsonArray = new JSONArray(json);

            // Busca el animal por nombre
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject animalObject = jsonArray.getJSONObject(i);

                String nombre = animalObject.getString("nombre");
                if (nombre.equals(animal)) {
                    // Encuentra el animal por nombre, ahora puedes acceder a sus propiedades
                    String tipo = capitalizeFirstLetter(animalObject.getString("tipo"));
                    String descripcion = capitalizeFirstLetter(animalObject.getString("descripcion"));
                    String img = animalObject.getString("img");
                    // ...

                    // Actualiza las vistas con la información del animal
                    txt_title.setText(capitalizeFirstLetter(nombre));
                    txt_type.setText(tipo);
                    txt_description.setText(descripcion);
                    Glide.with(this)
                            .load("file:///android_asset/animales/" + img)
                            .into(img_animal);
                    // ...

                    break;  // Sal del bucle una vez que encuentres el animal
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromResource() {
        String json;
        try {
            // Lee el archivo JSON desde res/raw/animales.json
            InputStream is = getResources().openRawResource(R.raw.animales);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}