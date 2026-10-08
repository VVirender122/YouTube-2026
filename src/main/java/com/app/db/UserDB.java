package com.app.db;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import shadow.org.bson.Document;

public final class UserDB {

    private static final MongoCollection<Document> USERS =
            YouTubeDataBase.getDatabase().getCollection("users");

    static {
        try {
            USERS.createIndex(
                    new Document("email", 1),
                    new IndexOptions().unique(true)
            );
        } catch (Exception ignored) {
            // Existing indexes are fine; runtime operations will still work.
        }
    }

    private UserDB() {
    }

    public static boolean addUser(UserSchema user) {
        try {
            USERS.insertOne(new Document()
                    .append("firstName", user.getFirstName())
                    .append("lastName", user.getLastName())
                    .append("email", user.getEmail())
                    .append("passwordHash", user.getPasswordHash()));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Document getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try {
            return USERS.find(
                    eq("email", email.trim().toLowerCase())
            ).first();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean updatePassword(String email, String passwordHash) {
        if (email == null || passwordHash == null) {
            return false;
        }

        try {
            var result = USERS.updateOne(
                    eq("email", email.trim().toLowerCase()),
                    new Document("$set",
                            new Document("passwordHash", passwordHash))
            );

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean migrateLegacyPassword(
            String email,
            String passwordHash) {

        if (email == null || passwordHash == null) {
            return false;
        }

        try {
            var result = USERS.updateOne(
                    eq("email", email.trim().toLowerCase()),
                    new Document("$set",
                            new Document("passwordHash", passwordHash))
                            .append("$unset",
                                    new Document("password", ""))
            );

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stores the password-reset token hash and its expiration time.
     */
    public static boolean saveResetToken(
            String email,
            String tokenHash,
            long expiryMillis) {

        if (email == null || tokenHash == null || tokenHash.isBlank()) {
            return false;
        }

        try {
            var result = USERS.updateOne(
                    eq("email", email.trim().toLowerCase()),
                    new Document("$set",
                            new Document("resetTokenHash", tokenHash)
                                    .append("resetTokenExpiry", expiryMillis))
            );

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Atomically validates the reset token and changes the password.
     *
     * The token is removed during the same database operation, making
     * successful reset links one-time-use.
     */
    public static boolean resetPasswordWithToken(
            String tokenHash,
            String passwordHash,
            long currentTimeMillis) {

        if (tokenHash == null || tokenHash.isBlank()
                || passwordHash == null) {
            return false;
        }

        try {
            var filter = new Document("resetTokenHash", tokenHash)
                    .append("resetTokenExpiry",
                            new Document("$gte", currentTimeMillis));

            var update = new Document("$set",
                    new Document("passwordHash", passwordHash))
                    .append("$unset",
                            new Document("resetTokenHash", "")
                                    .append("resetTokenExpiry", ""));

            var result = USERS.updateOne(filter, update);

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes an outstanding reset token.
     */
    public static boolean clearResetToken(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        try {
            var result = USERS.updateOne(
                    eq("email", email.trim().toLowerCase()),
                    new Document("$unset",
                            new Document("resetTokenHash", "")
                                    .append("resetTokenExpiry", ""))
            );

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}