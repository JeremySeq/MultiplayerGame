package main.java.com.jeremyseq.multiplayer_game.level_editor;

import main.java.com.jeremyseq.multiplayer_game.client.Game;
import main.java.com.jeremyseq.multiplayer_game.common.level.*;
import main.java.com.jeremyseq.multiplayer_game.common.Vec2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LevelEditor extends JPanel implements ActionListener, KeyListener {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 900;

    public final int DELAY = 20;
    public HashMap<String, BufferedImage> tilemaps = new HashMap<>();
    public HashMap<BuildingType, BufferedImage> buildings = new HashMap<>();
    public HashMap<BuildingType, BufferedImage> contruction_buildings = new HashMap<>();
    public HashMap<BuildingType, BufferedImage> destroyed_buildings = new HashMap<>();

    int drawSize = 64;
    int tileSize = 64;
    public String tilemap = "flat";
    public int tilemapI = 0;
    public int tilemapJ = 0;

    private int frameCounter = 0; // counts frames
    private int animationFrame = 0; // frame that the animations are on

    public LevelEditorMouseHandler mouseHandler = new LevelEditorMouseHandler(this);

    public Vec2 camPos = new Vec2(0, 0);

    public Level level = new LevelReader().readLevel("level1");
    public String layer = String.valueOf(this.level.metadata.layers);

    private Timer timer;
    private boolean dPressed = false;
    private boolean moveUp = false;
    private boolean moveDown = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;

    public LevelEditor() {
        this.loadImages();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // this timer will call the actionPerformed() method every DELAY ms
        timer = new Timer(DELAY, this);
        timer.start();

        Thread receiveInput = new Thread(() -> {
            while (true) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));
                // Reading data using readLine
                try {
                    String tileChange = reader.readLine();
                    tilemap = tileChange.split(":")[0];
                    String tilePos = tileChange.split(":")[1];
                    tilemapI = Integer.parseInt(tilePos.split(",")[0]);
                    tilemapJ = Integer.parseInt(tilePos.split(",")[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        receiveInput.start();

        this.addMouseListener(mouseHandler);
        this.addMouseWheelListener(mouseHandler);

        this.addKeyListener(this);
        this.setFocusable(true);
    }

    public void mousePressed(MouseEvent e) {
        Vec2 mousePos = new Vec2(getMousePosition().x, getMousePosition().y);
        for (int i = 0; i < Game.WIDTH/drawSize + drawSize; i++) {
            for (int j = 0; j < Game.HEIGHT/drawSize + drawSize; j++) {
                if (mousePos.x > i*drawSize && mousePos.x < i*drawSize + drawSize && mousePos.y > j*drawSize && mousePos.y < j*drawSize + drawSize) {
                    Vec2 tilePos = new Vec2(i, j).subtract(new Vec2(7, 7).subtract(camPos)); // I don't know why its 7 it just is
                    if (dPressed) {
                        System.out.println("Deleting");
                        level.tiles.get(layer).removeIf(tile -> tile.x == (int) tilePos.x && tile.y == (int) tilePos.y);
                    } else {
                        level.tiles.computeIfAbsent(layer, k -> new ArrayList<>());
                        level.tiles.get(layer).add(new Tile((int) tilePos.x, (int) tilePos.y, tilemap, tilemapI, tilemapJ));
                    }
                }
            }
        }
    }

    public void mouseWheelUp() {
        if (layer.equals(String.valueOf(level.metadata.layers))) {
            return;
        }
        if (layer.contains("-")) {
            layer = layer.split("-")[1];
        } else {
            layer = layer + "-" + (Integer.parseInt(layer) + 1);
        }
        System.out.println(layer);
    }

    public void mouseWheelDown() {
        if (layer.equals("1")) {
            return;
        }
        if (layer.contains("-")) {
            layer = layer.split("-")[0];
        } else {
            layer = (Integer.parseInt(layer) - 1) + "-" + layer;
        }
        System.out.println(layer);
    }

    public void loadImages() {
        try {
            tilemaps.put("flat", ImageIO.read(Objects.requireNonNull(getClass().getResource("/TinySwordsPack/Terrain/Ground/Tilemap_Flat.png"))));
            tilemaps.put("elevation", ImageIO.read(Objects.requireNonNull(getClass().getResource("/TinySwordsPack/Terrain/Ground/Tilemap_Elevation.png"))));
            tilemaps.put("water", ImageIO.read(Objects.requireNonNull(getClass().getResource("/TinySwordsPack/Terrain/Water/Water.png"))));
            tilemaps.put("foam", ImageIO.read(Objects.requireNonNull(getClass().getResource("/TinySwordsPack/Terrain/Water/Foam/Foam.png"))));

            for (BuildingType buildingType : BuildingType.values()) {
                buildings.put(buildingType, ImageIO.read(Objects.requireNonNull(getClass().getResource(buildingType.imageFileName))));
            }
            for (BuildingType buildingType : BuildingType.values()) {
                contruction_buildings.put(buildingType, ImageIO.read(Objects.requireNonNull(getClass().getResource(buildingType.constructionImageFileName))));
            }
            for (BuildingType buildingType : BuildingType.values()) {
                destroyed_buildings.put(buildingType, ImageIO.read(Objects.requireNonNull(getClass().getResource(buildingType.destroyedImageFileName))));
            }

        } catch (IOException exc) {
            System.out.println("Error opening image file: " + exc.getMessage());
        }
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        // when calling g.drawImage() we can use "this" for the ImageObserver
        // because Component implements the ImageObserver interface, and JPanel
        // extends from Component. So "this" main.java.com.seq.chess.Board instance, as a Component, can
        // react to imageUpdate() events triggered by g.drawImage()

        // draw our graphics.
        drawBackground(g);


        // this smooths out animations on some systems
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawBackground(Graphics g) {
        draw(g, this);
    }

    public void draw(Graphics g, ImageObserver imageObserver) {
        frameCounter++;
        if (frameCounter >= 3) {
            animationFrame++;
            frameCounter = 0;
            if (animationFrame >= 8) {
                animationFrame = 0;
            }
        }


        Vec2 mousePos = null;
        if (this.getMousePosition() != null) {
            mousePos = new Vec2(this.getMousePosition().x, this.getMousePosition().y);
        }

        // draw water background
        for (int i = 0; i < Game.WIDTH/drawSize + 1; i++) {
            for (int j = 0; j < Game.HEIGHT/drawSize + 1; j++) {
                drawTile(g, imageObserver, i*drawSize, j*drawSize, tilemaps.get("water"), 0, 0, true);
            }
        }

        int numberOfLayers = level.metadata.layers;
        for (int i = 1; i <= numberOfLayers*2 - 1; i++) {
            String l;
            if (i % 2 == 1) {
                l = String.valueOf(i - (i - 1) / 2);
            } else {
                String prev = String.valueOf((i - 1) - (i - 2) / 2);
                String next = String.valueOf((i + 1) - (i) / 2);
                l = prev + "-" + next;
            }
            if (level.tiles.get(l) == null) {
                continue;
            }
            // draw foam around outline of layer 1
            if (i == 1) {
                ArrayList<Tile> outlineLayerTiles = level.getOuterTilesInLayer(l);
                for (Tile tile : outlineLayerTiles) {
                    drawTile(g, imageObserver, (tile.x)*drawSize, (tile.y)*drawSize, tilemaps.get("foam"), 1+3*animationFrame, 1);

                    drawTile(g, imageObserver, (tile.x)*drawSize, (tile.y-1)*drawSize, tilemaps.get("foam"), 1+3*animationFrame, 0);
                    drawTile(g, imageObserver, (tile.x+1)*drawSize, (tile.y)*drawSize, tilemaps.get("foam"), 2+3*animationFrame, 1);
                    drawTile(g, imageObserver, (tile.x)*drawSize, (tile.y+1)*drawSize, tilemaps.get("foam"), 1+3*animationFrame, 2);
                    drawTile(g, imageObserver, (tile.x-1)*drawSize, (tile.y)*drawSize, tilemaps.get("foam"), 3*animationFrame, 1);
                }
            }

            for (Tile tile : level.tiles.get(l)) {
                drawTile(g, imageObserver, tile.x * drawSize, tile.y * drawSize, tilemaps.get(tile.tilemap), tile.i, tile.j);
            }

            ArrayList<Building> buildingList = level.buildings.get(l);
            if (buildingList != null && !buildingList.isEmpty()) {
                for (Building building : buildingList) {
                    drawBuilding(g, imageObserver, building);
                }
            }

            if (l.equals(layer)) {
                break;
            }
        }

        for (int i = 0; i < Game.WIDTH/drawSize + drawSize; i++) {
            for (int j = 0; j < Game.HEIGHT/drawSize + drawSize; j++) {
                if (mousePos != null) {
                    if (mousePos.x > i*drawSize && mousePos.x < i*drawSize + drawSize && mousePos.y > j*drawSize && mousePos.y < j*drawSize + drawSize) {
                        g.drawRect(i*drawSize+1, j*drawSize+1, drawSize, drawSize);
                        Vec2 tilePos = new Vec2(i, j).subtract(new Vec2(7, 7).subtract(camPos)); // I don't know why its 7 it just is
                        g.setFont(new Font("Jetbrains Mono", Font.BOLD, 22));
                        g.drawString("Pos: " + (int) tilePos.x + ", " + (int) tilePos.y, 5, HEIGHT-20);
                    }
                }
            }
        }

        drawTile(g, imageObserver, 0, 0, tilemaps.get(this.tilemap), tilemapI, tilemapJ, true);
        g.setFont(new Font("Jetbrains Mono", Font.BOLD, 22));
        Rectangle2D bounds = g.getFont().getStringBounds(layer, g.getFontMetrics().getFontRenderContext());
        g.drawString("Layer: " + layer, drawSize + 10, (int) (bounds.getHeight()+2));
    }

    public void drawBuilding(Graphics g, ImageObserver imageObserver, Building building) {
        Vec2 renderPos = new Vec2(building.x*drawSize, building.y*drawSize);
        renderPos = this.getRenderPositionFromWorldPosition(renderPos);
        int x2 = (int) renderPos.x;
        int y2 = (int) renderPos.y;
        BufferedImage image;
        if (building.state == BuildingState.BUILT) {
            image = buildings.get(building.type);
        } else if (building.state == BuildingState.CONSTRUCTION) {
            image = contruction_buildings.get(building.type);
        } else {
            image = destroyed_buildings.get(building.type);
        }
        g.drawImage(
                image,
                x2, y2, drawSize*building.type.tileWidth, drawSize*building.type.tileHeight,
                imageObserver
        );
    }

    /**
     * @param x x-coordinate to draw on screen
     * @param y y-coordinate to draw on screen
     * @param tilemap tilemap buffered image
     * @param i tile on tilemap to draw
     * @param j tile on tilemap to draw
     */
    public void drawTile(Graphics g, ImageObserver imageObserver, int x, int y, BufferedImage tilemap, int i, int j) {
        drawTile(g, imageObserver, x, y, tilemap, i, j, false);
    }

    /**
     * @param x x-coordinate to draw on screen
     * @param y y-coordinate to draw on screen
     * @param tilemap tilemap buffered image
     * @param i tile on tilemap to draw
     * @param j tile on tilemap to draw
     * @param ignoreScrolling if this is true, renders the tile without converting to world position, meaning
     *                        if the player moves, this tile stays in the same spot on their screen
     */
    public void drawTile(Graphics g, ImageObserver imageObserver, int x, int y, BufferedImage tilemap, int i, int j, boolean ignoreScrolling) {
        Vec2 renderPos = new Vec2(x, y);
        if (!ignoreScrolling) {
            renderPos = getRenderPositionFromWorldPosition(renderPos);
        }
        int x2 = (int) renderPos.x;
        int y2 = (int) renderPos.y;
        g.drawImage(
                tilemap,
                x2, y2, x2 + drawSize, y2 + drawSize,
                tileSize*i, tileSize*j, tileSize*i + tileSize, tileSize*j + tileSize,
                imageObserver
        );
    }

    public Vec2 getRenderPositionFromWorldPosition(Vec2 vec2) {
        vec2 = vec2.add(new Vec2(WIDTH/2f, HEIGHT/2f));
        vec2 = vec2.subtract(camPos.multiply(drawSize));
        return vec2;
    }

    public Vec2 getWorldPositionFromRenderPosition(Vec2 vec2) {
        vec2 = vec2.add(camPos.multiply(drawSize));
        vec2 = vec2.subtract(new Vec2(WIDTH/2f, HEIGHT/2f));
        return vec2;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (moveUp) {
            camPos = camPos.add(new Vec2(0, -1));
        } else if (moveDown) {
            camPos = camPos.add(new Vec2(0, 1));
        } else if (moveLeft) {
            camPos = camPos.add(new Vec2(-1, 0));
        } else if (moveRight) {
            camPos = camPos.add(new Vec2(1, 0));
        }

        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

        if (e.getKeyChar() == 't') {
            System.out.println("Saving");
            try {
                FileWriter writer = new FileWriter("src/main/resources/levels/level1.json");
                writer.write(level.toJson());
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) {
            this.moveUp = true;
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            this.moveDown = true;
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            this.moveLeft = true;
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            this.moveRight = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            this.dPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            tilemapJ -= 1;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            tilemapJ += 1;
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            tilemapI -= 1;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            tilemapI += 1;
        }

        if (e.getKeyCode() == KeyEvent.VK_1) {
            tilemap = "flat";
        }
        if (e.getKeyCode() == KeyEvent.VK_2) {
            tilemap = "elevation";
        }
        System.out.println(tilemapI + ", " + tilemapJ);
    }

    @Override
    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_W) {
            this.moveUp = false;
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            this.moveDown = false;
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            this.moveLeft = false;
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            this.moveRight = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_R) {
            this.dPressed = false;
        }

    }
}
