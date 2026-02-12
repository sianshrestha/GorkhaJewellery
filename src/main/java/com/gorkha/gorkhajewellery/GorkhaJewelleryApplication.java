package com.gorkha.gorkhajewellery;

import atlantafx.base.theme.PrimerLight; // <--- NEW IMPORT
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class GorkhaJewelleryApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(GorkhaJewelleryApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(context::getBean);
        Parent root = fxmlLoader.load();

        // --- NEW: Load Custom CSS ---
        root.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        Scene scene = new Scene(root);
        stage.setTitle("Gorkha Jewellery Invoice System");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
    }
}