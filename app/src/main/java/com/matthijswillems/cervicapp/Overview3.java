package com.matthijswillems.cervicapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;

import java.util.ArrayList;


public class Overview3 extends Activity implements OnChartValueSelectedListener{

    String graph_description = "graph_description";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        //setup tab



        LineChart chart = (LineChart)findViewById(R.id.chart);
        //enable logcat output
        chart.setLogEnabled(true);

        //styling
        chart.setBackgroundColor(Color.rgb(255,255,255));
        chart.setDescription(graph_description);
        chart.setNoDataTextDescription("There's currently not enough data to display anything.");
        chart.setDrawGridBackground(true);
        chart.setGridBackgroundColor(Color.rgb(0, 174, 239));
        chart.setDrawBorders(false);
        chart.setBorderColor(Color.rgb(255,0,0));

        chart.setBorderWidth(0.5f);
        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        //set data, hier functieinvoegen die data retrieved

        LineData data = getData(36, 100);

        chart.setData(data);
        //refresh chart
        // chart.invalidate();

        //set Listener on data
        chart.setOnChartValueSelectedListener(this);
        //Legend
        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();

        // modify the legend ...
        l.setPosition(Legend.LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);

        l.setTextSize(11f);
        l.setTextColor(Color.BLUE);
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        //set labels
        chart.getXAxis().setDrawLabels(true);
        chart.getXAxis().setDrawGridLines(true);



    }

    //functie voor data
    private LineData getData(int count, float range) {
        String[] mMonths = new String[] {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec"
        };

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            xVals.add(mMonths[i % 12]);
        }

        ArrayList<Entry> yVals = new ArrayList<Entry>();

        for (int i = 0; i < count; i++) {
            float val = (float) (Math.random() * range) + 3;
            yVals.add(new Entry(val, i));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        set1.setLineWidth(1.75f);
        set1.setCircleSize(3f);
        set1.setColor(Color.WHITE);
        set1.setCircleColor(Color.WHITE);
        set1.setHighLightColor(Color.WHITE);
        set1.setDrawValues(false);




        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);


        return data;
    }


    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        System.out.println("herro is it me you'll rooking fol");
        //hier iets doen als bv een dag in de maand wordt aangeklikt.
    }

    @Override
    public void onNothingSelected() {

    }
}
