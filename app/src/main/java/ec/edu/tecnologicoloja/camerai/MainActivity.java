package ec.edu.tecnologicoloja.camerai;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ec.edu.tecnologicoloja.camerai.ml.ModelUnquant;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_STATE = 0;
    Button btn_camera, btn_gallery;
    private Bitmap bitmap;
    String[] animales={"grillo","tigre","mosca","panda","elefante","cucaracha","cerdo","lombriz","libélula","medusa","paloma","colibrí","mariposa","caballo","nutria","leon","serpiente","perro","gato","venado","estrella de mar","araña","caracol","pulpo","escorpion"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_camera = findViewById(R.id.btn_camera);
        btn_gallery = findViewById(R.id.btn_galery);

        btn_camera.setOnClickListener(this);
        btn_gallery.setOnClickListener(this);
        checkAndRequestPermissions();

    }
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle the result from the camera activity
                    if (result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap rawBitmap = (Bitmap) extras.get("data");

                            // Asegúrate de que el formato del bitmap sea ARGB_8888
                            if (rawBitmap != null) {
                                bitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, true);

                                // Llama a la función predict() con el nuevo bitmap
                                predict();
                            } else {
                                Log.e(TAG, "Raw Bitmap is null. Cannot perform prediction.");
                            }
                        } else {
                            Log.e(TAG, "Extras are null. Cannot perform prediction.");
                        }
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle the result from the gallery activity
                    if (result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                // Utiliza ImageDecoder para decodificar la imagen desde la Uri
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), selectedImageUri);

                                // Decodifica el bitmap y asegúrate de que el formato sea ARGB_8888
                                bitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true);

                                // Llama a la función predict() con el nuevo bitmap
                                predict();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Error decoding bitmap from Uri: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, "Selected image Uri is null. Cannot perform prediction.");
                        }


                    }
                }
            }
    );

    public static int encontrarPosicionDelMayor(float[] array) {
        if (array == null || array.length == 0) {
            // Manejar el caso de un array vacío o nulo según tus necesidades
            return -1;
        }

        float maximo = array[0];
        int posicion = 0;

        for (int i = 1; i < array.length; i++) {
            if (array[i] >= maximo) {
                maximo = array[i];
                Log.e("Maximo: ", "Maximo: " + maximo);
                posicion = i;
            }
        }

        return posicion;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_camera) {
            startCamera();
            Log.e("onClick: ", "onClick: " + "startCamera");
        } else if (view.getId() == R.id.btn_galery) {
            Log.e("onClick: ", "onClick: " + "openGallery");
            openGallery();


        }

    }

    // declarar permisos en tiempo de ejecución para que me proporcione dicho acceso a la aplicación programada.
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_STATE);
                return;  // Solicitar permisos y salir del método
            }
        }
    }



    private void startCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        galleryLauncher.launch(galleryIntent);
    }

    private void predict() {
        Log.d(TAG, "predict()");
        if (bitmap != null) {
            // Escala el bitmap a las dimensiones requeridas
            bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

            try {
                // Inicializa el modelo
                ModelUnquant model = ModelUnquant.newInstance(MainActivity.this);

                // Convierte el bitmap a ByteBuffer


                // Crea el TensorBuffer de entrada
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                tensorImage.load(bitmap);
                ByteBuffer buffer = tensorImage.getBuffer();
                inputFeature0.loadBuffer(buffer);

                // Realiza la inferencia del modelo y obtén los resultados
                ModelUnquant.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                float[] results = outputFeature0.getFloatArray();
                int position = encontrarPosicionDelMayor(results);

                pushToDescriptionActivity(animales[position]);
                Log.d("Result", Arrays.toString(results));

                // Imprime los resultados
                Log.d("ResultPosition", "Result: " + position);

                // Libera los recursos del modelo
                model.close();
            } catch (IOException e) {
                Log.d("ErrorResult", "IO Exception: " + e.getMessage());
                // Maneja la excepción apropiadamente
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("ErrorResult", "Exception: " + e.getMessage());
                // Captura cualquier excepción adicional
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Bitmap is null. Cannot perform prediction.");
        }
    }
    private void pushToDescriptionActivity(String texto) {
        Intent intent = new Intent(this, DescriptionActivity.class);
        intent.putExtra("TEXTO_EXTRA", texto);
        startActivity(intent);
    }


}