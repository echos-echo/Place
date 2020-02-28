package place.client.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.client.model.ClientModel;
import place.client.network.NetworkClient;


import java.text.SimpleDateFormat;
import java.util.*;

public class PlaceGUI extends Application implements Observer {
    /**
     * The gaps that should be put in between color choices
     */
    private static final int BOTTOM_GAPS = 5;
    /**
     * The spacing that is used on the bottom HBox
     */
    private static final int BOTTOM_HBOX_SPACING = 5;
    /**
     * The minimum size of the GridPane.
     */
    private static final int MIN_GRID_SIZE = 650;
    /**
     * The size of the tile preview(s)
     */
    private static final int TILE_PREVIEW_SIZE = 50;
    /**
     * The size of all the control items
     */
    private static final int COLOR_CONTROL_SIZE = TILE_PREVIEW_SIZE/2;
    /**
     * The padding Insets for the color bar on the top
     */
    private static final Insets COLOR_BAR_INSETS = new Insets(10, 5, 10,5);
    /**
     * The padding Insets for the bottom HBox
     */
    private static final Insets BOTTOM_HBOX_INSETS = new Insets(15,25,20,25);
    /**
     * The padding Insets for the main grid
     */
    private static final Insets MAIN_GRID_INSETS = new Insets(0, 10, 10, 10);
    /**
     * The date formatter
     */
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
    /**
     * The time formatter
     */
    private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    /**
     * The username that a user decides on
     */
    private String username;
    /**
     * host name that we're connected to
     */
    private String hostname;
    /**
     * The port we are connected to
     */
    private int port;
    /**
     * The model used to control the board
     */
    private ClientModel model;
    /**
     * The connection to the server.
     */
    private NetworkClient serverConn;
    /**
     * The scene of the GUI.
     */
    private Scene scene;
    /**
     * The main GridPane of rectangles that represent all of the PlaceTiles in the game.
     */
    private GridPane mainGrid;
    /**
     * The currently selected PlaceColor that will be used to send to the server if a PlaceTile is clicked on.
     */
    private int currentColor = 0;
    /**
     * The size that each tile should be on screen
     */
    private int rectSize;
    /**
     * The text that is used to show the name of the currently selected color in the bottom HBox.
     */
    private Text selectedColorName;
    /**
     * A Rectangle which has a preview of the currently selected color.
     */
    private Rectangle selectedColorPreview;
    /**
     * The preview rectangle that is the color of the PlaceTile that the mouse is currently hovering over.
     */
    private Rectangle tilePreview;
    /**
     * The location of the PlaceTile that the mouse is currently over.
     */
    private Text tileLocationInfo;
    /**
     * The owner of the PlaceTile that the mouse is currently over.
     */
    private Text tileOwnerInfo;
    /**
     * The date on which the PlaceTile that the mouse is currently over was set.
     */
    private Text tileCreateDateInfo;
    /**
     * The time when the PlaceTile that the mouse is currently over was set.
     */
    private Text tileCreateTimeInfo;

    //==========================================================

    /**
     * Initializes the client
     *
     * @throws Exception any sort of exception
     */
    @Override
    public void init() throws Exception
    {
        super.init();

        List < String > parameters = super.getParameters().getRaw();
        this.hostname = parameters.get(0);
        this.port = Integer.parseInt(parameters.get(1));
        this.username = parameters.get(2);
        this.model = new ClientModel();
        try
        {
            this.serverConn = new NetworkClient(this.hostname, this.port, this.username, this.model);
        }
        catch(PlaceException e)
        {
            this.serverConn.close();
            System.err.println(e);
        }
        this.model.addObserver(this);

        this.rectSize = MIN_GRID_SIZE / this.model.getDIM();
    }

    /**
     * Constructs the GUI and displays it after each update from client(s)
     *
     * @param primaryStage the stage where the GUI is shown
     */
    @Override
    public void start(Stage primaryStage)
    {
        BorderPane root = new BorderPane();
        root.setTop( buildColorBar() );
        root.setCenter( this.mainGrid = buildMainGrid() );
        root.setBottom( buildBottomBox() );

        this.serverConn.start();
        this.scene = new Scene(root);
        primaryStage.setScene(this.scene);
        primaryStage.setTitle("Place: " + this.username + "@" + this.hostname + ":" + this.port);

        primaryStage.setResizable(false);

        primaryStage.show();
    }

