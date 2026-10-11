<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.app.db.VideoSchema" %>
<%@ page import="com.app.helpers.YouTubeDataApi" %>
<%@ page import="com.app.helpers.HtmlUtils" %>

<%
    String searchTerm = request.getParameter("q");
    if (searchTerm == null) searchTerm = "";
    searchTerm = searchTerm.trim();

    List<VideoSchema> videos = List.of();
    String apiError = null;
    try {
        videos = searchTerm.isBlank()
                ? YouTubeDataApi.getPopularVideos(18)
                : YouTubeDataApi.searchVideos(searchTerm, 18);
    } catch (YouTubeDataApi.ApiException e) {
        apiError = e.getMessage();
    }

    String userName = (String) session.getAttribute("userName");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>YouTube</title>
    <link rel="stylesheet" href="css/YTDashboard.css">
</head>
<body>

<header class="header">
    <div>
        <h1>Welcome<%= userName == null ? "" : ", " + HtmlUtils.escape(userName) %></h1>
        <p>Explore videos and discover something new.</p>
    </div>

    <div>
        <form method="get" action="YTDashboard.jsp">
            <input type="search" name="q" value="<%= HtmlUtils.escape(searchTerm) %>"
                   placeholder="Search YouTube..." autocomplete="off">
            <button type="submit">Search</button>
        </form>
        <form action="LogoutServlet" method="post">
            <button type="submit">Logout</button>
        </form>
    </div>
</header>

<main class="videos">
<%
    if (apiError != null) {
%>
    <section class="video">
        <div class="video-info" role="alert">
            <h3>Videos couldn't be loaded</h3>
            <p><%= HtmlUtils.escape(apiError) %></p>
            <p>Please try again in a moment.</p>
        </div>
    </section>
<%
    } else if (videos.isEmpty()) {
%>
    <section class="video">
        <div class="video-info">
            <h3><%= searchTerm.isBlank() ? "No popular videos are available right now" : "No videos found" %></h3>
            <p><%= searchTerm.isBlank()
                    ? "Try refreshing the page in a moment."
                    : "Try different keywords and search again." %></p>
        </div>
    </section>
<%
    } else {
        for (VideoSchema video : videos) {
%>
    <article class="video">
        <a class="video-open"
           href="https://www.youtube.com/watch?v=<%= HtmlUtils.escape(video.getVideoID()) %>"
           data-video-id="<%= HtmlUtils.escape(video.getVideoID()) %>"
           data-video-title="<%= HtmlUtils.escape(video.getVideoName()) %>"
           aria-label="Play <%= HtmlUtils.escape(video.getVideoName()) %> inside YouTube-2026">
            <img class="video-thumbnail"
                 src="<%= HtmlUtils.escape(video.getThumbnail()) %>"
                 alt=""
                 loading="lazy">
            <span class="play-overlay" aria-hidden="true">▶</span>
        </a>
        <div class="video-info">
            <h3 title="<%= HtmlUtils.escape(video.getVideoName()) %>">
                <%= HtmlUtils.escape(video.getVideoName()) %>
            </h3>
            <p><%= HtmlUtils.escape(video.getChannelName()) %></p>
            <small>
                <%= HtmlUtils.escape(video.getVideoDuration()) %>
                <% if (!video.getViewCount().isBlank()) { %>
                    · <%= HtmlUtils.escape(video.getViewCount()) %> views
                <% } %>
            </small>
        </div>
    </article>
<%
        }
    }
%>
</main>

<div class="player-modal" id="playerModal" hidden>
    <div class="player-backdrop" data-player-close></div>
    <section class="player-dialog" role="dialog" aria-modal="true"
             aria-labelledby="playerTitle" tabindex="-1">
        <div class="player-heading">
            <h2 id="playerTitle">Now playing</h2>
            <button type="button" class="player-close" id="playerClose" aria-label="Close video player">×</button>
        </div>
        <div class="player-frame-wrap">
            <iframe id="videoPlayer" title="YouTube video player"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    referrerpolicy="strict-origin-when-cross-origin"
                    allowfullscreen></iframe>
        </div>
        <p class="player-fallback" id="playerFallback" hidden>
            This video cannot be embedded. You can
            <a id="openOnYouTube" href="#" target="_blank" rel="noopener noreferrer">open it on YouTube</a>.
        </p>
    </section>
</div>

<script>
(() => {
    const modal = document.getElementById('playerModal');
    const dialog = modal.querySelector('.player-dialog');
    const iframe = document.getElementById('videoPlayer');
    const title = document.getElementById('playerTitle');
    const closeButton = document.getElementById('playerClose');
    const fallback = document.getElementById('playerFallback');
    const fallbackLink = document.getElementById('openOnYouTube');
    let previousFocus = null;

    function closePlayer() {
        modal.hidden = true;
        iframe.src = 'about:blank';
        fallback.hidden = true;
        if (previousFocus) previousFocus.focus();
    }

    document.querySelectorAll('.video-open').forEach(link => {
        link.addEventListener('click', event => {
            const videoId = link.dataset.videoId || '';
            // YouTube video IDs use 11 URL-safe characters. Leave the normal link
            // behavior intact if an unexpected ID is returned by the API.
            if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) return;

            event.preventDefault();
            previousFocus = link;
            title.textContent = link.dataset.videoTitle || 'Now playing';
            fallbackLink.href = 'https://www.youtube.com/watch?v=' + videoId;
            fallback.hidden = true;
            iframe.src = 'https://www.youtube-nocookie.com/embed/' + videoId + '?autoplay=1';
            modal.hidden = false;
            closeButton.focus();
        });
    });

    closeButton.addEventListener('click', closePlayer);
    modal.querySelectorAll('[data-player-close]').forEach(element => {
        element.addEventListener('click', closePlayer);
    });
    document.addEventListener('keydown', event => {
        if (!modal.hidden && event.key === 'Escape') closePlayer();
    });

    // Browsers do not expose a reliable cross-origin signal for blocked embeds;
    // this link is also available to users if YouTube displays an embed error.
})();
</script>

</body>
</html>