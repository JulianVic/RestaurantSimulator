package com.simulador.app;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.simulador.config.Config;
import com.simulador.infraestructura.entidades.*;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public class EntidadIlustrador implements EntityFactory {

    @Spawns("customer")
    public Entity spawnCustomer(SpawnData data) {
        List<String> imagePaths = List.of(
                "texturas/pinguinos/comensal1.png",
                "texturas/pinguinos/comensal2.png",
                "texturas/pinguinos/comensal3.png"
        );

        Random rand = new Random();
        int randomIndex = rand.nextInt(imagePaths.size());
        ImageView imageView = new ImageView(new Image(imagePaths.get(randomIndex)));
        imageView.setFitWidth(Config.SPRITE_SIZE * 2);
        imageView.setFitHeight(Config.SPRITE_SIZE * 2);

        return entityBuilder()
                .at(data.getX(), data.getY())
                .viewWithBBox(imageView)
                .with(data.<Customer>get("customerComponent"))
                .build();
    }


    @Spawns("waiter")
    public Entity spawnWaiter(SpawnData data) {
        ImageView imageView = new ImageView(new Image("/texturas/pinguinos/Mesera.png"));
        imageView.setFitWidth(Config.SPRITE_SIZE * 2);
        imageView.setFitHeight(Config.SPRITE_SIZE * 2);

        return entityBuilder()
                .at(data.getX(), data.getY())
                .viewWithBBox(imageView)
                .with(data.<Waiter>get("waiterComponent"))
                .build();
    }

    @Spawns("cook")
    public Entity spawnCook(SpawnData data) {
        List<String> imagePaths = List.of(
                "texturas/pinguinos/cocinera1.png",
                "texturas/pinguinos/cocinera2.png"

        );

        Random rand = new Random();
        int randomIndex = rand.nextInt(imagePaths.size());
        ImageView imageView = new ImageView(new Image(imagePaths.get(randomIndex)));
        imageView.setFitWidth(Config.SPRITE_SIZE * 2);
        imageView.setFitHeight(Config.SPRITE_SIZE * 2);

        return entityBuilder()
                .at(data.getX(), data.getY())
                .viewWithBBox(imageView)
                .build();
    }


    @Spawns("table")
    public Entity spawnTable(SpawnData data) {
        ImageView imageView = new ImageView(new Image("/texturas/objetos/mesa.png"));
        imageView.setFitWidth(Config.SPRITE_SIZE * 2);
        imageView.setFitHeight(Config.SPRITE_SIZE * 2);

        return entityBuilder()
                .at(data.getX(), data.getY())
                .viewWithBBox(imageView)
                .with(new Table(data.get("tableNumber"), new Point2D(data.getX(), data.getY())))
                .build();
    }

    @Spawns("receptionist")
    public Entity spawnReceptionist(SpawnData data) {
        ImageView imageView = new ImageView(new Image("/texturas/pinguinos/Recepcionista.png"));
        imageView.setFitWidth(Config.SPRITE_SIZE * 3);
        imageView.setFitHeight(Config.SPRITE_SIZE * 4);

        return entityBuilder()
                .at(Config.RECEPTIONIST_X, Config.RECEPTIONIST_Y)
                .viewWithBBox(imageView)
                .with(data.<Receptionist>get("receptionistComponent"))
                .build();
    }

}


