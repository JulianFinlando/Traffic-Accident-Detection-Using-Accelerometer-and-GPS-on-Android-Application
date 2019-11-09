package sukses.skripsi.crashdetection;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class BayesActivity extends AppCompatActivity {
    private double h1 = 0.50;
    private double h2 = 0.50;
    private double e1h1;
    private double e1h2;
    private double e4h1;
    private double e4h2;
    private double countH1;
    private double countH2;
    private double resultH1;
    private double resultH2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bayes);
        bayesianReasoning();
    }

    public void bayesianReasoning(){
        //count H1
        e1h1 = 0.10;
        e4h1 = 0.10;
        countH1 = e1h1*e4h1*h1;

        //count H2
        e1h2 = 0.90;
        e4h2 = 0.90;
        countH2 = e1h2*e4h2*h2;

        resultH1 = countH1/(countH1+countH2);
        resultH2 = countH2/(countH1+countH2);

        Log.e("H1", String.valueOf(resultH1));
        Log.e("H2", String.valueOf(resultH2));

        Log.e("Total", String.valueOf(resultH1+resultH2));

        if(resultH1 > resultH2){
            Log.e("Hasil","Terjadi Kecelakaan");
        }else if(resultH2 > resultH1){
            Log.e("Hasil","Tidak terjadi kecelakaan");
        }
    }

}
