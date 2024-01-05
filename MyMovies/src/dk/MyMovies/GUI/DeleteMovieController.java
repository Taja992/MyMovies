package dk.MyMovies.GUI;

import dk.MyMovies.BLL.BLLManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DeleteMovieController implements Initializable {

    @FXML
    private Label lblMovName;
    private int ID;
    BLLManager BLL = new BLLManager();

    public void deleteMovie(ActionEvent actionEvent) {
        BLL.deleteMovie(ID);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setData(int ID, String name){
        lblMovName.setText(name);
        this.ID = ID;
    }

}