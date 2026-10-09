package com.app.helpers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.config.SecretsReader;
import com.app.db.VideoSchema;

public final class YouTubeDataApi {

    private static final Logger LOGGER = Logger.getLogger(YouTubeDataApi.class.getName());
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final String BASE = "https://www.googleapis.com/youtube/v3/";
    private static final Locale LOCALE = Locale.US;

    private YouTubeDataApi() {
    }

    /**
     * A safe, user-displayable API failure. Detailed diagnostics are logged server-side.
     */
    public static final class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private ApiException(String message) {
            super(message);
        }

        private ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static List<VideoSchema> getPopularVideos(int maxResults) {
        String url = BASE + "videos?part=snippet,contentDetails,statistics"
                + "&chart=mostPopular&regionCode=US&maxResults=" + clamp(maxResults)
                + "&key=" + apiKey();
        return fetchVideoDetails(url);
    }

    public static List<VideoSchema> searchVideos(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return getPopularVideos(maxResults);
        }

        String url = BASE + "search?part=snippet&type=video"
                + "&maxResults=" + clamp(maxResults)
                + "&q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                + "&key=" + apiKey();

        final JSONObject root;
        try {
            root = getJson(url);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to parse YouTube search response.", e);
            throw new ApiException("YouTube returned an invalid search response.", e);
        }

        JSONArray items = root.optJSONArray("items");
        if (items == null) {
            LOGGER.warning("YouTube search response did not contain an items array.");
            throw new ApiException("YouTube returned an unexpected search response.");
        }

        List<VideoSchema> results = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            JSONObject id = item == null ? null : item.optJSONObject("id");
            JSONObject snippet = item == null ? null : item.optJSONObject("snippet");
            if (id == null || snippet == null) {
                continue;
            }

            String videoId = id.optString("videoId", "");
            if (videoId.isBlank()) {
                continue;
            }

