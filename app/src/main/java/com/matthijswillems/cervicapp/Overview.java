package com.matthijswillems.cervicapp;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;


public class Overview extends FragmentActivity implements ActionBar.TabListener{

    private static int currentTab;
    private static int getDataCounter;
    private static float getDataRange;
    private static String chartName;

    AppSectionsPagerAdapter mAppSectionsPagerAdapter;


    ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview2);



        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4); //used to load all fragments
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);


            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_overview2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
// When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        System.out.println(mViewPager.getCurrentItem());
        currentTab = mViewPager.getCurrentItem();


    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {



        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            System.out.println("getItem"+i);
            switch (i) {
                case 0:

                    // The other sections of the app are dummy placeholders.
                    Fragment fragment = new DummySectionFragment();
                    Bundle args = new Bundle();
                    args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment.setArguments(args);
                    System.out.println("daily" +fragment);
                    chartName = "daily";
                    return fragment;


                case 1:

                    // The other sections of the app are dummy placeholders.
                    Fragment fragment2 = new DummySectionFragment2();
                    Bundle args2 = new Bundle();
                    args2.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment2.setArguments(args2);
                    System.out.println("weekly" +fragment2);
                    chartName = "weekly";
                    return fragment2;


                case 2:

                    // The other sections of the app are dummy placeholders.
                    Fragment fragment3 = new DummySectionFragment3();
                    Bundle args3 = new Bundle();
                    args3.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment3.setArguments(args3);
                    System.out.println("monthly" +fragment3);
                    chartName = "monthly";
                    return fragment3;

                default:
                    // The other sections of the app are dummy placeholders.
                    Fragment fragment4 = new DummySectionFragment();
                    Bundle args4 = new Bundle();
                    args4.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment4.setArguments(args4);
                    System.out.println("default" +fragment4);
                    return fragment4;


            }


        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String returnString = null;
            switch (position) {
                case 0:
                    returnString = "Daily";

                    break;
                case 1:
                    returnString = "Weekly";

                    break;
                case 2:
                    returnString = "Monthly";

                    break;
            }
            return returnString;

        }
    }

    public static class DummySectionFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false);


            final LineChart chartName = (LineChart) rootView.findViewById(R.id.chart);
            Bundle args = getArguments();
            //initializeChart();
            String graph_description = "Daily Overview";
            //enable logcat output
            chartName.setLogEnabled(true);

            //styling
            chartName.setBackgroundColor(Color.rgb(255, 255, 255));
            chartName.setDescription(graph_description);
            chartName.setNoDataTextDescription("There's currently not enough data to display anything.");
            chartName.setDrawGridBackground(true);
            chartName.setGridBackgroundColor(Color.rgb(0, 174, 239));
            chartName.setDrawBorders(false);
            chartName.setBorderColor(Color.rgb(255, 0, 0));

            chartName.setBorderWidth(0.5f);
            // enable touch gestures
            chartName.setTouchEnabled(true);

            // enable scaling and dragging
            chartName.setDragEnabled(true);
            chartName.setScaleEnabled(true);
            chartName.setPinchZoom(true);

            //set data, hier functieinvoegen die data retrieved

            LineData data = getData(24, 100);

            chartName.setData(data);
            //refresh chart
            chartName.invalidate();

            //set Listener on data
            //chart.setOnChartValueSelectedListener(OnChartValueSelectedListener l)

            //Legend
            // get the legend (only possible after setting data)
            Legend l = chartName.getLegend();

            // modify the legend ...
            l.setPosition(Legend.LegendPosition.LEFT_OF_CHART);
            l.setForm(Legend.LegendForm.LINE);

            l.setTextSize(11f);
            l.setTextColor(Color.BLUE);
            l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
            //set labels
            chartName.getXAxis().setDrawLabels(true);
            chartName.getXAxis().setDrawGridLines(true);


            //((TextView) rootView.findViewById(android.R.id.text1)).setText(
            //      getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
        //functie voor data
        private LineData getData(int count, float range) {
            System.out.println(count+ " "+range);

            String[] mMonths = new String[] {
                    "1st Hour", "2nd Hour", "3rd Hour", "4th Hour", "5th Hour", "6th Hour", "7th Hour", "8th Hour", "9th Hour", "10th Hour", "11th Hour", "12th Hour"
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
            LineDataSet set1 = new LineDataSet(yVals, "Hourly Percentage Score");
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

    }

        public static class DummySectionFragment2 extends Fragment {

            public static final String ARG_SECTION_NUMBER = "section_number";

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
                View rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false);


                final LineChart chart = (LineChart) rootView.findViewById(R.id.chart);
                Bundle args = getArguments();
                //initializeChart();
                String graph_description = "Weekly Overview";
                //enable logcat output
                chart.setLogEnabled(true);

                //styling
                chart.setBackgroundColor(Color.rgb(255, 255, 255));
                chart.setDescription(graph_description);
                chart.setNoDataTextDescription("There's currently not enough data to display anything.");
                chart.setDrawGridBackground(true);
                chart.setGridBackgroundColor(Color.rgb(0, 174, 239));
                chart.setDrawBorders(false);
                chart.setBorderColor(Color.rgb(255, 0, 0));

                chart.setBorderWidth(0.5f);
                // enable touch gestures
                chart.setTouchEnabled(true);

                // enable scaling and dragging
                chart.setDragEnabled(true);
                chart.setScaleEnabled(true);
                chart.setPinchZoom(true);

                //set data, hier functieinvoegen die data retrieved

                LineData data = getData(7,100);

                chart.setData(data);
                //refresh chart
                chart.invalidate();

                //set Listener on data
                //chart.setOnChartValueSelectedListener(OnChartValueSelectedListener l)

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


                //((TextView) rootView.findViewById(android.R.id.text1)).setText(
                //      getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
                return rootView;
            }
            //functie voor data
            private LineData getData(int count, float range) {
                System.out.println(count+ " "+range);

                String[] mMonths = new String[] {
                        "Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7"
                };

                ArrayList<String> xVals = new ArrayList<String>();
                for (int i = 0; i < count; i++) {
                    xVals.add(mMonths[i % 7]); //A bigger number than 7 will never happen though.
                    //it will switch to my monthly overview
                }

                ArrayList<Entry> yVals = new ArrayList<Entry>();

                for (int i = 0; i < count; i++) {
                    float val = (float) (Math.random() * range) + 3;
                    yVals.add(new Entry(val, i));
                }

                // create a dataset and give it a type
                LineDataSet set1 = new LineDataSet(yVals, "Daily Percentage Score");
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

        }

            public static class DummySectionFragment3 extends Fragment {

                public static final String ARG_SECTION_NUMBER = "section_number";

                @Override
                public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                         Bundle savedInstanceState) {
                    View rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false);


                    final LineChart chart = (LineChart) rootView.findViewById(R.id.chart);
                    Bundle args = getArguments();
                    //initializeChart();
                    String graph_description = "Monthly Overview";
                    //enable logcat output
                    chart.setLogEnabled(true);

                    //styling
                    chart.setBackgroundColor(Color.rgb(255, 255, 255));
                    chart.setDescription(graph_description);
                    chart.setNoDataTextDescription("There's currently not enough data to display anything.");
                    chart.setDrawGridBackground(true);
                    chart.setGridBackgroundColor(Color.rgb(0, 174, 239));
                    chart.setDrawBorders(false);
                    chart.setBorderColor(Color.rgb(255, 0, 0));

                    chart.setBorderWidth(0.5f);
                    // enable touch gestures
                    chart.setTouchEnabled(true);

                    // enable scaling and dragging
                    chart.setDragEnabled(true);
                    chart.setScaleEnabled(true);
                    chart.setPinchZoom(true);

                    //set data, hier functieinvoegen die data retrieved

                    LineData data = getData(31,100);

                    chart.setData(data);
                    //refresh chart
                    chart.invalidate();

                    //set Listener on data
                    //chart.setOnChartValueSelectedListener(OnChartValueSelectedListener l)

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


                    //((TextView) rootView.findViewById(android.R.id.text1)).setText(
                    //      getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
                    return rootView;
                }
                //functie voor data
                private LineData getData(int count, float range) {
                    System.out.println(count+ " "+range);

                    String[] mMonths = new String[] {
                            "Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7", "Day 8", "Day 9", "Day 10", "Day 11", "Day 12", "Day 13", "Day 14", "Day 15", "Day 16", "Day 17", "Day 18", "Day 19", "Day 20", "Day 21", "Day 22", "Day 23", "Day 24", "Day 25", "Day 26", "Day 27", "Day 28", "Day 29", "Day 30", "Day 31",
                    };

                    ArrayList<String> xVals = new ArrayList<String>();
                    for (int i = 0; i < count; i++) {
                        xVals.add(mMonths[i % 30]);
                    }

                    ArrayList<Entry> yVals = new ArrayList<Entry>();

                    for (int i = 0; i < count; i++) {
                        float val = (float) (Math.random() * range) + 3;
                        yVals.add(new Entry(val, i));
                    }

                    // create a dataset and give it a type
                    LineDataSet set1 = new LineDataSet(yVals, "Monthly Percentage Score");
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

            }









    }


