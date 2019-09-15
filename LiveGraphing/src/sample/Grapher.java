package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 * Created by Stephen Welch on 10/25/2017.
 */
public class Grapher extends Application {


    //private Queue<XYChart.Data> dataQueue = new ArrayBlockingQueue<XYChart.Data>(1000);
    private XYChart.Data<Number, Number> nextData, lastData;
    @Override
    public void start(Stage stage) {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setAnimated(false);
        xAxis.setLabel("X");
        xAxis.setAutoRanging(false);
        xAxis.setForceZeroInRange(true);
        xAxis.setLowerBound(-4);
        xAxis.setUpperBound(4);

        yAxis.setAnimated(false);
        yAxis.setLabel("Y");
        yAxis.setAutoRanging(false);
        xAxis.setForceZeroInRange(true);
        yAxis.setLowerBound(-4);
        yAxis.setUpperBound(4);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("X");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.getData().add(series);

        Scene scene = new Scene(chart, 640, 640);
        stage.setScene(scene);
        stage.show();

        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50);
                    //if(dataQueue.peek() != null)Platform.runLater(() -> series.getData().add(dataQueue.poll()));
                    if(nextData != lastData) Platform.runLater(() -> series.getData().add(nextData));
                    lastData = nextData;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public synchronized void addDataPoint(float x, float y) {
        System.out.println("Adding data point");
        nextData = new XYChart.Data<>(x, y);
        //dataQueue.offer(new XYChart.Data(x, y));
    }

}
