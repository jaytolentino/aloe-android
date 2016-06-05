package com.onepinkhat.aloe.models;

import android.support.annotation.Nullable;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.List;

/**
 * Model representing details about returned event results through the Google Calendar API
 *
 * Created by jay on 6/4/16.
 */
public class EventsResult {

    private Events events;
    private String errorMessage;

    public EventsResult(){ /* NOP */ }

    public EventsResult(@Nullable Events events) {
        this.events = events;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns info about the events as well as the event items themselves, null if there was an
     * issue retrieving the events
     *
     * @return {@link Events} resulting from a Google Calendar API request
     */
    @Nullable
    public Events getEvents() {
        return events;
    }

    /**
     * Returns only the event items themselves, null if there was an issue retrieving the events
     *
     * @return a list of {@link Event} items with info about each individual event
     */
    @Nullable
    public List<Event> getEventItems() {
        return events != null ? events.getItems() : null;
    }


    /**
     * Returns the error message that occured while retrieving events, null if no error
     *
     * @return a String representing the error message
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
}
