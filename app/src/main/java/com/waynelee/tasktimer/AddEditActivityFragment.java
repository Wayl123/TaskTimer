package com.waynelee.tasktimer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddEditActivityFragment extends Fragment {
    private static final String TAG = "AddEditActivityFragment";

    private enum FragmentEditMode {EDIT, ADD}
    private FragmentEditMode mMode;

    private EditText mNameTextView;
    private EditText mDescriptionTextView;
    private EditText mSortOrderTextView;
    private OnSaveClicked mSaveListener = null;

    interface OnSaveClicked {
        void onSaveClicked();
    }

    public AddEditActivityFragment() {
        Log.d(TAG, "AddEditActivityFragment: constructor called");
    }

    public boolean canClose() {
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.d(TAG, "onAttach: starts");
        super.onAttach(context);

        // Activities containing this fragment must implement its callbacks.
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof OnSaveClicked)) {
            throw new ClassCastException(activity.getClass().getSimpleName()
                + " must implement AddEditActivityFragment.OnSaveClicked interface");
        }
        mSaveListener = (OnSaveClicked) activity;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach: starts");
        super.onDetach();
        mSaveListener = null;

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: starts");
        View view = inflater.inflate(R.layout.fragment_add_edit_activity, container, false);

        mNameTextView = view.findViewById(R.id.addedit_name);
        mDescriptionTextView = view.findViewById(R.id.addedit_description);
        mSortOrderTextView = view.findViewById(R.id.addedit_sortorder);
        Button saveButton = view.findViewById(R.id.addedit_save);

        Bundle arguments = getArguments();

        final Task task;
        if (arguments != null) {
            Log.d(TAG, "onCreateView: retrieving task details.");

            task = (Task) arguments.getSerializable(Task.class.getSimpleName());
            if (task != null) {
                Log.d(TAG, "onCreateView: task details found, editing...");
                mNameTextView.setText(task.getName());
                mDescriptionTextView.setText(task.getDescription());
                mSortOrderTextView.setText(Integer.toString(task.getSortOrder()));
                mMode = FragmentEditMode.EDIT;
            } else {
                mMode = FragmentEditMode.ADD;
            }
        } else {
            task = null;
            Log.d(TAG, "onCreateView: no arguments, adding new record");
            mMode = FragmentEditMode.ADD;
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //update the database if at least one field has changed
                int so; //to save repeated conversions to int
                if (mSortOrderTextView.length() > 0) {
                    so = Integer.parseInt(mSortOrderTextView.getText().toString());
                } else {
                    so = 0;
                }

                ContentResolver contentResolver = getActivity().getContentResolver();
                ContentValues values = new ContentValues();

                switch (mMode) {
                    case EDIT:
                        if (task != null) {
                            if (!mNameTextView.getText().toString().equals(task.getName())) {
                                values.put(TasksContract.Columns.TASKS_NAME, mNameTextView.getText().toString());
                            }
                            if (!mDescriptionTextView.getText().toString().equals(task.getDescription())) {
                                values.put(TasksContract.Columns.TASKS_DESCRIPTION, mDescriptionTextView.getText().toString());
                            }
                            if (so != task.getSortOrder()) {
                                values.put(TasksContract.Columns.TASKS_SORTORDER, so);
                            }
                            if (values.size() != 0) {
                                Log.d(TAG, "onClick: updating task");
                                contentResolver.update(TasksContract.buildTaskUri(task.getId()), values, null, null);
                            }
                        }
                        break;
                    case ADD:
                        if (mNameTextView.length() > 0) {
                            Log.d(TAG, "onClick: adding new task");
                            values.put(TasksContract.Columns.TASKS_NAME, mNameTextView.getText().toString());
                            values.put(TasksContract.Columns.TASKS_DESCRIPTION, mDescriptionTextView.getText().toString());
                            values.put(TasksContract.Columns.TASKS_SORTORDER, so);
                            contentResolver.insert(TasksContract.CONTENT_URI, values);
                        }
                        break;
                }
                Log.d(TAG, "onClick: done editing");

                if (mSaveListener != null) {
                    mSaveListener.onSaveClicked();
                }
            }
        });
        Log.d(TAG, "onCreateView: exiting...");

        return view;
    }
}