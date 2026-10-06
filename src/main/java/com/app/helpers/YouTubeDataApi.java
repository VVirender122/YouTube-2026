package com.app.helpers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.app.config.SecretsReader;
import com.app.db.VideoSchema;

public final class YouTubeDataApi {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String BASE =
            "https://www.googleapis.com/youtube/v3/";
    private static final Locale LOCALE = Locale.US;

    private YouTubeDataApi() {
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

        try {
            JSONObject root = getJson(url);
            JSONArray items = root.optJSONArray("items");
            List<VideoSchema> results = new ArrayList<>();

            if (items == null) {
                return results;
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                JSONObject id = item == null ? null : item.optJSONObject("id");
                JSONObject snippet = item == null ? null : item.optJSONObject("snippet");
                if (id == null || snippet == null) continue;

                String videoId = id.optString("videoId", "");
                if (videoId.isBlank()) continue;

                results.add(new VideoSchema(
                        snippet.optString("title", "Untitled"),
                        videoId,
                        thumbnail(snippet),
                        "",
                        "",
                        snippet.optString("channelTitle", "Unknown channel")));
            }

            // Search results don't include statistics/duration; enrich them in one request.
            if (!results.isEmpty()) {
                return enrich(results);
            }
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<VideoSchema> fetchVideoDetails(String url) {
        try {
            JSONObject root = getJson(url);
            JSONArray items = root.optJSONArray("items");
            List<VideoSchema> results = new ArrayList<>();

            if (items == null) return results;

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;

                JSONObject snippet = item.optJSONObject("snippet");
                JSONObject details = item.optJSONObject("contentDetails");
                JSONObject stats = item.optJSONObject("statistics");
                if (snippet == null) continue;

                results.add(toSchema(item.optString("id", ""), snippet, details, stats));
            }
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<VideoSchema> enrich(List<VideoSchema> videos) {
        String ids = videos.stream()
                .map(VideoSchema::getVideoID)
                .filter(id -> !id.isBlank())
                .map(id -> URLEncoder.encode(id, StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "%2C" + b)
                .orElse("");

        if (ids.isBlank()) return videos;

        String url = BASE + "videos?part=contentDetails,statistics"
                + "&id=" + ids + "&key=" + apiKey();

        try {
            JSONObject root = getJson(url);
            JSONArray items = root.optJSONArray("items");
            if (items == null) return videos;

            java.util.Map<String, JSONObject> byId = new java.util.HashMap<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) byId.put(item.optString("id"), item);
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
        } catch (Exception e) {
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
        JSONObject high = thumbnails.optJSONObject("high");
        if (high == null) high = thumbnails.optJSONObject("medium");
        if (high == null) high = thumbnails.optJSONObject("default");
        return high == null ? "" : high.optString("url", "");
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

    private static JSONObject getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("YouTube API returned HTTP " + response.statusCode());
        }

        return new JSONObject(response.body());
    }

    private static String apiKey() {
        return SecretsReader.readData("secrets", LOCALE, "YOUTUBE_API_KEY");
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(24, value));
    }
}
