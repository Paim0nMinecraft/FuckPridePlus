package cc.paimonmc.fuckprideplus;


import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class FxUtil {
    public static File openFileChooser(String title,String desc){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(desc, "*." + desc)
        );
        Stage stage = new Stage();
        return fileChooser.showOpenDialog(stage);
    }
}
