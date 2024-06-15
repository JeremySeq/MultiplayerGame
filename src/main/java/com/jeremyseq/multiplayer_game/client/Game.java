package main.java.com.jeremyseq.multiplayer_game.client;

import main.java.com.jeremyseq.multiplayer_game.Client;
import main.java.com.jeremyseq.multiplayer_game.common.Vec2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

public class Game extends JPanel implements ActionListener {

    public static final int WIDTH = 640;
    public static final int HEIGHT = 900;
    private static final float SPEED = 5;

    public final int DELAY = 20;
    public final Client client;

    public ClientPlayer clientPlayer;

    public ArrayList<ClientPlayer> players = new ArrayList<>();

    public KeyHandler keyHandler = new KeyHandler();
    public MouseHandler mouseHandler = new MouseHandler(this);

    private Timer timer;

    public Game(Client client) {
        this.client = client;

        this.clientPlayer = new ClientPlayer(this, client.username, new Vec2(0, 0));
        this.players.add(clientPlayer);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        Thread receiveServerResponses = new Thread(() -> {
            while (true) {
                try {
                    ClientInterpretPacket.interpretPacket(this, client.server_response.readUTF());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        receiveServerResponses.start();

        // this timer will call the actionPerformed() method every DELAY ms
        timer = new Timer(DELAY, this);
        timer.start();

        this.addMouseListener(mouseHandler);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);
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

        try {
            client.out.writeUTF("$pos:" + clientPlayer.position.toPacketString());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // this smooths out animations on some systems
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawBackground(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (ClientPlayer player : this.players) {
            player.draw(g, this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // this method is called by the timer every DELAY ms.
        // use this space to update the state of your game or animation
        // before the graphics are redrawn.

        Vec2 dir = new Vec2(0, 0);
        if (keyHandler.leftPressed) {
            dir = dir.add(new Vec2(-1, 0));
        }
        if (keyHandler.rightPressed) {
            dir = dir.add(new Vec2(1, 0));
        }
        if (keyHandler.upPressed) {
            dir = dir.add(new Vec2(0, -1));
        }
        if (keyHandler.downPressed) {
            dir = dir.add(new Vec2(0, 1));
        }

        if (dir.x != 0 || dir.y != 0) {
            this.clientPlayer.position = this.clientPlayer.position.add(dir.normalize().multiply(SPEED));
        }
        try {
            client.out.writeUTF("$movement:" + dir.normalize().multiply(SPEED).toPacketString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // calling repaint() will trigger paintComponent() to run again,
        // which will refresh/redraw the graphics.
        repaint();
    }

    public ClientPlayer getClientPlayerByUsername(String username) {
        ClientPlayer respectivePlayer = null;
        for (ClientPlayer player : players) {
            if (player.username.equals(username)) {
                respectivePlayer = player;
            }
        }
        return respectivePlayer;
    }
}
