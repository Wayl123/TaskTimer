package com.waynelee.tasktimer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.security.InvalidParameterException;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        CursorRecyclerViewAdapter.OnTaskClickListener{
    private static final String TAG = "MainActivityFragment";

    public static final int LOADER_ID = 0;

    private CursorRecyclerViewAdapter mAdapter;

    private Timing mCurrentTiming = null;

    public MainActivityFragment() {
        Log.d(TAG, "MainActivityFragment: ");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: starts");
        super.onActivityCreated(savedInstanceState);

        // Activities containing this fragment must implement its callbacks
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof CursorRecyclerViewAdapter.OnTaskClickListener)) {
            throw new ClassCastException(activity.getClass().getSimpleName()
                    + " must implement CursorRecyclerViewAdapter.OnTaskClickListener interface");
        }

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
        setTimingText(mCurrentTiming);
    }

    @Override
    public void onEditClick(@NonNull Task task) {
        Log.d(TAG, "onEditClick: called");
        CursorRecyclerViewAdapter.OnTaskClickListener listener = (CursorRecyclerViewAdapter.OnTaskClickListener) getActivity();
        if (listener != null) {
            listener.onEditClick(task);
        }
    }

    @Override
    public void onDeleteClick(@NonNull Task task) {
        Log.d(TAG, "onDeleteClick: called");
        CursorRecyclerViewAdapter.OnTaskClickListener listener = (CursorRecyclerViewAdapter.OnTaskClickListener) getActivity();
        if (listener != null) {
            listener.onDeleteClick(task);
        }
    }

    @Override
    public void onTaskLongClick(@NonNull Task task) {
        Log.d(TAG, "onTaskLongClick: called");
        if (mCurrentTiming != null) {
            if (task.getId() == mCurrentTiming.getTask().getId()) {
                // The current task was tapped a second time, so stop timing
                saveTiming(mCurrentTiming);
                mCurrentTiming = null;
                setTimingText(null);
            } else {
                // A new task is being timed, so stop the old one first
                saveTiming(mCurrentTiming);
                mCurrentTiming = new Timing(task);
                setTimingText(mCurrentTiming);
            }
        } else {
            // No task being timed, so start timing the new task
            mCurrentTiming = new Timing(task);
            setTimingText(mCurrentTiming);
        }
    }

    private void saveTiming(@NonNull Timing currentTiming) {
        Log.d(TAG, "saveTiming: entering");

        // If we have an open timing, set the duration and save
        currentTiming.setDuration();

        ContentResolver contentResolver = getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(TimingsContract.Columns.TIMINGS_TASK_ID, currentTiming.getTask().getId());
        values.put(TimingsContract.Columns.TIMINGS_START_TIME, currentTiming.getStartTime());
        values.put(TimingsContract.Columns.TIMINGS_DURATION, currentTiming.getDuration());

        // Update table in database
        contentResolver.insert(TimingsContract.CONTENT_URI, values);

        Log.d(TAG, "saveTiming: exiting");
    }

    private void setTimingText(Timing timing) {
        TextView taskName = getActivity().findViewById(R.id.current_task);

        if (timing != null) {
            taskName.setText(getString(R.string.current_timing_text, timing.getTask().getName()));
        } else {
            taskName.setText(getString(R.string.no_task_message));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: starts");
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.task_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (mAdapter == null) {
            mAdapter = new CursorRecyclerViewAdapter(null, this);
        }
        recyclerView.setAdapter(mAdapter);

        Log.d(TAG, "onCreateView: returning");
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Log.d(TAG, "onCreateLoader: starts with id " + id);
        String[] projection = {TasksContract.Columns._ID,
                TasksContract.Columns.TASKS_NAME,
                TasksContract.Columns.TASKS_DESCRIPTION,
                TasksContract.Columns.TASKS_SORTORDER};
        String sortOrder = TasksContract.Columns.TASKS_SORTORDER + "," + TasksContract.Columns.TASKS_NAME + " COLLATE NOCASE";

        switch (id) {
            case LOADER_ID:
                return new CursorLoader(getActivity(),
                        TasksContract.CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder);
            default:
                throw new InvalidParameterException(TAG + ".onCreateLoader called with invalid loader id " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished: entering");
        mAdapter.swapCursor(data);
        int count = mAdapter.getItemCount();

        Log.d(TAG, "onLoadFinished: count is " + count);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset: starts");
        mAdapter.swapCursor(null);
    }
}