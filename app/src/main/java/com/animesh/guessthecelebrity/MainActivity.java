package com.animesh.guessthecelebrity;

import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // Link to the website https://www.imdb.com/list/ls052283250/
    private ArrayList<String> names = null;
    private ArrayList<String> celebspicsURL = null;
    private ImageView celebPhoto;
    private Button options[] = new Button[4];
    private Toast displayAnswer = null;
    private int loc=0,ansLoc=0;
    // Scrapping the website task
    private class DownloadDB implements Callable<Pair< ArrayList<String>, ArrayList<String> > >{
        private URL link;
        private ArrayList<String> namesOfCelebs;
        private ArrayList<String> URLofCelebs;

        public DownloadDB(String url)  {
            this.namesOfCelebs = new ArrayList<>();
            this.URLofCelebs = new ArrayList<>();
            try {
                this.link = new URL(url);
            }catch(MalformedURLException e){
                e.printStackTrace();
                Log.e("URL malformed","Url is not correct");
            }
        }
        @Override
        public Pair< ArrayList<String>,ArrayList<String> > call() throws Exception{
            StringBuilder htmlcode = new StringBuilder();
            //Pulling the html
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(((HttpURLConnection)
                        link.openConnection()).getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int charac = 0;
            try {
                charac = reader.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (charac != -1) {
                htmlcode.append((char) charac);
                try {
                    charac = reader.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Now scan and get all the links
            Pattern url = Pattern.compile("<img alt=\"(.*?)\"\\nheight=\"[0-9]*\"\nsrc=\"(.*?)\"\nwidth=\"[0-9]*\" />");
            Matcher m = url.matcher(htmlcode.toString());
            int count = 0;
            while(m.find()){

                URLofCelebs.add(m.group(2));
                namesOfCelebs.add(m.group(1));
                Log.i("Celebrities",m.group(1)+" "+m.group(2));

            }
            return new Pair<>(namesOfCelebs,URLofCelebs);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        celebPhoto = findViewById(R.id.output_pic);
        // Downloading Data
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Pair<ArrayList<String>,ArrayList<String> > > data =  executor
                .submit(new DownloadDB("https://www.imdb.com/list/ls052283250/"));
        try {
            Pair< ArrayList<String>,ArrayList<String> > p = data.get();
            names = p.first;
            celebspicsURL = p.second;
        }catch(Exception e){
            e.printStackTrace();
        }
        options[0] = findViewById(R.id.op1);
        options[1] = findViewById(R.id.op2);
        options[2] = findViewById(R.id.op3);
        options[3] = findViewById(R.id.op4);
        //Hold the thread
        setGame();
    }
    private void setGame(){
        loc = getRandomCeleb();
        ExecutorService exec= Executors.newSingleThreadExecutor();
        Future<Bitmap> x = exec.submit(() -> {
            HttpURLConnection imgstream = (HttpURLConnection) new URL(celebspicsURL.get(loc))
                    .openConnection();
            imgstream.connect();
            return BitmapFactory.decodeStream(imgstream.getInputStream());
        });
        try {
            celebPhoto.setImageBitmap(x.get());
            setOptions();
        } catch (ExecutionException e) {
            Log.i("Error","Error occured");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.i("Error","Error occured");
            e.printStackTrace();
        }

    }
    private int getRandomCeleb(){
        double value =  Math.random()*names.size();
        return (int) value;}
    private int getRandomButton(){
        double value = Math.random()*4;
        return (int) value;
    }
    private void setOptions(){
        ansLoc = getRandomButton();
        for(int i=0;i<4;i++){
            if (i==ansLoc) options[i].setText(names.get(loc));
            else{
                int temp = getRandomCeleb();
                temp = (temp==loc)? getRandomCeleb(): temp;
                options[i].setText(names.get(temp));
            }
        }
    }
    public void selectedOption(View view){
        // Get tag from data
        int tag = Integer.parseInt((String) view.getTag());
        if(displayAnswer!=null) displayAnswer.cancel();
        if (tag == ansLoc) {
            Log.i("Answer", "Correct");
             displayAnswer = Toast.makeText(getApplicationContext(), "Correct",
                    Toast.LENGTH_SHORT);

        }else{
            Log.i("Answer", "Incorrect");
            displayAnswer = Toast.makeText(getApplicationContext(), "Incorrect", Toast.LENGTH_SHORT);
        }
        setGame();
        displayAnswer.show();
    }
}