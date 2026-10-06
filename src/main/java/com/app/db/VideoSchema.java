package com.app.db;

public class VideoSchema {

    private final String videoName;
    private final String videoID;
    private final String thumbnail;
    private final String videoDuration;
    private final String viewCount;
    private final String channelName;

    public VideoSchema(String videoName, String videoID, String thumbnail,
                       String videoDuration, String viewCount, String channelName) {
        this.videoName = videoName;
        this.videoID = videoID;
        this.thumbnail = thumbnail;
        this.videoDuration = videoDuration;
        this.viewCount = viewCount;
        this.channelName = channelName;
    }

    public String getVideoName() { return videoName; }
    public String getVideoID() { return videoID; }
    public String getThumbnail() { return thumbnail; }
    public String getVideoDuration() { return videoDuration; }
    public String getViewCount() { return viewCount; }
    public String getChannelName() { return channelName; }
}
