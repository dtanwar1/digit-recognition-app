package com.example.digitrecognitionapp;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.net.Uri;




import com.example.digitrecognitionapp.ml.Rn50Lite;


import org.supercsv.io.ICsvMapWriter;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import android.widget.ImageView;

import java.io.IOException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;

//@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class FirstFragment extends Fragment {
    //Motion

    private ActivityResultLauncher<Intent> cameraLauncher;

    private ActivityResultLauncher<Intent> galleryLauncher;

    //Acceleration sensor: TYPE_ACCELEROMETER: include gravity


    private ImageView IVPreviewImage;
    int SELECT_PICTURE = 200;

    //Gyroscope sensor: TYPE_GYROSCOPE


    //CSV file to store data from accelerator and gyroscope
    //private String[] HEADER = new String[] { " Time ", "acc -x ", "acc-y", "acc-Z", "accuracy"};
    private ICsvMapWriter beanWriter = null;

    //Recorder and player part
    private TextView phrases;
    private TextView predicted;
    private Button clickShow;

    private Button start,pause,play, stop, type, pause2, next, previous, loop, switch_btn, camera;

    private SurfaceView mvideo;






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("sensor", "sensor destroyed");
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }












    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("111", "onViewCreated");
        IVPreviewImage = view.findViewById(R.id.IVPreviewImage);
        phrases = view.findViewById(R.id.phrases);
        predicted = view.findViewById(R.id.textView3);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        long time = System.currentTimeMillis();
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        Context context = getContext();
                        MediaStore.Images.Media.insertImage(context.getContentResolver(),
                                imageBitmap, "img", "Image saved from AudioApp");

                        try{
                            //Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), selectedImageUri);


                            int width = imageBitmap.getWidth();
                            int height = imageBitmap.getHeight();

                            // Create a new bitmap for the grayscale image
                            Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                            // Iterate through each pixel in the original image
                            for (int x = 0; x < width; x++) {
                                for (int y = 0; y < height; y++) {
                                    // Get the color of the current pixel
                                    int pixelColor = imageBitmap.getPixel(x, y);

                                    // Extract the RGB values
                                    int red = Color.red(pixelColor);
                                    int green = Color.green(pixelColor);
                                    int blue = Color.blue(pixelColor);

                                    // Calculate the grayscale value using the provided formula
                                    int grayValue = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

//                                        if (grayValue > 169) {
                                    if (grayValue > 100) {
                                        grayValue = 0;
                                    }else{
                                        grayValue = 255;
                                    }


                                    // Set the pixel in the grayscale bitmap
                                    int grayPixel = Color.rgb(grayValue, grayValue, grayValue);
                                    grayscaleBitmap.setPixel(x, y, grayPixel);
                                }
                            }

                            IVPreviewImage.setImageBitmap(grayscaleBitmap);

                            try {

                                ImageProcessor imageProcessor =
                                        new ImageProcessor.Builder()
                                                .add(new ResizeOp(32, 32, ResizeMethod.BILINEAR))
//                                                    .add(new NormalizeOp())
                                                .build();
                                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                                tensorImage.load(grayscaleBitmap);

                                tensorImage = imageProcessor.process(tensorImage);

                                Rn50Lite model = Rn50Lite.newInstance(context);


                                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
                                inputFeature0.loadBuffer(tensorImage.getBuffer());


                                Rn50Lite.Outputs outputs = model.process(inputFeature0);
                                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                                float[] myop = outputFeature0.getFloatArray();
                                float val = 0.0f;
                                int a= -1;

                                for(int i = 0; i< myop.length;i++){

                                    val = Math.max(myop[i],val);
                                    if (val == myop[i]){
                                        a = i;
                                    }
                                }
                                String b = Integer.toString(a);
                                long timeA = System.currentTimeMillis() - time;
                                Log.i("Output", b);

                                predicted.setText("Predicted text :" +b);

                                phrases.setText("Latency (ms) : " +timeA);


                                // Releases model resources if no longer used.
                                model.close();
                            } catch (IOException e) {
                                // TODO Handle the exception
                            }
                        }catch (Exception ex){
                            System.out.println(ex);
                        }
                    }
                }
        );



        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            //IVPreviewImage.setImageURI(selectedImageUri);
                            Context context = getContext();


                            try{
                                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), selectedImageUri);


                                int width = originalBitmap.getWidth();
                                int height = originalBitmap.getHeight();

                                // Create a new bitmap for the grayscale image
                                Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                                // Iterate through each pixel in the original image
                                for (int x = 0; x < width; x++) {
                                    for (int y = 0; y < height; y++) {
                                        // Get the color of the current pixel
                                        int pixelColor = originalBitmap.getPixel(x, y);

                                        // Extract the RGB values
                                        int red = Color.red(pixelColor);
                                        int green = Color.green(pixelColor);
                                        int blue = Color.blue(pixelColor);

                                        // Calculate the grayscale value using the provided formula
                                        int grayValue = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

//                                        if (grayValue > 169) {
                                        if (grayValue > 100) {
                                            grayValue = 0;
                                        }else{
                                            grayValue = 255;
                                        }


                                        // Set the pixel in the grayscale bitmap
                                        int grayPixel = Color.rgb(grayValue, grayValue, grayValue);
                                        grayscaleBitmap.setPixel(x, y, grayPixel);
                                    }
                                }

                                IVPreviewImage.setImageBitmap(grayscaleBitmap);

                                try {
//                                    move to global if shit latency
                                    ImageProcessor imageProcessor =
                                            new ImageProcessor.Builder()
                                                    .add(new ResizeOp(32, 32, ResizeMethod.BILINEAR))
//                                                    .add(new NormalizeOp())
                                                    .build();
                                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                                    tensorImage.load(grayscaleBitmap);

                                    tensorImage = imageProcessor.process(tensorImage);

                                    Rn50Lite model = Rn50Lite.newInstance(context);

                                    // Creates inputs for reference.
                                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
                                    inputFeature0.loadBuffer(tensorImage.getBuffer());

                                    // Runs model inference and gets result.
                                    Rn50Lite.Outputs outputs = model.process(inputFeature0);
                                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                                    float[] myop = outputFeature0.getFloatArray();
                                    float val = 0.0f;
                                    int a= -1;

                                    for(int i = 0; i< myop.length;i++){

                                        val = Math.max(myop[i],val);
                                        if (val == myop[i]){
                                             a = i;
                                        }
                                    }
                                    String b = Integer.toString(a);
                                    Log.i("Output", b);






                                    // Releases model resources if no longer used.
                                    model.close();
                                } catch (IOException e) {
                                    // TODO Handle the exception
                                }
                            }catch (Exception ex){
                                System.out.println(ex);
                            }
                        }
                    }
                });

        start = view.findViewById(R.id.start);
        pause = view.findViewById(R.id.pause);
        pause.setEnabled(false);
        play = view.findViewById(R.id.play);
        stop = view.findViewById(R.id.stop);
        stop.setEnabled(false);

        clickShow = view.findViewById(R.id.click_display);

        mvideo = view.findViewById(R.id.surfaceView);
        type = view.findViewById(R.id.type);
        loop = view.findViewById(R.id.loop);
        camera = view.findViewById(R.id.camera);
        pause2 = view.findViewById(R.id.pause2);
        pause2.setEnabled(false);
        next = view.findViewById(R.id.next);
        previous = view.findViewById(R.id.previous);
        mvideo.getHolder().setKeepScreenOn(true);//keep screen lighting








        clickShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    Intent newIntent = Intent.createChooser(intent,"Select Picture");
                    if (galleryLauncher != null) {
                        galleryLauncher.launch(newIntent);
                    }
                }catch(Exception ex){
                    System.out.println(ex);
                }
            }
        });







        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(cameraLauncher!=null) {
                    cameraLauncher.launch(intent);
                }
            }
        });






    }


}
