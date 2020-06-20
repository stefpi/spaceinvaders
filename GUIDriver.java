/*----------------------------------------------------*
 | Name: Stefan Pitigoi
 | Class: ICS3U
 | Date: 19/06/20
 |
 | A simple, but fun, javafx spaceinvader game.
 |
 | Some of the more complex concepts were learned from AlmasB's github project:
 | https://github.com/AlmasB/FXTutorials/blob/master/src/com/almasb/invaders/SpaceInvadersApp.java.
 | His project is very simple, and I listed all of the code that I took or repurposed from his project. I believe that my
 | project goes above and beyond his and is quite different in many aspects. To see his project, he has a video guide
 | which can be found at https://www.youtube.com/watch?v=FVo1fm52hz0.
 *----------------------------------------------------*/

package spaceinvaders;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GUIDriver extends Application {
    final static int WIDTH = 1000;
    final static int HEIGHT = 1000;

    boolean playerWon = false;
    boolean gameEnd = false;
    boolean gameStart = false;

    Group root;
    Group sprites;
    AnimationTimer timer;
    Sprite player;
    Button play;
    int lives;
    Text livesText = new Text("");
    int fireDelay;

    Text gtime = new Text();
    int time = 0;
    long timestamp;
    DecimalFormat df = new DecimalFormat("00");

    boolean goLeft, goRight;
    Image enemyImage;
    Image enemyHit;
    Image laser;
    Media laserSound;
    MediaPlayer laserPlayer;

    int enemyMove = 0;
    int delta;
    Random r;

    VBox vallignStart;
    /**
     * Combines a block of code the will create the elements of the start menu
     * Pre: gameStart = false
     * Post: new Scene with all required elements
     */
    private Scene startMenu() {
        vallignStart = new VBox();
        vallignStart.setStyle("-fx-background-color: BLACK");
        vallignStart.setAlignment(Pos.CENTER);

        Text menuTitle = new Text("SPACE INVADERS");
        menuTitle.setFont(Font.font("JetBrains Mono", 50));
        menuTitle.setFill(Color.GREEN);

        play = new Button("Play");
        play.setMinWidth(350);

        vallignStart.getChildren().addAll(menuTitle, play);
        return new Scene(vallignStart, WIDTH, HEIGHT, Color.BLACK);
    }

    /**
     * Combines a block of code that will remove all sprites and create elements of the end menu
     * Pre: gameEnd = true
     * Post: new elements will be created, which will form the end menu
     */
    private void endMenu() {
        VBox group = new VBox();
        group.setAlignment(Pos.CENTER);
        Text title = new Text("");
        Text playerTime = new Text((time%3600)/60 + ":" + df.format(time%60));
        playerTime.setFill(Color.WHITE);
        playerTime.setFont(Font.font("JetBrains Mono", 14));
        if (playerWon) {
            title.setText("YOU WON");
            title.setFont(Font.font("JetBrains Mono", 50));
            title.setFill(Color.GREEN);
        } else {
            title.setText("YOU LOST");
            title.setFont(Font.font("JetBrains Mono", 50));
            title.setFill(Color.RED);
        }
        group.getChildren().addAll(title, playerTime);
        group.relocate(WIDTH/2 - 150, HEIGHT/2 - 55);
        root.getChildren().add(group);
        root.getChildren().removeAll(sprites, livesText, gtime);
        timer.stop();

        String path = "./highscores.txt";
        File highscore = new File(path);
        Writer writer = null;
        PrintWriter output = null;
        if(highscore.exists()) {
            System.out.println("File already exists");
            try {
                writer = new FileWriter(path, true);
            } catch (IOException ignored) {}
            output = new PrintWriter(writer);
            output.print("\n" + (time%3600)/60 + ":" + df.format(time%60));
            output.close();
        } else {
            try {
                output = new PrintWriter(path);
                highscore.createNewFile();
                System.out.println("New file created");
                output = new PrintWriter(highscore);
            } catch (IOException ignored) { }
            output.print((time%3600)/60 + ":" + df.format(time%60));
            output.close();
        }

        Text pastScores = new Text("Past Scores");
        pastScores.setFill(Color.GRAY);
        pastScores.setFont(Font.font("JetBrains Mono", 25));
        group.getChildren().add(pastScores);
        int lines = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./highscores.txt"));
            while (reader.readLine() != null) lines++;
            for (int i=lines-2; i > lines-6; i--) {
                String s = Files.readAllLines(Paths.get("./highscores.txt")).get(i);
                Text score = new Text(s);
                score.setFill(Color.WHITE);
                score.setFont(Font.font("JetBrains Mono", 14));
                group.getChildren().add(score);
            }
            reader.close();
        } catch (IOException ignored) {}
    }

    /**
     * Code re-purposed from https://github.com/AlmasB/FXTutorials/blob/master/src/com/almasb/invaders/SpaceInvadersApp.java.
     * Creates a class that extends the javafx imageview package. This allows new objects created using this class to appear
     * as sprites on a screen, each with their own functions, variables and image.
     */
    private static class Sprite extends javafx.scene.image.ImageView {
        boolean dead = false;
        String type;

        Sprite(int x, int y, int l, int w, String type, Image image) {
            super(image);

            this.type = type;
            setTranslateX(x);
            setTranslateY(y);
        }

        void moveUp() {
            setTranslateY(getTranslateY() - 5);
        }
        void moveDown() {
            setTranslateY(getTranslateY() + 5);
        }
        void moveRight(double location) {
            if(!((int)location+32 == WIDTH)) setTranslateX(getTranslateX() + 4);
        }
        void moveLeft(double location) {
            if(!((int)location == 0)) setTranslateX(getTranslateX() - 4);
        }
    }

    /**
     * Code repurposed from https://github.com/AlmasB/FXTutorials/blob/master/src/com/almasb/invaders/SpaceInvadersApp.java.
     * Function that creates a list of all the sprites on in the group node "sprites". Needed for Animation Timer to be able to
     * move bullets and enemies because program needs to move each sprite individually using their object class function moveUp()/
     * moveDown().
     * Pre:
     * Post: list of sprites on sprites node exists
     */
    private List<Sprite> sprites() {
        return sprites.getChildren().stream().map(n -> (Sprite)n).collect(Collectors.toList());
    }
    /**
     * Takes the input of a sprite that is shooting. Based on that information, it takes the type of spirte, either enemy or player
     * and spawns a new spirte, with the image of a laser, by invoking the spirte class and adding the Image View to the Node.
     * Pre: s.type != null
     * Post:
     */
    private void shoot(Sprite s) {
        Sprite bullet;
        if(s.type.equals("player")) {
            bullet = new Sprite((int)s.getTranslateX(), (int)s.getTranslateY() - 20, 4, 20, s.type + "bullet", laser);
            sprites.getChildren().add(bullet);
        } else {
            bullet = new Sprite((int)s.getTranslateX(), (int)s.getTranslateY() + 30, 4, 20, s.type + "bullet", laser);
            sprites.getChildren().add(bullet);
        }
    }

    double enemyUrge;
    int enemyNum = 27;
    /**
     * A method that combines most of the code that is implemented in the AnimationTimer to keep the main start function
     * tidy and neat. Includes code that randomizes enemy shooting, moves bullets upwards every frame and detects
     * collsions.
     * Pre: AnimationTimer calls this function every frame
     * Post:
     */
    private void update() {
        enemyUrge += 0.01;

        if (enemyNum == 0) {
            playerWon = true;
            gameEnd = true;
            endMenu();
            return;
        }
        if (lives <= 0) {
            player.dead = true;
            playerWon = false;
            gameEnd = true;
            endMenu();
            return;
        }

        if (!gameEnd && gameStart) {
            if(System.currentTimeMillis() - timestamp > 1000) {
                time += 1;
                gtime.setText((time%3600)/60 + ":" + df.format(time%60));
                timestamp = System.currentTimeMillis();
            }
        }

        /*
         * Using the previous sprites() list function, the code loops through every element of the list. Uses a lambda
         * function to represent every element as "s".
         */
        sprites().forEach(s -> {
            //conditional statement that checks the sprites type and does different things based on s.type.
            switch (s.type) {
                //if s.type is "playerbullet", the program moves the sprite upwards
                case "playerbullet":
                    s.moveUp();
                    //taken from AlmasB on github. Gets all enemies in Node, and checks if playerbullet intersects any enemy.
                    sprites().stream().filter(e -> e.type.equals("enemy")).forEach(enemy -> {
                        if (s.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                            enemy.dead = true;
                            s.dead = true;
                            enemyNum -= 1;
                        }
                    });
                    break;
                //if s.type is "enemybullet", the program does the same as case playerbullet, but only if enemybullet hits player.
                case "enemybullet":
                    s.moveDown();
                    if (s.getBoundsInParent().intersects(player.getBoundsInParent())) {
                        s.dead = true;
                        lives -= 1;
                        livesText.setText("Lives: " + lives);
                        System.out.print(lives);
                    }
                    break;
                case "enemy":
                    /* when the enemy urge is above 2, this code produces a random integer. If that integer is below 7,
                     * it shoots a bullet at the location of that sprite.
                     */
                    if (enemyUrge > 2) {
                        if (r.nextInt(30) + 1 < 7) {
                            shoot(s);
                            enemyUrge = 1.8;
                        }
                    }
                    break;
            }
            /*
             * removeIf function expects boolean, so when a sprite is dead, the lambda function will return true, which will
             * remove the target sprite from the sprites Node. Also taken from AlmasB's project on github.
             */
            sprites.getChildren().removeIf(n -> {
                Sprite temp = (Sprite) n;
                return temp.dead;
            });

            // when the variable reaches above 2.01, it resets the variable back to 0.
            if (enemyUrge > 2.01) {
                enemyUrge = 0;
            }
        });
    }

    /**
     * Creates the 3 by 7 grid of enemies using loop structures
     * Pre: gameStart = true;
     * Post:
     */
    private void generateEnemies() {
        for(int i=0; i < 3; i++) {
            for(int x=0; x < 9; x++) {
                Sprite enemy = new Sprite(75+(x*100), 100+i*100, 50, 50, "enemy", enemyImage);
                sprites.getChildren().add(enemy);
            }
        }
    }
    /**
     * Moves enemy left by 1 or right by 1 depending on the value of global variable delta
     * Pre: delta = 1 or delta = -1
     * Post: enemy has been moved
     */
    private void moveEnemy() {
        if (delta == 1) {
            // uses sprites() function to select all enemy sprites and move all of them to the right
            sprites().forEach(s -> {
                if (s.type.equals("enemy")) {
                    s.setTranslateX(s.getTranslateX() + 1);
                }
            });
        } else if (delta == -1) {
            // uses sprites() function to select all enemy sprites and move all of them to the left
            sprites().forEach(s -> {
                if (s.type.equals("enemy")) {
                    s.setTranslateX(s.getTranslateX() - 1);
                }
            });
        }
    }

    Scene scene;
    @Override
    public void start(Stage stage) throws Exception {
        root = new Group();
        scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        sprites = new Group();
        root.getChildren().add(sprites);
        sprites.minHeight(WIDTH);
        sprites.minWidth(HEIGHT);

        laser = new Image(new FileInputStream("src/spaceinvaders/laser.png"));
        laserSound = new Media(new File("src/spaceinvaders/shoot.wav").toURI().toString());
        laserPlayer = new MediaPlayer(laserSound);
        enemyImage = new Image(new FileInputStream("src/spaceinvaders/alien.png"));
        enemyHit = new Image(new FileInputStream("src/spaceinvaders/alienhit.png"));
        Image playerImage = new Image(new FileInputStream("src/spaceinvaders/playersprite.png"));
        player = new Sprite(WIDTH/2 - 16, 900, 32, 32, "player", playerImage);
        sprites.getChildren().add(player);
        player.dead = false;

        lives = 3;
        livesText.setText("Lives: " + lives);
        livesText.relocate(10, 10);
        root.getChildren().add(livesText);
        livesText.setFont(Font.font("JetBrains Mono", 14));
        livesText.setFill(Color.WHITE);

        gtime.relocate(950, 10);
        gtime.setFill(Color.WHITE);
        gtime.setFont(Font.font("JetBrains Mono", 14));
        root.getChildren().add(gtime);
        timestamp = System.currentTimeMillis();

        Rectangle ground = new Rectangle(1000, 5);
        Rectangle invisGround = new Rectangle(1000, 73);
        ground.setFill(Color.DARKGREEN);
        ground.relocate(0, 924);
        invisGround.setFill(Color.GREEN);
        invisGround.relocate(0, 929);
        root.getChildren().addAll(ground, invisGround);

        generateEnemies();
        r = new Random();

        scene.setOnKeyPressed(e -> {
            if (!player.dead) {
                switch (e.getCode()) {
                    case LEFT:
                        goLeft = true;
                        break;
                    case RIGHT:
                        goRight = true;
                        break;
                    case SPACE:
                        // when firedelay is above 40, player is able to shoot a bullet
                        if(fireDelay > 40) {
                            laserPlayer.stop(); //stops laser sound
                            shoot(player);
                            laserPlayer.play(); //plays laser sound
                            fireDelay = 0; // resets fire delay
                        }
                }
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT: goLeft = false; break;
                case RIGHT: goRight = false; break;
            }
        });

        //initalizes all important variables
        delta = -1;
        enemyUrge = 0;
        fireDelay = 0;

        //creates a javafx element which executes blocks of code every frame
        timer = new AnimationTimer() {
            public void handle(long now) {
                if(goLeft) player.moveLeft(player.getTranslateX());
                if(goRight) player.moveRight(player.getTranslateX());

                /**
                 * adds the variable delta to the variable enemyMove. Everytime enemyMove becomes
                 * greater or less than 50 or -50, it switches signs (+ to - or - to +). After each
                 * "check" the moveEnemy function is called, which utilizes the value of delta.
                 */
                enemyMove += delta;
                if(enemyMove >= 50) {
                    delta = -1;
                } else if (enemyMove <= -50) {
                    delta = 1;
                }
                moveEnemy();

                // increases fire delay by 1 every frame
                fireDelay += 1;

                update();
            }
        };
        timer.start(); //starts the AnimationTimer

        stage.setScene(startMenu()); //initializes and sets scene to start menu
        stage.show();

        // when start menu play button is pressed it switches scene to main game and sets gameStart to true
        play.setOnAction(e -> {
            stage.setScene(scene);
            gameStart = true;
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}