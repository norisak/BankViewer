package com.norisak.bankviewer;

import com.norisak.bankviewer.network.geapi.GeApi;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

	public static Stage stage;
	public static ExecutorService executorService;
	public static List<Runnable> runOnShutdown = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("bank.fxml"));
        primaryStage.setTitle("Bank Viewer v0.1");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();


        int numThreads = 2;

        executorService = Executors.newFixedThreadPool(numThreads);

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				while (true) {
					long beforeTime = System.currentTimeMillis();
					GeApi.performRequest();
					try { // Try to get one request exactly every 5 seconds
						Thread.sleep(beforeTime + 5000 - System.currentTimeMillis());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		IconManager.getInstance();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				for (Runnable r : runOnShutdown){
					r.run();
				}
				executorService.shutdown();
				Platform.exit();
				System.exit(0);
			}
		});

    }


    public static void main(String[] args) {
        launch(args);
    }
}
