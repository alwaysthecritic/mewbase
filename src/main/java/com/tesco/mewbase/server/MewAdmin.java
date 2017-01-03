package com.tesco.mewbase.server;

import com.tesco.mewbase.projection.Projection;
import com.tesco.mewbase.projection.ProjectionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by tim on 15/12/16.
 */
public interface MewAdmin {

    ProjectionBuilder buildProjection(String name);

    CompletableFuture<Boolean> createChannel(String channelName);

    CompletableFuture<Boolean> createBinder(String binderName);

    List<String> listProjections();

    Projection getProjection(String projectionName);

    List<String> listChannels();

    List<String> listBinders();

}
