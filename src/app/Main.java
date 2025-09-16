package app;

import javafx.application.Application;
import javafx.stage.Stage;
import ui.fx.FxMinesweeperWindow;

public class Main extends Application {
    private static String[] savedArgs;

    public static void main(String[] args) {
        savedArgs = args;
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        int rows = 16, cols = 16, mines = 40;
        if (savedArgs != null && savedArgs.length == 1) {
            switch (savedArgs[0].toLowerCase()) {
                case "beginner"     -> { rows = 9;  cols = 9;  mines = 10; }
                case "intermediate" -> { rows = 16; cols = 16; mines = 40; }
                case "advanced"     -> { rows = 16; cols = 30; mines = 99; }
            }
        } else if (savedArgs != null && savedArgs.length == 3) {
            try {
                rows = Integer.parseInt(savedArgs[0]);
                cols = Integer.parseInt(savedArgs[1]);
                mines = Integer.parseInt(savedArgs[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid custom args. Use: <rows> <cols> <mines>");
            }
        }
        new FxMinesweeperWindow(stage, rows, cols, mines);
    }
}
