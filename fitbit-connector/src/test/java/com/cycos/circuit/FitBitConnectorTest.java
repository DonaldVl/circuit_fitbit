package com.cycos.circuit;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class FitBitConnectorTest {

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testFitBitConnector() {
        Assert.assertTrue(true);
    }
    
    @Test
    public void createExampleChart() {
        String title = "Our first challenge but not a personal one :-(";
        boolean legend = true;
        boolean tooltips = true;
        Locale urls = Locale.US;

        Map<String, Integer> map = new HashMap<String, Integer>();
//        map.put("Armin", 10000);
//        map.put("Alper", 8000);
        map.put("Michael", 8000);
        map.put("Donald", 1000);

        PieDataset dataset = createPieDataSet(map.entrySet());
        JFreeChart createPieChart = ChartFactory.createPieChart(title, dataset, legend, tooltips, urls);

        try {
            ChartUtilities.saveChartAsJPEG(new File("./src/main/resources/ChallengeResult.jpg"), createPieChart, 500, 300);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception while saving chart as jpeg");
        }
    }

    private static PieDataset createPieDataSet(Set<Entry<String, Integer>> data) {
        DefaultPieDataset result = new DefaultPieDataset();
        for (Entry<String, Integer> entry : data) {
            result.setValue(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
