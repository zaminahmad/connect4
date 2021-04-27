package com.internshala.connectfour;

import com.sun.deploy.security.SelectableSecurityManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {

	private static final int COLUMNS=7;
	private static final int ROWS=6;
	private static final int CIRCLE_DIAMETER=80;
	private static final String discColor1="#24303E";
	private static final String discColor2="4CAA88";
	private static String PLAYER_ONE = null;
	private static String PLAYER_TWO = null;
	private Boolean isPlayerOneTurn = true;


	private Disc[][] insertedDiscsArray = new Disc[ROWS][COLUMNS];
	@FXML
	public GridPane rootGridPane;
	@FXML
	public Pane insertedDiscsPane;
	@FXML
	public Label playerNameLabel;
	@FXML
	public Label turn;
	@FXML
	public TextField playerOneTextField, playerTwoTextField;
	@FXML
	public Button setNamesButton;

	private boolean isAllowedToInsert = true;
	private boolean isNamesSet=false;
	public boolean isNewGame=false;

	public void createPlayground(){
			setNamesButton.setOnAction(event -> {
				PLAYER_ONE = playerOneTextField.getText();
				PLAYER_TWO = playerTwoTextField.getText();
				if (!(PLAYER_ONE.isEmpty() || PLAYER_TWO.isEmpty())) {
					isNamesSet = true;
					playerOneTextField.setEditable(false);
					playerTwoTextField.setEditable(false);
					playerNameLabel.setText(PLAYER_ONE);
					turn.setText("TURN");
				}
			});

		Shape rectangleWithHoles = createGameStructuralGrid();
		rootGridPane.add(rectangleWithHoles,0,1);

		List<Rectangle> rectangleList = createClickableColumns();
		for (Rectangle rectangle:rectangleList) {

			rootGridPane.add(rectangle, 0, 1);
		}

	}

	private Shape createGameStructuralGrid(){
		Shape rectangleWithHoles = new Rectangle((COLUMNS+1)*CIRCLE_DIAMETER,(ROWS+1)*CIRCLE_DIAMETER);

		for (int row=0;row<ROWS;row++){
			for (int col=0;col<COLUMNS;col++){
				Circle circle= new Circle();
				circle.setRadius(CIRCLE_DIAMETER/2);
				circle.setCenterX(CIRCLE_DIAMETER/2);
				circle.setCenterY(CIRCLE_DIAMETER/2);
				circle.setSmooth(true);

				circle.setTranslateX(col * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER/4);
				circle.setTranslateY(row * (CIRCLE_DIAMETER+5) + CIRCLE_DIAMETER/4);

				rectangleWithHoles=Shape.subtract(rectangleWithHoles,circle);
			}
		}

		rectangleWithHoles.setFill(Color.WHITE);
		return rectangleWithHoles;
	}


	private List<Rectangle> createClickableColumns(){

		List<Rectangle> rectangleList = new ArrayList<>();

		for (int col=0;col<COLUMNS;col++) {
			Rectangle rectangle = new Rectangle(CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);
			rectangle.setFill(Color.TRANSPARENT);
			rectangle.setTranslateX((CIRCLE_DIAMETER / 4) +col * (CIRCLE_DIAMETER+5));

			rectangle.setOnMouseEntered(event -> rectangle.setFill(Color.valueOf("#eeeeee26")));
			rectangle.setOnMouseExited(event -> rectangle.setFill(Color.TRANSPARENT));

			final int column = col;
			rectangle.setOnMouseClicked(event -> {
				if(isAllowedToInsert) {
					isAllowedToInsert=false;
					insertDisc(new Disc(isPlayerOneTurn), column);
				}
			});
			rectangleList.add(rectangle);

		}
		return rectangleList;
	}
	private void insertDisc(Disc disc,int column){
		if (!isNamesSet){
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Set Names");
			alert.setHeaderText("No Names Set!");
			alert.setContentText("Before playing make sure you have set names of both players.");
			Optional<ButtonType> btnClicked = alert.showAndWait();
			resetGame();
		}else {

			int row = ROWS - 1;
			while (row >= 0) {

				if (getDiscIfPresent(row, column) == null)
					break;
				row--;
			}
			if (row < 0){
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("Connect Four");
				alert.setTitle("Column is Full.");
				alert.setContentText("Please select any other column to insert disc.");
				isAllowedToInsert=true;
				alert.show();
				return;

			}

			insertedDiscsArray[row][column] = disc;
			insertedDiscsPane.getChildren().add(disc);

			int currentRow = row;
			disc.setTranslateX((CIRCLE_DIAMETER / 4) + column * (CIRCLE_DIAMETER + 5));
			TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5), disc);
			translateTransition.setToY(row * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
			translateTransition.setOnFinished(event -> {
				isAllowedToInsert = true;
				if (gameEnded(currentRow, column)) {

					gameOver();
					return;
				}
				if (currentRow == 0 && drawGame()){
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Connect Four");
					alert.setHeaderText("Game Draw!");
					alert.setContentText("Want to play again?");
					ButtonType yesBtn = new ButtonType("Yes");
					ButtonType noBtn = new ButtonType("No, Exit");
					alert.getButtonTypes().setAll(yesBtn, noBtn);
					Platform.runLater(()->{
						Optional<ButtonType> btnClicked = alert.showAndWait();
						if(btnClicked.isPresent() && btnClicked.get()==yesBtn){
							resetGame();
						}else{
							Platform.exit();
							System.exit(0);
						}
					});
				}

				isPlayerOneTurn = !isPlayerOneTurn;
				playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);

			});
			translateTransition.play();
		}
	}



	private boolean gameEnded(int row, int column) {


		List<Point2D> verticalPoints = IntStream.rangeClosed(row-3,row+3)
										.mapToObj(r->new Point2D(r,column))
				                        .collect(Collectors.toList());

		List<Point2D> horizontalPoints = IntStream.rangeClosed(column-3,column+3)
										.mapToObj(col->new Point2D(row,col))
										.collect(Collectors.toList());
		Point2D startPoint1 = new Point2D(row-3,column+3);
		List<Point2D> diagonal1Points=IntStream.rangeClosed(0,6)
										.mapToObj(i->startPoint1.add(i,-i))
										.collect(Collectors.toList());
		Point2D startPoint2 = new Point2D(row-3,column-3);
		List<Point2D> diagonal2Points=IntStream.rangeClosed(0,6)
										.mapToObj(i->startPoint2.add(i,i))
										.collect(Collectors.toList());
		boolean isEnded = (checkCombinations(verticalPoints) || checkCombinations(horizontalPoints))||
				(checkCombinations(diagonal1Points) || checkCombinations(diagonal2Points));

		return isEnded;
	}

	private boolean checkCombinations(List<Point2D> points) {

		int chain=0;
		for (Point2D point:points) {
			int rowIndexForArray = (int) point.getX();
			int columnIndexForArray = (int) point.getY();
			Disc disc= getDiscIfPresent(rowIndexForArray,columnIndexForArray);
			if (disc != null && disc.isPlayerOneMove== isPlayerOneTurn){
				chain++;
				if (chain==4){
					return true;
				}
			}else {
				chain=0;
			}

		}
		return false;
	}

	private Disc getDiscIfPresent(int row , int column){
		if (row>=ROWS || row<0 || column<0 || column>=COLUMNS)
			return null;
		return insertedDiscsArray[row][column];

	}
	private void gameOver() {

		String winner = isPlayerOneTurn?PLAYER_ONE:PLAYER_TWO;

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Connect Four");
		alert.setHeaderText("The Winner is "+winner);
		alert.setContentText("Want to play again?");

		ButtonType yesBtn = new ButtonType("Yes");
		ButtonType noBtn = new ButtonType("No, Exit");
		alert.getButtonTypes().setAll(yesBtn, noBtn);

		Platform.runLater(()->{
			Optional<ButtonType> btnClicked = alert.showAndWait();
			if(btnClicked.isPresent() && btnClicked.get()==yesBtn){

				resetGame();
			}else{
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public void resetGame() {

		insertedDiscsPane.getChildren().clear();

		for (int row=0;row<insertedDiscsArray.length;row++){
			for (int col=0;col<insertedDiscsArray[row].length;col++){
				insertedDiscsArray[row][col]=null;
			}
		}
		isPlayerOneTurn=true;
		playerNameLabel.setText(PLAYER_ONE);
		isAllowedToInsert = true;
		if (isNewGame){
			isNamesSet=false;
			playerOneTextField.clear();
			playerTwoTextField.clear();
			playerOneTextField.setEditable(true);
			playerTwoTextField.setEditable(true);
			playerNameLabel.setText("");
			turn.setText("");
		}
		createPlayground();
	}
	public boolean drawGame(){
		for (int col =0;col<COLUMNS;col++){
			if (insertedDiscsArray[0][col]==null){
				return false;
			}
		}
		return true;
	}

	private  static class Disc extends Circle{
		private final boolean isPlayerOneMove;
		public Disc(boolean isPlayerOneMove){
			this.isPlayerOneMove = isPlayerOneMove;
			setFill(isPlayerOneMove?Color.valueOf(discColor1):Color.valueOf(discColor2));
			setRadius(CIRCLE_DIAMETER/2);
			setCenterX(CIRCLE_DIAMETER/2);
			setCenterY(CIRCLE_DIAMETER/2);

		}

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}
}
