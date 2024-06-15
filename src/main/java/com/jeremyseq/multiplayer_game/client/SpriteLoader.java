package main.java.com.jeremyseq.multiplayer_game.client;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

public class SpriteLoader {
    private final File imageFile;
    private BufferedImage image;
    private int frameCounter = 0;
    private int animationFrame = 0;

    private final int tileWidth;
    private final int tileHeight;
    private final int drawSize;

    private final int framesPerAnimation;
    private int animationCount;

    private int playingAnimation;

    public SpriteLoader(File imageFile, int animationCount, int framesPerAnimation, int tileSize, int drawSize) {
        this.imageFile = imageFile;
        this.framesPerAnimation = framesPerAnimation;
        this.animationCount = animationCount;
        this.tileHeight = tileSize;
        this.tileWidth = tileSize;
        this.drawSize = drawSize;
    }

    public void loadImage() {
        if (image == null) {
            try {
                image = ImageIO.read(this.imageFile);
            } catch (IOException exc) {
                System.out.println("Error opening image file: " + exc.getMessage());
            }
        }
    }

    public void drawAnimation(Graphics g, ImageObserver imageObserver, int animation, int x, int y) {
        loadImage();

        if (animation != playingAnimation) {
            playingAnimation = animation;
            animationFrame = 0;
            frameCounter = 0;
        }

        frameCounter++;
        if (frameCounter >= 3) {
            animationFrame++;
            frameCounter = 0;
            if (animationFrame >= framesPerAnimation) {
                animationFrame = 0;
            }
        }

        g.drawImage(
                this.image,
                x - drawSize/2, y - drawSize/2, x + drawSize/2, y + drawSize/2,
                animationFrame*tileWidth, animation*tileHeight, animationFrame*tileWidth+tileWidth, animation*tileHeight+tileHeight,
                imageObserver
        );
    }
}
