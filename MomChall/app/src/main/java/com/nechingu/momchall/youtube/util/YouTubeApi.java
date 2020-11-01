/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nechingu.momchall.youtube.util;

import android.util.Log;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.LiveBroadcasts.Transition;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.IngestionInfo;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.MonitorStreamInfo;
import com.nechingu.momchall.youtube.YoutubeActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class YouTubeApi {

    public static final String RTMP_URL_KEY = "rtmpUrl";
    public static final String BROADCAST_ID_KEY = "broadcastId";
    private static final int FUTURE_DATE_OFFSET_MILLIS = 5 * 1000;

//    public static void createLiveEvent(YouTube youtube, String description, String name) {
    public static String createLiveEvent(YouTube youtube, String description, String name) {
        // We need a date that's in the proper ISO format and is in the future,
        // since the API won't
        // create events that start in the past.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long futureDateMillis = System.currentTimeMillis() + FUTURE_DATE_OFFSET_MILLIS;

        Date futureDate = new Date();
        futureDate.setTime(futureDateMillis);
        String date = dateFormat.format(futureDate);

        Log.i(YoutubeActivity.APP_NAME, String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date));

        try {
            LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
            broadcastSnippet.setTitle(name);
            broadcastSnippet.setDescription(description);
            broadcastSnippet.setScheduledStartTime(new DateTime(futureDate));

            LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
            MonitorStreamInfo monitorStream = new MonitorStreamInfo();
            monitorStream.setEnableMonitorStream(true);         // iOS와 동일하게 변경
            // monitorStream.setEnableMonitorStream(false);
            contentDetails.setMonitorStream(monitorStream);

            // Create LiveBroadcastStatus with privacy status.
            LiveBroadcastStatus status = new LiveBroadcastStatus();
//            status.setPrivacyStatus("unlisted");
            status.setPrivacyStatus("public");

            LiveBroadcast broadcast = new LiveBroadcast();
            broadcast.setKind("youtube#liveBroadcast");
            broadcast.setSnippet(broadcastSnippet);
            broadcast.setStatus(status);
            broadcast.setContentDetails(contentDetails);

            // Create the insert request
//            YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube.liveBroadcasts().insert("snippet,status,contentDetails", broadcast);
            // iOS
            YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube.liveBroadcasts().insert("id,snippet,status,contentDetails", broadcast);

            // Request is executed and inserted broadcast is returned
            LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();
            // Print information from the API response.

            // Create a snippet with title.
            LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
            streamSnippet.setTitle(name);
            streamSnippet.setDescription(description);

            // Create content distribution network with format and ingestion
            // type.
            CdnSettings cdn = new CdnSettings();
            cdn.setFormat("720p");
            cdn.setIngestionType("rtmp");

            LiveStream stream = new LiveStream();
            stream.setKind("youtube#liveStream");
            stream.setSnippet(streamSnippet);
            stream.setCdn(cdn);

            // Create the insert request
//            YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams().insert("snippet,cdn", stream);
            // iOS
            YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams().insert("id,snippet,cdn,status", stream);

            // Request is executed and inserted stream is returned
            LiveStream returnedStream = liveStreamInsert.execute();

            // Create the bind request
//            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube.liveBroadcasts().bind(returnedBroadcast.getId(), "id,contentDetails");
            // iOS
            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube.liveBroadcasts().bind(returnedBroadcast.getId(), "id,snippet,contentDetails,status");

            // Set stream id to bind
            liveBroadcastBind.setStreamId(returnedStream.getId());

            // Request is executed and bound broadcast is returned
            liveBroadcastBind.execute();

            Log.w("SeongKwon", "\n================== createLiveEvent ==================\n");
            Log.w("SeongKwon", "returnedBroadcast - Id: " + returnedBroadcast.getId());
            Log.w("SeongKwon", "\n================== createLiveEvent ==================\n");
            return returnedBroadcast.getId();
        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getStackTrace());
            t.printStackTrace();
        }
        return "";
    }

    // TODO: Catch those exceptions and handle them here.
//    public static List<EventData> getLiveEvents(YouTube youtube) throws IOException {
    public static List<EventData> getLiveEvents(YouTube youtube, String broadcastId) throws IOException {
        Log.i(YoutubeActivity.APP_NAME, "Requesting live events.");

        YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube.liveBroadcasts().list("id,snippet,contentDetails,status");
        // liveBroadcastRequest.setMine(true);
//        liveBroadcastRequest.setBroadcastStatus("upcoming");

        liveBroadcastRequest.setId(broadcastId);

        // List request is executed and list of broadcasts are returned
        LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();

        // Get the list of broadcasts associated with the user.
        List<LiveBroadcast> returnedList = returnedListResponse.getItems();

        List<EventData> resultList = new ArrayList<EventData>(returnedList.size());
        EventData event;

        for (LiveBroadcast broadcast : returnedList) {
            event = new EventData();
            event.setEvent(broadcast);
            String streamId = broadcast.getContentDetails().getBoundStreamId();
            if (streamId != null) {
                String ingestionAddress = getIngestionAddress(youtube, streamId);
                event.setIngestionAddress(ingestionAddress);
            }
            resultList.add(event);
        }
        return resultList;
    }

//    public static void startEvent(YouTube youtube, String broadcastId)
    public static void startEvent(YouTube youtube, String broadcastId, String type) // type - live, testing
        throws IOException {

        Log.w("SeongKwon", "\n========================= startEvent =========================\n");
        Log.w("SeongKwon", "broadcastId = " + broadcastId);
        Log.w("SeongKwon", "\n--------------------------------------------------------------\n");

//        Transition transitionRequest = youtube.liveBroadcasts().transition("live", broadcastId, "status");
        Transition transitionRequest = youtube.liveBroadcasts().transition(type, broadcastId, "id,snippet,contentDetails,status");
        transitionRequest.execute();
    }

    public static void endEvent(YouTube youtube, String broadcastId) throws IOException {
//        Transition transitionRequest = youtube.liveBroadcasts().transition("completed", broadcastId, "status");
        Transition transitionRequest = youtube.liveBroadcasts().transition("complete", broadcastId, "id,snippet,contentDetails,status");
        transitionRequest.execute();
    }

    public static String getIngestionAddress(YouTube youtube, String streamId)
            throws IOException {
//        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams().list("cdn");
        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams().list("id,snippet,cdn,status");
        liveStreamRequest.setId(streamId);
        LiveStreamListResponse returnedStream = liveStreamRequest.execute();

        List<LiveStream> streamList = returnedStream.getItems();

        // Print information from the API response.
        Log.w("SeongKwon", "\n================== Returned Streams ==================\n");
        for (LiveStream stream : streamList) {
            Log.w("SeongKwon", "LiveStream - Id: " + stream.getId());
            Log.w("SeongKwon", "\n-------------------------------------------------------------\n");
        }

        if (streamList.isEmpty()) {
            return "";
        }
        IngestionInfo ingestionInfo = streamList.get(0).getCdn().getIngestionInfo();
        return ingestionInfo.getIngestionAddress() + "/"
                + ingestionInfo.getStreamName();
    }
}