package dk.MyMovies.GUI;

import dk.MyMovies.BE.CatMovConnectionBE;
import dk.MyMovies.BE.Category;
import dk.MyMovies.BE.Movie;
import dk.MyMovies.BLL.BLLCatMov;
import dk.MyMovies.BLL.BLLCategory;
import dk.MyMovies.BLL.BLLMovie;
import dk.MyMovies.Exceptions.MyMoviesExceptions;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppController implements Initializable {

    private BLLCategory bllCat = new BLLCategory();
    private BLLMovie bllMov = new BLLMovie();
    private BLLCatMov bllCatMov = new BLLCatMov();
    public Button btnAddCat;
    @FXML
    private CheckBox checkRating;
    @FXML
    private Slider sliderVolume;
    @FXML
    private ToggleButton togglePlayPause;
    @FXML
    private Slider progressSlider;
    @FXML
    private MediaView mediaView;
    @FXML
    private TableView<CatMovConnectionBE> tblMovie;
    @FXML
    private TableColumn<CatMovConnectionBE, String> colName;
    @FXML
    private TableColumn<CatMovConnectionBE, String> colCat;
    @FXML
    private TableColumn<CatMovConnectionBE, Double> colRating;
    @FXML
    private TableColumn<CatMovConnectionBE, String> colLast;
    @FXML
    private TableColumn<CatMovConnectionBE, String> colImdb;
    @FXML
    private ListView<CheckBox> lvCategories;
    @FXML
    private Slider ratingSlider;
    @FXML
    private Label lblSliderValue;
    @FXML
    private Button star1;
    @FXML
    private Button star2;
    @FXML
    private Button star3;
    @FXML
    private Button star4;
    @FXML
    private Button star5;
    @FXML
    private Button star6;
    @FXML
    private Button star7;
    @FXML
    private Button star8;
    @FXML
    private Button star9;
    @FXML
    private Button star10;
    private MediaPlayer player;
    private ChangeListener<MediaPlayer.Status> playPauseListener;
    ImageView playView = new ImageView();
    ImageView pauseView = new ImageView();
    private ObservableList<CatMovConnectionBE> originalItems;
    private FileChooser.ExtensionFilter filter1 = new FileChooser.ExtensionFilter(".mp4 files", "*.mp4");
    private FileChooser.ExtensionFilter filter2 = new FileChooser.ExtensionFilter(".mpeg4 files", "*.mpeg4");
    private static final Logger logger = Logger.getLogger(AppController.class.getName());
    private ContextMenu rightClickMenu;




    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            displayMovies();
            checkForUselessMovies();
        } catch (MyMoviesExceptions | IOException e) {
            logger.log(Level.SEVERE, "Error initializing AppController", e);
            showErrorDialog(new MyMoviesExceptions("Error initializing AppController: ", e));
            throw new RuntimeException(e);
        }
        sliderVolume.setVisible(false);
        rightClickMenu();
        RatingSlider();
        checkBoxCat();
        playPauseImage();
        creatingStars();
        ratingListener();
        pauseMovieTableSelection();
    }


    //////////////////////////////////////////////////////////
    ////////////////////GUI Stuff/////////////////////////////
    /////////////////////////////////////////////////////////

    //display movie data on table
    public void displayMovies() throws MyMoviesExceptions {
        List<CatMovConnectionBE> mapCatMovConnectionBES = bllCatMov.getAllCatMovConnections();
        List<Movie> allMovies = bllMov.getAllMovies();

        // Using getCatMovMap method to avoid duplicate code
        Map<Integer, CatMovConnectionBE> catMovMap = getCatMovMap(mapCatMovConnectionBES);

        // Create a new list to hold all movies
        List<CatMovConnectionBE> allCatMovConnectionBES = new ArrayList<>();
        listAll(allCatMovConnectionBES, allMovies, catMovMap);

        //Observable list created for our search menu
        originalItems = FXCollections.observableArrayList(allCatMovConnectionBES);
        //This can be put into a new method but many things call it, and it would require alot of
        // changing so maybe if we have time we can figure that out.
        if (!allCatMovConnectionBES.isEmpty()) {
            tblMovie.getItems().clear();
            tblMovie.getItems().addAll(allCatMovConnectionBES);
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colImdb.setCellValueFactory(new PropertyValueFactory<>("IMDBRating"));
            colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
            colLast.setCellValueFactory(new PropertyValueFactory<>("lastView"));
            colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        }
    }

    //Lists all the categories in the listview and gives them checkboxes
    public void checkBoxCat() {
        try {
            //clear the list before this is called so when we add a new one, it doesnt duplicate list
            lvCategories.getItems().clear();
            List<Category> categories = bllCat.getAllCategory();
            for (Category category : categories) {
                CheckBox checkBox = new CheckBox(category.getCatName());
                checkBox.setUserData(category.getCatId()); // Store the category ID in the CheckBox
                // Add listener to know if the checkbox is checked or not and calls the updateMovieTable method to update the table accordingly
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateMovieTable());
                lvCategories.getItems().add(checkBox); // Adds the checkbox to the listview
            }
        } catch (MyMoviesExceptions e) {
            logger.log(Level.SEVERE, "Error retrieving all categories: AppController - ", e);
            showErrorDialog(new MyMoviesExceptions("Error retrieving all categories", e));
        }
    }

    //This updates the movie table based on selected categories
    private void updateMovieTable() {
        try {
            List<Integer> selectedCategoryIds = new ArrayList<>();
            for (CheckBox item : lvCategories.getItems()) {
                if (item != null && item.isSelected()) {
                    selectedCategoryIds.add((Integer) item.getUserData());
                }
            }
            List<CatMovConnectionBE> catMovConnectionBES;
            if (selectedCategoryIds.isEmpty()) {
                // Retrieve all movies and their categories
                List<CatMovConnectionBE> mapCatMovConnections = bllCatMov.getAllCatMovConnections();
                List<Movie> allMovies = bllMov.getAllMovies();

                // Using getCatMovMap method to avoid duplicate code in displayMovies
                Map<Integer, CatMovConnectionBE> catMovMap = getCatMovMap(mapCatMovConnections);

                // Create a new list to hold all movies
                catMovConnectionBES = new ArrayList<>();
                // Using listAll method to avoid duplicate code in displayMovies
                listAll(catMovConnectionBES, allMovies, catMovMap);
            } else {
                List<Integer> movieIds = bllCatMov.getMoviesForCategories(selectedCategoryIds);
                catMovConnectionBES = bllCatMov.getCatMovConnectionsByIds(movieIds);
                Map<Integer, CatMovConnectionBE> catMovMap = getCatMovMap(catMovConnectionBES);
                catMovConnectionBES = catMovMap.values().stream().toList();
            }

            //Observable list for search
            originalItems = FXCollections.observableArrayList(catMovConnectionBES);
            //by making a second observable list we keep our list even if search has been removed
            ObservableList<CatMovConnectionBE> observableMovies = FXCollections.observableArrayList(catMovConnectionBES);
            tblMovie.setItems(observableMovies);
        } catch (MyMoviesExceptions e) {
            logger.log(Level.SEVERE, "Error retrieving movies for categories: AppController - ", e);
            showErrorDialog(new MyMoviesExceptions("Error retrieving movies for categories", e));
        }
    }

    //gets all ID checkboxes which have been ticked. made its own method to avoid repeating code
    private List<Integer> getSelectedCategoryIDs(){
        List<Integer> selectedIds = new ArrayList<>();
        for (Object item : lvCategories.getItems()) {
            if (item instanceof CheckBox && ((CheckBox) item).isSelected()) {
                selectedIds.add((Integer) ((CheckBox) item).getUserData());
            }
        }
        return selectedIds;
    }

    private void checkForUselessMovies() throws IOException {
        try {
            List<Movie> useless = bllMov.getUselessMovies();
            if (!useless.isEmpty()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML/DeleteWarningScene.fxml"));
                Parent root = loader.load();
                DeleteWarningSceneController controller = loader.getController();
                controller.setAppController(this);
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setAlwaysOnTop(true);
                stage.show();
            }
        } catch (MyMoviesExceptions e) {
            logger.log(Level.SEVERE, "Error retrieving all movies with a rating below 6 that were last opened 2 years ago - ");
            showErrorDialog((new MyMoviesExceptions("Error retrieving all movies with a rating below 6 that were last opened 2 years ago", e)));
            throw new RuntimeException(e);
        }
    }

    //Error Message Display
    private void showErrorDialog(MyMoviesExceptions e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("This can't be good");
        alert.setHeaderText("Oh No! We ran into a problem!");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    public void RatingSlider() {
        ratingSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                lblSliderValue.setText(String.valueOf(ratingSlider.getValue()).substring(0,3)));

    }

    //This makes it so movies will all be listed in our table regardless of if it has a category or not
    // it takes an input for CatMovConnection, Movie and the Map containing our keys
    private void listAll(List<CatMovConnectionBE> catMovConnectionBES, List<Movie> allMovies, Map<Integer, CatMovConnectionBE> catMovMap) {
        for (Movie movie : allMovies) {
            //We are using the catMovMap to efficiently tell if a movie has a category
            if (catMovMap.containsKey(movie.getId())) {
                // If the movie has a category, add the CatMovConnection to the list
                catMovConnectionBES.add(catMovMap.get(movie.getId()));
            } else {
                // If the movie doesn't have a category, create a new CatMovConnection without a category and add it to the list
                catMovConnectionBES.add(new CatMovConnectionBE(movie.getId(), movie.getName(), movie.getRating(), movie.getIMDBRating(), movie.getFilePath(), movie.getLastView(), -1));
            }
        }
    }

    //Use this to show movies only once if they have multiple categories
    private Map<Integer, CatMovConnectionBE> getCatMovMap(List<CatMovConnectionBE> catMovConnectionBES) {
        //making a map which is similar(can store Objects) to the primary key on a database but its local/faster but wont save when program is closed which is ok in this case
        Map<Integer, CatMovConnectionBE> catMovMap = new HashMap<>();
        for (CatMovConnectionBE catMovConnectionBE : catMovConnectionBES) {
            //I am checking to see if my catMovConnection has a key in the map
            if (catMovMap.containsKey(catMovConnectionBE.getId())) {
                //Gets the CatMovConnection connected to this ID
                CatMovConnectionBE existingCatMovConnectionBE = catMovMap.get(catMovConnectionBE.getId());
                // taking our existing CatMovConnection and adding , + the new category
                existingCatMovConnectionBE.setCategoryName(existingCatMovConnectionBE.getCategoryName() + ", " + catMovConnectionBE.getCategoryName());
            } else {
                //If there is no key then add it to map using the ID as its key with .put(key, value);
                catMovMap.put(catMovConnectionBE.getId(), catMovConnectionBE);
            }
        }
        return catMovMap;
    }

    //////////////////////////////////////////////////////////
    ////////////////////Movie Stuff///////////////////////////
    /////////////////////////////////////////////////////////

    //add new movie
    @FXML
    private void addMovie(ActionEvent actionEvent) throws MyMoviesExceptions {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Movie");
        chooser.getExtensionFilters().addAll(filter1, filter2); //applying filters so we can only select MP4s and MPEG4s
        File selected = chooser.showOpenDialog(tblMovie.getScene().getWindow()); //opening the filechooser in from our window

        if (selected != null) {
            String name = selected.getName().substring(0, selected.getName().indexOf('.'));

            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = currentDate.format(formatter);

            //we don't set rating since you can't get that from just the file alone.
            bllMov.createMovie(name, null, selected.getPath(), formattedDate);
            displayMovies();
        }
    }

    //delete selected movie (if a movie was selected)
    @FXML
    private void deleteMovie(ActionEvent actionEvent) throws IOException {
        Movie selected = tblMovie.getSelectionModel().getSelectedItem();
        if (selected != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML/DeleteMovieScene.fxml"));
            Parent root = loader.load();
            DeleteMovieController controller = loader.getController();
            controller.setData(selected.getId(), selected.getName(), this);
            openNewWindow(root);
        }
    }

    //edit selected movie (if a movie was selected)
    @FXML
    private void editMovie(ActionEvent actionEvent) throws IOException {
        Movie selected = tblMovie.getSelectionModel().getSelectedItem();
        if (selected != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML/EditMovieScene.fxml"));
            Parent root = loader.load();
            EditMovieController controller = loader.getController();
            if(selected.getLastView() == null){
                selected.setLastView("");
            }
            controller.setData(selected.getId(), selected.getName(), String.valueOf(selected.getRating()), String.valueOf(selected.getIMDBRating()), selected.getFilePath(), selected.getLastView(), this);
            openNewWindow(root);
        }
    }

    //open a new window (just to avoid repeating code)
    private void openNewWindow(Parent root) {
        Scene scene = new Scene(root);
        Stage stag = new Stage();
        stag.setScene(scene);
        stag.show();
    }


    //////////////////////////////////////////////////////////
    ////////////////////Category Stuff////////////////////////
    //////////////////////////////////////////////////////////

    //display cat data on table
    public void addCategory(ActionEvent actionEvent) {
        try {
            FXMLLoader editCatScene = new FXMLLoader(getClass().getResource("FXML/EditCatScene.fxml"));
            Parent root = editCatScene.load();
            EditCatSceneController controller = editCatScene.getController();
            controller.setAppController(this);
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Add category");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void setupCategoryListView() {
        // Create a context menu
        ContextMenu contextMenu = new ContextMenu();

        // Create a menu item for removing a category
        MenuItem removeCategoryItem = new MenuItem("Remove Category");
        removeCategoryItem.setOnAction(event -> {
            // Get the selected category
            CheckBox selectedCategory = lvCategories.getSelectionModel().getSelectedItem();
            if (selectedCategory != null) {
                // Get the category ID from the CheckBox's user data
                int categoryId = (int) selectedCategory.getUserData();
                try {
                    // Delete the category
                    bllCat.deleteCategory(categoryId);

                    // Remove the CheckBox from the ListView
                    lvCategories.getItems().remove(selectedCategory);
                    refreshRightClickMenu();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error removing category: AppController - ", e);
                    showErrorDialog(new MyMoviesExceptions("Error removing category", e));
                }
            }
        });
        // Add the menu item to the context menu
        contextMenu.getItems().add(removeCategoryItem);

        // Set the context menu on the ListView
        lvCategories.setContextMenu(contextMenu);
    }

    //////////////////////////////////////////////////////////
    ////////////////////MediaPlayer functions/////////////////
    //////////////////////////////////////////////////////////

    public void togglePlayPause(ActionEvent actionEvent) {
        Movie selected = tblMovie.getSelectionModel().getSelectedItem();
        if (selected != null) {
            File file = new File(selected.getFilePath());
            if (file.exists()) {
                Media media = new Media(file.toURI().toString());
                //added a check to have the button play the newly selected song .getMedia equals .getSource
                if (player == null || !player.getMedia().getSource().equals(media.getSource())) {
                    setMediaPlayer(null);
                }

                // Update the last view date of the selected movie
                LocalDateTime currentDateTime = LocalDateTime.now();
                try {
                    bllMov.updateLastView(currentDateTime, selected.getId());
                    displayMovies();
                } catch (MyMoviesExceptions e) {
                    logger.log(Level.SEVERE, "Error updating last view date", e);
                    showErrorDialog(new MyMoviesExceptions("Error updating last view date", e));
                }

            }
        }

        MediaPlayer.Status status = player.getStatus();

        if (status == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY || status == MediaPlayer.Status.STOPPED) {
            player.play();
        }

        setVolumeSlider();
    }

    private void pauseMovieTableSelection(){
        tblMovie.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (player != null && player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
            }
        });
    }


    private void playPauseImage() {
        Image playImage = new Image("/dk/MyMovies/GUI/Images/playbtn.png");
        Image pauseImage = new Image("/dk/MyMovies/GUI/Images/pausebtn.png");

        playView = new ImageView(playImage);
        playView.setFitWidth(35);
        playView.setFitHeight(35);

        pauseView = new ImageView(pauseImage);
        pauseView.setFitWidth(35);
        pauseView.setFitHeight(35);


        // Set initial graphic
        togglePlayPause.setGraphic(playView);

        // Add the listener to the status property of the media player
        if (player != null) {
            player.statusProperty().addListener(playPauseListener);
        }

        //use css after this
        togglePlayPause.getStyleClass().add("playPauseButton");

    }

    private void playPauseListener() {
        playPauseListener = (obs, oldStatus, newStatus) -> {
            if (newStatus == MediaPlayer.Status.PAUSED || newStatus == MediaPlayer.Status.STOPPED || newStatus == MediaPlayer.Status.READY) {
                togglePlayPause.setGraphic(playView);
            } else if (newStatus == MediaPlayer.Status.PLAYING) {
                togglePlayPause.setGraphic(pauseView);
            }
        };
    }

    public void skipBackward(ActionEvent actionEvent) {
        if (player != null) {
            Duration currentTime = player.getCurrentTime();
            player.seek(Duration.seconds(currentTime.toSeconds() -  10));
        }
    }

    public void skipForward(ActionEvent actionEvent) {
        if (player != null) {
            Duration currentTime = player.getCurrentTime();
            player.seek(Duration.seconds(currentTime.toSeconds() +  10));
        }
    }

    public void Stop(ActionEvent actionEvent) {
        player.stop();
    }


    public void setMediaPlayer(MouseEvent mouseEvent) {
        Movie selected = tblMovie.getSelectionModel().getSelectedItem();
        if(selected !=null){
            File file = new File(selected.getFilePath());
            if(file.exists()) {
                Media media = new Media(file.toURI().toString());
                player = new MediaPlayer(media);
                mediaView.setMediaPlayer(player);
                setProgressSlider();
                //added a listener for the play/pause button when Mediaplayer is created
                playPauseListener();
                if (player != null) {
                    player.statusProperty().addListener(playPauseListener);
                }
            } else {
                System.out.println("File not found: " + selected.getFilePath());
            }
        }
    }


    public void setProgressSlider(){
        if(player != null){
            player.setOnReady(new Runnable() {
                @Override
                public void run() {
                    progressSlider.setMax(player.getTotalDuration().toSeconds()); //setting the max value of the slider to the duration in seconds
                    //duration in mediaplayer is only accessible when status = ready. which is why it needs to be in here
                }
            });

            player.currentTimeProperty().addListener(new ChangeListener<Duration>() {
                @Override
                public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                    progressSlider.setValue(newValue.toSeconds()); //setting the progressbars value to be the new time value
                }
            });
        }
    }

    public void openFullScreen(ActionEvent actionEvent) {
        Stage fullScreenStage = new Stage();
        MediaView fullScreenMediaView = new MediaView();
        fullScreenMediaView.setMediaPlayer(player);
        Scene scene = new Scene(new Group(fullScreenMediaView));
        //add event to watch for escape key being pressed to close full screen
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == KeyCode.ESCAPE) {
                fullScreenStage.close();
            }
        });
        fullScreenStage.setScene(scene);
        fullScreenStage.setFullScreen(true);
        fullScreenStage.show();
    }

    public void setMovieTime(MouseEvent mouseEvent) {
        if(player != null){
            player.seek(Duration.seconds(progressSlider.getValue())); //mediaplayer changes to the slider value (which is in seconds)
        }
    }

    public void toggleVolumeSlider(ActionEvent actionEvent) {
        if(sliderVolume.isVisible()){
            sliderVolume.setVisible(false);
        } else {
            sliderVolume.setVisible(true);
        }
    }
    private void setVolumeSlider() {
        if(player != null){
            sliderVolume.setValue(player.getVolume() * 100 );
            //mediaplayer volume is usually between 0-1 and our slider is between 0-100, so we're multiplying by 100
            //setting our slider value to the mediaplayers automatic volume value

            sliderVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
                //setting the volume of the mediaplayer to be the sliders new value
                double sliderVal = newValue.doubleValue();
                player.setVolume(sliderVal/100);
                //we need to divideby 100 since we multiplied the value by 100 earlier
            });
        }
    }

    public void searchName(KeyEvent keyEvent) throws MyMoviesExceptions {
        TextField source = (TextField) keyEvent.getSource();
        String searchText = source.getText().toLowerCase();

        if (searchText.isEmpty()) {
            // If the search text is empty, repopulate the table with the original items
            tblMovie.getItems().setAll(originalItems);
        } else {
            // Create a new list for the filtered items
            List<CatMovConnectionBE> filteredItems = new ArrayList<>();

            List<Integer> selectedCatIds = getSelectedCategoryIDs();


            if(selectedCatIds.isEmpty()){
                // Filter the original items based on the search text
                for (CatMovConnectionBE item : originalItems) {
                    if(checkRating.isSelected()){
                        if(item.getName().toLowerCase().contains(searchText)
                                && item.getIMDBRating() == Double.parseDouble(lblSliderValue.getText())){
                            filteredItems.add(item);
                        }
                    } else {
                        if (item.getName().toLowerCase().contains(searchText)) {
                            filteredItems.add(item);
                        }
                    }

                }
            } else {
                List<Integer> MovIds = bllCatMov.getMoviesForCategories(selectedCatIds);
                List<CatMovConnectionBE> catMovConnections = bllCatMov.getCatMovConnectionsByIds(MovIds);
                Map<Integer,CatMovConnectionBE> CatMovMap = getCatMovMap(catMovConnections);

                for(Map.Entry<Integer,CatMovConnectionBE> current: CatMovMap.entrySet()){
                    if(checkRating.isSelected()){
                        if(current.getValue().getName().toLowerCase().contains(searchText)
                                && current.getValue().getIMDBRating() == Double.parseDouble(lblSliderValue.getText())){
                            filteredItems.add(CatMovMap.get(current.getKey()));
                        }
                    } else {
                        if (current.getValue().getName().toLowerCase().contains(searchText)) {
                            filteredItems.add(current.getValue());
                        }
                    }
                }

            }

            // Update the table items
            tblMovie.getItems().setAll(filteredItems);
        }
    }


    //////////////////////////////////////////////////////////
    ////////////////////Right Click Menu//////////////////////
    /////////////////////////////////////////////////////////

    private void rightClickMenu(){
        rightClickMenu = new ContextMenu();
        rightClickMenu.getItems().add(rightClickMenuRemoveMovie());
        rightClickMenu.getItems().add(rightClickMenuAddCategory());
        rightClickMenuRemoveCategory();
        setupCategoryListView();
        tblMovie.setContextMenu(rightClickMenu);
    }

    public void refreshRightClickMenu() {
        rightClickMenu();
    }

    private MenuItem rightClickMenuRemoveMovie() {
        MenuItem removeMovie = new MenuItem("Remove Movie");
        removeMovie.setOnAction(mouseClick -> {
            try {
                deleteMovie(mouseClick);
            } catch (IOException e) {
                throw new RuntimeException("Error with right click menu - ", e);
            }
        });
        return removeMovie;
    }

    private Menu rightClickMenuAddCategory(){
        Menu addCategoryMenu = new Menu("Add Category");
        try {
            List<Category> categories = bllCat.getAllCategory();
            for (Category category : categories) {
                // Add Category submenu
                MenuItem categoryItem = new MenuItem(category.getCatName());
                categoryItem.setOnAction(actionEvent -> {
                    CatMovConnectionBE selectedCatMov = tblMovie.getSelectionModel().getSelectedItem();
                    if (selectedCatMov != null) {
                        try {
                            bllCatMov.addMovieToCategory(category.getCatId(), selectedCatMov.getId());
                            // Refresh the movie table to reflect the changes
                            displayMovies();
                        } catch (MyMoviesExceptions e) {
                            logger.log(Level.SEVERE, "Error adding movie to category: AppController - ", e);
                            showErrorDialog(new MyMoviesExceptions("You cannot add duplicate categories to the same movie", e));
                        }
                    }
                });
                addCategoryMenu.getItems().add(categoryItem);
            }
        } catch (MyMoviesExceptions e) {
            logger.log(Level.SEVERE, "Error retrieving all categories: AppController - ", e);
            showErrorDialog(new MyMoviesExceptions("Error retrieving all categories for right click menu", e));
        }
        return addCategoryMenu;
    }

    private void rightClickMenuRemoveCategory() {
        // Delete Category submenu
        Menu removeCategory = new Menu("Remove Category");
        rightClickMenu.getItems().add(removeCategory);

        rightClickMenu.setOnShowing(event -> {
            Movie selectedMovie = tblMovie.getSelectionModel().getSelectedItem();
            if (selectedMovie != null) {
                removeCategory.getItems().clear(); // Clear the old categories
                try {
                    List<CatMovConnectionBE> catMovConnectionBES = bllCatMov.getCategoriesForMovie(selectedMovie.getId());
                    for (CatMovConnectionBE catMovConnectionBE : catMovConnectionBES) {
                        MenuItem categoryItem = new MenuItem(catMovConnectionBE.getCategoryName());
                        categoryItem.setOnAction(actionEvent -> {
                            try {
                                bllCatMov.removeMovieFromCategory(catMovConnectionBE.getCatMovID());
                                // Refresh the movie table to reflect the changes
                                displayMovies();
                            } catch (MyMoviesExceptions e) {
                                logger.log(Level.SEVERE, "Error removing movie from category: AppController", e);
                                showErrorDialog(new MyMoviesExceptions("Error removing movie from category", e));
                            }
                        });
                        removeCategory.getItems().add(categoryItem);
                    }
                } catch (MyMoviesExceptions e) {
                    logger.log(Level.SEVERE, "Error retrieving categories for movie: AppController", e);
                    showErrorDialog(new MyMoviesExceptions("Error retrieving categories for movie in right click menu", e));
                }
            }
        });
    }


    //////////////////////////////////////////////////////////
    ////////////////////Stars Rating//////////////////////////
    /////////////////////////////////////////////////////////


    private void creatingStars(){
        List<Button> buttons = Arrays.asList(star1, star2, star3, star4, star5, star6, star7, star8, star9, star10);
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            button.getStyleClass().add(i % 2 == 0 ? "starEven" : "starOdd");
        }
    }

    @FXML
    private void handleMouseEnter(MouseEvent event) {
        Button hoveredButton = (Button) event.getSource();
        //This converts my string "star#" to an integer .substring uses the 4th spot on each of my IDs(starting at 0) which is the # of the button
        int buttonNumber = Integer.parseInt(hoveredButton.getId().substring(4));

        List<Button> buttons = Arrays.asList(star1, star2, star3, star4, star5, star6, star7, star8, star9, star10);
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i < buttonNumber) {
                //short form of if else statement, and we use a modulus (%) to apply the style accordingly for even or odd
                button.getStyleClass().remove(i % 2 == 0 ? "starEven" : "starOdd");
                button.getStyleClass().add(i % 2 == 0 ? "starHoverEven" : "starHoverOdd");
            }
        }
    }

    @FXML
    private void handleMouseExit(MouseEvent event) {
        Button exitedButton = (Button) event.getSource();
        int buttonNumber = Integer.parseInt(exitedButton.getId().substring(4));

        List<Button> buttons = Arrays.asList(star1, star2, star3, star4, star5, star6, star7, star8, star9, star10);
        for (int i = 0; i < buttonNumber; i++) {
            Button button = buttons.get(i);
            //checks to see if button has been clicked, if not it removes the styleclass and adds the unfilled icon in the proper spots
            if (!button.getStyleClass().contains("starClickedEven") && !button.getStyleClass().contains("starClickedOdd")) {
                button.getStyleClass().removeAll(Arrays.asList("starHoverEven", "starHoverOdd"));
                button.getStyleClass().add(i % 2 == 0 ? "starEven" : "starOdd");
            }
        }
    }

    @FXML
    private void handleClick(MouseEvent event) throws MyMoviesExceptions {
        Button clickedButton = (Button) event.getSource();
        //This converts my string "star#" to an integer .substring uses the 5th letter on each of my IDs which is the # of the button
        int buttonNumber = Integer.parseInt(clickedButton.getId().substring(4));

        List<Button> buttons = Arrays.asList(star1, star2, star3, star4, star5, star6, star7, star8, star9, star10);
        fillingStars(buttonNumber, buttons);
        double rating = buttonNumber * 0.5;

        CatMovConnectionBE selectedMovie = tblMovie.getSelectionModel().getSelectedItem();
        int selectedIndex = tblMovie.getSelectionModel().getSelectedIndex();
        if(selectedMovie != null) {
            try {
                bllMov.setPersonalRating(rating, selectedMovie.getId());
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        tblMovie.requestFocus();
                        tblMovie.getSelectionModel().select(selectedIndex);
                        tblMovie.getFocusModel().focus(selectedIndex);
                    }
                });

            } catch (MyMoviesExceptions e) {
                logger.log(Level.SEVERE, "Error setting personal rating: AppController - ", e);
                showErrorDialog(new MyMoviesExceptions("Error setting personal rating.", e));
            }
            displayMovies();
        }
    }

    private void ratingListener(){
        //this listener sends the rating via .getRating from whatever is selected on the table
        tblMovie.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                //send the .getRating to the method below
                updateStarRating(newSelection.getRating());
            }
        });
    }

    private void updateStarRating(double rating) {
        //the received rating gets multiplied by 2 so it will match our button IDs (1.5 x 2 = star3)
        int buttonNumber = (int) (rating * 2); // Convert rating to button number

        List<Button> buttons = Arrays.asList(star1, star2, star3, star4, star5, star6, star7, star8, star9, star10);
        //send star#, list to method below
        fillingStars(buttonNumber, buttons);
    }

    private void fillingStars(int buttonNumber, List<Button> buttons) {
        //loop made with the star list
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (i < buttonNumber) {
                //adds the clicked image to everything below where clicked
                button.getStyleClass().removeAll(Arrays.asList("starEven", "starOdd", "starHoverEven", "starHoverOdd"));
                button.getStyleClass().add(i % 2 == 0 ? "starClickedEven" : "starClickedOdd");
            } else {
                //adds the unclicked image to everything above where its been clicked
                button.getStyleClass().removeAll(Arrays.asList("starHoverEven", "starHoverOdd", "starClickedEven", "starClickedOdd"));
                button.getStyleClass().add(i % 2 == 0 ? "starEven" : "starOdd");
            }
        }
    }

}