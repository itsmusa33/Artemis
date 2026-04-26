import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application{

    @Override
    public void start(Stage stage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainLayout.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();

        Scene scene = new Scene(root, 1220, 780);

        stage.setTitle("Artillery Simulator - Artemis");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.setFullScreenExitHint("");

        scene.setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.F11)
                stage.setFullScreen(!stage.isFullScreen());
        });

        stage.show();
        Platform.runLater(controller::init_canvas);
    }

    public static void main(String[] args){
        launch(args);
    }
}
