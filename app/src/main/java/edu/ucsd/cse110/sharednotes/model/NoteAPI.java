package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: - getNote (maybe getNoteAsync)
    // TODO: - putNote (don't need putNotAsync, probably)
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    public Note getNote(String title) throws ExecutionException, InterruptedException, TimeoutException {
        String noteTitle = title.replace(" ", "%20");

        Log.i("GET", noteTitle);

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + noteTitle)
                .method("GET", null)
                .build();

        var executor = Executors.newSingleThreadExecutor();
        Callable<Note> callable = () -> {
            var response = client.newCall(request).execute();
            assert response.body() != null;
            var body = response.body().string();
            Log.i("GET NOTE", body);
            return Note.fromJSON(body);
        };
//        try (var response = client.newCall(request).execute()) {
//            assert response.body() != null;
//            String noteJson = response.body().string();
//            Log.i("GET", noteJson);
//            return Note.fromJSON(noteJson);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Future<Note> future_get = executor.submit(callable);
        Note note_get = future_get.get(1, TimeUnit.SECONDS);
        return note_get;
//        return null;
    }

    public void postNote(Note note) {
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String noteTitle = note.title.replace(" ", "%20");

        String noteJson = note.toJSON();
        Log.i("POSTNOTE JSON", noteJson + " " +noteTitle);
        Thread putThread = new Thread(() -> {
            var body = RequestBody.create(noteJson, JSON);
            Request request = new Request.Builder()
                    .url("https://sharednotes.goto.ucsd.edu/notes/" + noteTitle)
                    .put(body)
                    .build();
            try (var response = client.newCall(request).execute()) {
                assert response.body() != null;
                Log.i("POSTNOTE", response.body().string());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        putThread.start();
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }
}
