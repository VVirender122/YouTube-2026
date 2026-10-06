package com.app.db;

import java.util.Locale;

import com.app.config.SecretsReader;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class YouTubeDataBase {

    // Connection string (with credentials) comes from secrets, never from code
    private static final String CONNECTION_STRING =
            SecretsReader.readData("secrets", new Locale("en", "US"), "MONGO_URI");
    private static final MongoClient mongoClient;
    private static final MongoDatabase database;

    static {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(
                        new ConnectionString(CONNECTION_STRING)
                )
                .serverApi(serverApi)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("YouTube-DB");
    }

    public static MongoDatabase getDatabase() {
        return database;
    }
}