    /**
     * Builds a GridPane of singular PlaceTiles
     *
     * @return a GridPane which is DIM x DIM housing representations of each PlaceTile.
     */
    private GridPane buildMainGrid()
    {
        GridPane mainGrid = new GridPane();
        mainGrid.setPadding(MAIN_GRID_INSETS);
        mainGrid.setStyle("-fx-background-color:#999;");

        for(int row = 0; row < this.model.getDIM(); ++row)
        {
            for (int col = 0; col < this.model.getDIM(); ++col)
            {
                PlaceTile tile = this.model.getTile(row, col);
                Rectangle tileRectangle = buildSingleTile(tile);
                mainGrid.add(tileRectangle, col, row);
            }
        }
        return mainGrid;
    }

    /**
     * Builds a Rectangle object that represents a single PlaceTile.
     *
     * @param tile The tile we want to represent.
     *
     * @return A Rectangle that represents the PlaceTile passed.
     */
    private Rectangle buildSingleTile(PlaceTile tile)
    {
        int row = tile.getRow();
        int col = tile.getCol();

        PlaceColor tileColor = tile.getColor();

        Rectangle tileRectangle = new Rectangle(this.rectSize, this.rectSize,
                Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()));

        String date = DATE_FORMAT.format(new Date(tile.getTime()));
        String time = TIME_FORMAT.format(new Date(tile.getTime()));

        tileRectangle.setOnMouseEntered(
                (ActionEvent) ->
                        javafx.application.Platform.runLater(() ->
                        {
                            tileRectangle.setFill(getCurrentColor());
                            this.tilePreview.setFill(
                                    Color.rgb(tileColor.getRed(),tileColor.getGreen(),tileColor.getBlue())
                            );
                            this.tileLocationInfo.setText("(" + row +
                                    "," + col + ")");
                            this.tileOwnerInfo.setText(tile.getOwner());
                            this.tileCreateDateInfo.setText(date);
                            this.tileCreateTimeInfo.setText(time);
                        })
        );

