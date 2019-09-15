package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {

    Grapher grapher = new Grapher();
    PhoneConnection phone = new PhoneConnection(grapher);

    @Override
    public void start(Stage stage) {
        grapher.start(stage);
        phone.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}