            results.add(new VideoSchema(
                    snippet.optString("title", "Untitled"),
                    videoId,
                    thumbnail(snippet),
                    "",
                    "",
                    snippet.optString("channelTitle", "Unknown channel")));
        }

        // Statistics are optional; retain the usable search results if enrichment fails.
        return results.isEmpty() ? results : enrich(results);
    }

    private static List<VideoSchema> fetchVideoDetails(String url) {
        JSONObject root = getJson(url);
        JSONArray items = root.optJSONArray("items");
        if (items == null) {
            LOGGER.warning("YouTube videos response did not contain an items array.");
            throw new ApiException("YouTube returned an unexpected video response.");
        }

        List<VideoSchema> results = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }

            JSONObject snippet = item.optJSONObject("snippet");
            JSONObject details = item.optJSONObject("contentDetails");
            JSONObject stats = item.optJSONObject("statistics");
            if (snippet == null) {
                continue;
            }

            results.add(toSchema(item.optString("id", ""), snippet, details, stats));
        }
        return results;
    }

    private static List<VideoSchema> enrich(List<VideoSchema> videos) {
        String ids = videos.stream()
                .map(VideoSchema::getVideoID)
                .filter(id -> !id.isBlank())
                .map(id -> URLEncoder.encode(id, StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "%2C" + b)
                .orElse("");

        if (ids.isBlank()) {
            return videos;
        }

        String url = BASE + "videos?part=contentDetails,statistics"
                + "&id=" + ids + "&key=" + apiKey();

        try {
            JSONObject root = getJson(url);
            JSONArray items = root.optJSONArray("items");
            if (items == null) {
                LOGGER.warning("YouTube enrichment response did not contain an items array; using search results.");
                return videos;
            }

            Map<String, JSONObject> byId = new HashMap<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) {
                    byId.put(item.optString("id"), item);
                }
            }

            List<VideoSchema> enriched = new ArrayList<>();
            for (VideoSchema video : videos) {
                JSONObject item = byId.get(video.getVideoID());
                if (item == null) {
                    enriched.add(video);
                    continue;
                }

                JSONObject details = item.optJSONObject("contentDetails");
                JSONObject stats = item.optJSONObject("statistics");
                enriched.add(new VideoSchema(
                        video.getVideoName(),
                        video.getVideoID(),
                        video.getThumbnail(),
                        formatDuration(details == null ? "" : details.optString("duration", "")),
                        formatNumber(stats == null ? "" : stats.optString("viewCount", "")),
                        video.getChannelName()));
            }
            return enriched;
        } catch (ApiException e) {
            LOGGER.log(Level.WARNING, "YouTube search-result enrichment failed; returning videos without statistics.", e);
            return videos;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unexpected error enriching YouTube search results; returning original results.", e);
            return videos;
        }
    }

    private static VideoSchema toSchema(String id, JSONObject snippet,
                                        JSONObject details, JSONObject stats) {
        return new VideoSchema(
                snippet.optString("title", "Untitled"),
                id,
                thumbnail(snippet),
                formatDuration(details == null ? "" : details.optString("duration", "")),
                formatNumber(stats == null ? "" : stats.optString("viewCount", "")),
                snippet.optString("channelTitle", "Unknown channel"));
    }

    private static String thumbnail(JSONObject snippet) {
        JSONObject thumbnails = snippet.optJSONObject("thumbnails");
        if (thumbnails == null) return "";
        JSONObject selected = thumbnails.optJSONObject("high");
        if (selected == null) selected = thumbnails.optJSONObject("medium");
        if (selected == null) selected = thumbnails.optJSONObject("default");
        return selected == null ? "" : selected.optString("url", "");
    }

    private static String formatDuration(String iso) {
        if (iso == null || iso.isBlank()) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
                .matcher(iso);
        if (!m.matches()) return iso;

        long h = parse(m.group(1));
        long min = parse(m.group(2));
        long sec = parse(m.group(3));

        if (h > 0) return h + ":" + String.format("%02d:%02d", min, sec);
        return min + ":" + String.format("%02d", sec);
    }

    private static long parse(String value) {
        return value == null || value.isBlank() ? 0 : Long.parseLong(value);
    }

    private static String formatNumber(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            long n = Long.parseLong(value);
            if (n >= 1_000_000_000L) return String.format(Locale.US, "%.1fB", n / 1_000_000_000d);
            if (n >= 1_000_000L) return String.format(Locale.US, "%.1fM", n / 1_000_000d);
            if (n >= 1_000L) return String.format(Locale.US, "%.1fK", n / 1_000d);
            return Long.toString(n);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static JSONObject getJson(String url) {
        final HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "YouTube API request was interrupted.", e);
            throw new ApiException("The YouTube request was interrupted.", e);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Unable to complete YouTube API request.", e);
            throw new ApiException("Unable to connect to YouTube. Please try again.", e);
        }

        if (response.statusCode() != 200) {
            // Do not log the request URL because it contains the API key.
            LOGGER.warning("YouTube API returned HTTP status " + response.statusCode() + ".");
            throw new ApiException(messageForStatus(response.statusCode()));
        }

        try {
            return new JSONObject(response.body());
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "YouTube API returned invalid JSON.", e);
            throw new ApiException("YouTube returned an invalid response. Please try again.", e);
        }
    }

    private static String messageForStatus(int status) {
        if (status == 400) return "YouTube rejected the request. Please check your search and try again.";
        if (status == 401 || status == 403) return "YouTube is temporarily unavailable for this application. Please try again later.";
        if (status == 429) return "Too many requests were made. Please wait a moment and try again.";
        if (status >= 500) return "YouTube is temporarily unavailable. Please try again later.";
        return "Unable to load videos from YouTube right now. Please try again.";
    }

    private static String apiKey() {
        try {
            return SecretsReader.readData("secrets", LOCALE, "YOUTUBE_API_KEY");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "YouTube API key configuration is unavailable.", e);
            throw new ApiException("YouTube is not configured correctly. Please contact the site administrator.", e);
        }
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(24, value));
    }
}