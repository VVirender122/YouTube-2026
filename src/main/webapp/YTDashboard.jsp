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
        <a href="https://www.youtube.com/watch?v=<%= HtmlUtils.escape(video.getVideoID()) %>"
           target="_blank" rel="noopener noreferrer">
            <img class="video-thumbnail"
                 src="<%= HtmlUtils.escape(video.getThumbnail()) %>"
                 alt="<%= HtmlUtils.escape(video.getVideoName()) %>"
                 loading="lazy">
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

</body>
</html>