        tileRectangle.setOnMouseExited(
                (ActionEvent) -> javafx.application.Platform.runLater(() ->
                        tileRectangle.setFill(Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()))
                )
        );

        tileRectangle.setOnMouseClicked(
                (ActionEvent) -> this.serverConn.sendTile(
                        new PlaceTile(row, col, this.username, PlaceColor.values()[this.currentColor], System.currentTimeMillis())
                )
        );

        return tileRectangle;
    }

    /**
     * A small method to return a Color object of the currently selected color.
     *
     * @return a Color object of the currently selected color.
     */
    private Color getCurrentColor()
    {
        PlaceColor currentSelected = PlaceColor.values()[this.currentColor];
        return Color.rgb(currentSelected.getRed(), currentSelected.getGreen(), currentSelected.getBlue());
    }

    /**
     * Builds the color bar where users can select the color they want to use
     *
     * @return A FlowPane which houses the color bar.
     */
    private FlowPane buildColorBar()
    {
        FlowPane colorBar = new FlowPane(BOTTOM_GAPS, BOTTOM_GAPS);
        colorBar.setPadding(COLOR_BAR_INSETS);
        colorBar.setStyle("-fx-background-color: #999;");
        colorBar.setAlignment(Pos.CENTER);

        for( PlaceColor color : PlaceColor.values())
        {
            Rectangle colorChoice = new Rectangle(COLOR_CONTROL_SIZE, COLOR_CONTROL_SIZE,
                    Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
            colorChoice.setStroke(Color.WHITE);
            colorChoice.setOnMouseClicked( (EventAction) -> this.setCurrentColor(color.getNumber()) );
            colorChoice.setOnMouseEntered( (EventAction) -> scene.setCursor(Cursor.HAND) );
            colorChoice.setOnMouseExited( (EventAction) -> scene.setCursor(Cursor.DEFAULT) );

            colorBar.getChildren().add(colorChoice);
        }

        return colorBar;
    }

    /**
     * Changes the selected color when a user clicks on a color in the color bar
     *
     * @param color The color that the user selected.
     */
    private void setCurrentColor(int color)
    {
        this.currentColor = color;

        PlaceColor selectedColor = PlaceColor.values()[color];

        javafx.application.Platform.runLater(
                () ->
                {
                    this.selectedColorName.setText(selectedColor.name());
                    this.selectedColorPreview.setFill(
                            Color.rgb(selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue())
                    );
                }
        );
    }

    /**
     * Builds the preview VBox which houses the preview of the PlaceTile the mouse is over as well as the currently
     * selected color.
     *
     * @return A HBox which houses the preview of the PlaceTile the mouse is hovering over.
     */
    private HBox buildBottomBox()
    {
        HBox bottomBox = new HBox();
        bottomBox.setSpacing( BOTTOM_HBOX_SPACING );
        bottomBox.setPadding( BOTTOM_HBOX_INSETS );
        bottomBox.setStyle("-fx-background-color:#999;");
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);

        Text selectedColorPre = new Text("Selected color");
        PlaceColor selected = PlaceColor.values()[this.currentColor];
        this.selectedColorName = new Text(selected.name());
        this.selectedColorPreview = new Rectangle(
                TILE_PREVIEW_SIZE, TILE_PREVIEW_SIZE, Color.rgb(selected.getRed(), selected.getGreen(), selected.getBlue())
        );

        selectedColorPre.setFill(Color.WHITE);
        this.selectedColorName.setFill(Color.WHITE);
        this.selectedColorPreview.setStroke(Color.DARKGREY);
        this.selectedColorPreview.setStrokeWidth(1.5);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text tileInfoHeader = new Text("Tile info");
        Tooltip tileInfoAbout = new Tooltip("Displays information about the tile your mouse is over.");
        Tooltip.install(tileInfoHeader, tileInfoAbout);

        this.tilePreview = new Rectangle(
                TILE_PREVIEW_SIZE, TILE_PREVIEW_SIZE, Color.rgb(selected.getRed(), selected.getGreen(), selected.getBlue())
        );
        this.tileLocationInfo = new Text("(0,0)");
        this.tileOwnerInfo = new Text("Owner");
        this.tileCreateDateInfo = new Text("12/31/69");
        this.tileCreateTimeInfo = new Text("19:00:00");

        tileInfoHeader.setFill(Color.WHITE);
        this.tilePreview.setStroke(Color.DARKGREY);
        this.tilePreview.setStrokeWidth(1.5);
        this.tileLocationInfo.setFill(Color.WHITE);
        this.tileOwnerInfo.setFill(Color.WHITE);
        this.tileCreateDateInfo.setFill(Color.WHITE);
        this.tileCreateTimeInfo.setFill(Color.WHITE);

        bottomBox.getChildren().addAll(
                selectedColorPre,
                this.selectedColorPreview,
                this.selectedColorName,
                spacer,
                tileInfoHeader,
                this.tilePreview,
                this.tileLocationInfo,
                this.tileOwnerInfo,
                this.tileCreateDateInfo,
                this.tileCreateTimeInfo
        );

        return bottomBox;
    }

    /**
     * The update method that is called by an Observable
     *
     * @param o The Observable (it attaches itself to make sure we are being updated from the correct model.
     * @param tile The tile that is being sent for update.
     */
    public void update(Observable o, Object tile)
    {
        assert o == this.model : "Wrong observable.";

        if(tile instanceof PlaceTile)
        {
            changeTile((PlaceTile) tile);
        }
        else
        {
            this.serverConn.error("Something's wrong.\n" +
                    "Restarting board to account for errors...uwu...");
            redrawGrid();
        }
    }

    /**
     * When the update method is called, it sets a PlaceTile which then updates on the Place board
     *
     * @param tile The tile that was changed
     */
    private void changeTile(PlaceTile tile)
    {
        javafx.application.Platform.runLater(
                () -> this.mainGrid.add(buildSingleTile(tile), tile.getCol(), tile.getRow())
        );
    }

    /**
     * This redraws the entire GridPane in the event the update method is sent something that isn't a PlaceTile.
     */
    private void redrawGrid()
    {
        for (int row = 0; row < this.model.getDIM(); ++row)
        {
            for (int col = 0; col < this.model.getDIM(); ++col)
            {
                PlaceTile tile = this.model.getTile(row, col);
                javafx.application.Platform.runLater(() -> mainGrid.add(buildSingleTile(tile), tile.getCol(), tile.getRow()));
            }
        }
    }

    /**
     * Closes connection to the server once the GUI is closer
     *
     * @throws Exception
     */
    public void stop() throws Exception
    {
        super.stop();
        this.serverConn.close();
    }

    /**
     * Launches a PlaceGUI.
     *
     * @param args The arguments that the GUI should be built with.
     *             args should have: host, port, and username in that order.
     */
    public static void main(String[] args)
    {
        if(args.length != 3)
        {
            System.err.println("Please run the GUI as:");
            System.err.println("$ java PlaceGUI host port username");
            return;
        }

        try
        {
            Application.launch(args);
        }
        catch(Exception e)
        {
            System.err.println(e);
        }
    }

}
