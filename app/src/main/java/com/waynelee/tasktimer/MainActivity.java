package com.waynelee.tasktimer;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.waynelee.tasktimer.debug.TestData;

public class MainActivity extends AppCompatActivity implements CursorRecyclerViewAdapter.OnTaskClickListener,
        AddEditActivityFragment.OnSaveClicked,
        AppDialog.DialogEvents {
    private static final String TAG = "MainActivity";

    // Whether in landscape or on a tablet
    private boolean mTwoPane = false;

    public static final int DIALOG_ID_DELETE = 1;
    public static final int DIALOG_ID_CANCEL_EDIT = 2;
    private static final int DIALOG_ID_CANCEL_EDIT_UP = 3;

    private AlertDialog mDialog = null; // Module scope because we need to dismiss it in onStop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTwoPane = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        Log.d(TAG, "onCreate: twoPane is " + mTwoPane);

        FragmentManager fragmentManager = getSupportFragmentManager();
        // If the AddEditActivity fragment exists, we're editing
        boolean editing = fragmentManager.findFragmentById(R.id.task_details_container) != null;
        Log.d(TAG, "onCreate: editing is " + editing);

        // We need references to the containers, so we can show or hide them as necessary.
        // No need to case them, as we're only calling a method that's available for all views.
        View mainFragment = findViewById(R.id.fragment);
        View addEditLayout = findViewById(R.id.task_details_container);

        if (mTwoPane) {
            Log.d(TAG, "onCreate: twoPane mode");
            mainFragment.setVisibility(View.VISIBLE);
            addEditLayout.setVisibility(View.VISIBLE);
        } else if (editing) {
            Log.d(TAG, "onCreate: single pane, editing");
            // Hide the left hand fragment, to make room for editing
            mainFragment.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "onCreate: single pane, not editing");
            // Show left hand fragment
            mainFragment.setVisibility(View.VISIBLE);
            // Hide the editing frame
            addEditLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveClicked() {
        Log.d(TAG, "onSaveClicked: starts");
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.task_details_container);
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commit();
        }
        if (!mTwoPane) {
            View mainFragment = findViewById(R.id.fragment);
            View addEditLayout = findViewById(R.id.task_details_container);

            // and make sure the MainActivityFragment is visible.
            mainFragment.setVisibility(View.VISIBLE);
            // We've just removed the editing fragment, so hide the frame
            addEditLayout.setVisibility(View.GONE);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (BuildConfig.DEBUG) {
            MenuItem generate = menu.findItem(R.id.menumain_generate);
            generate.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.menumain_addTask:
                taskEditRequest(null);
                break;
            case R.id.menumain_showDurations:
                startActivity(new Intent(this, DurationsReport.class));
                break;
            case R.id.menumain_settings:
                break;
            case R.id.menumain_showAbout:
                showAboutDialog();
                break;
            case R.id.menumain_generate:
                TestData.generateTestData(getContentResolver());
                break;
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected: home button pressed");
                AddEditActivityFragment fragment = (AddEditActivityFragment)
                        getSupportFragmentManager().findFragmentById(R.id.task_details_container);
                if (fragment != null && fragment.canClose()) {
                    return super.onOptionsItemSelected(item);
                } else {
                    showConfirmationDialog(DIALOG_ID_CANCEL_EDIT_UP);
                    return true; // Indicate we are handling this
                }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    public void showAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);

        builder.setView(messageView);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            }
        });

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);

        TextView tv = messageView.findViewById(R.id.about_version);
        tv.setText("v" + BuildConfig.VERSION_NAME);

        TextView about_url = messageView.findViewById(R.id.about_url);
        if (about_url != null) {
            about_url.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String s = ((TextView) view).getText().toString();
                    intent.setData(Uri.parse(s));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "No browser application found, cannot visit world-wide web", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }

        mDialog.show();
    }

    @Override
    public void onEditClick(@NonNull Task task) {
        taskEditRequest(task);
    }

    @Override
    public void onDeleteClick(@NonNull Task task) {
        Log.d(TAG, "onDeleteClick: starts");

        AppDialog dialog = new AppDialog();
        Bundle args = new Bundle();
        args.putInt(AppDialog.DIALOG_ID, DIALOG_ID_DELETE);
        args.putString(AppDialog.DIALOG_MESSAGE, getString(R.string.deldiag_message, task.getId(), task.getName()));
        args.putInt(AppDialog.DIALOG_POSITIVE_RID, R.string.deldiag_positive_caption);

        args.putLong("TaskId", task.getId());

        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onTaskLongClick(@NonNull Task task) {
        // Required to satisfy the interface
    }

    private void taskEditRequest(Task task) {
        Log.d(TAG, "taskEditRequest: starts");
        AddEditActivityFragment fragment = new AddEditActivityFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(Task.class.getSimpleName(), task);
        fragment.setArguments(arguments);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.task_details_container, fragment)
                .commit();
        if (!mTwoPane) {
            // Hide the left hand fragment and show the right hand frame
            View mainFragment = findViewById(R.id.fragment);
            View addEditLayout = findViewById(R.id.task_details_container);

            mainFragment.setVisibility(View.GONE);
            addEditLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPositiveDialogResult(int dialogId, Bundle args) {
        Log.d(TAG, "onPositiveDialogResult: called");
        switch (dialogId) {
            case DIALOG_ID_DELETE:
                long taskId = args.getLong("TaskId");
                if (BuildConfig.DEBUG && taskId == 0) {
                    throw new AssertionError("Task ID is zero");
                }
                getContentResolver().delete(TasksContract.buildTaskUri(taskId), null, null);
                break;
            case DIALOG_ID_CANCEL_EDIT:
            case DIALOG_ID_CANCEL_EDIT_UP:
                // No action required
                break;
        }
    }

    @Override
    public void onNegativeDialogResult(int dialogId, Bundle args) {
        Log.d(TAG, "onNegativeDialogResult: called");
        switch (dialogId) {
            case DIALOG_ID_DELETE:
                // No action required
                break;
            case DIALOG_ID_CANCEL_EDIT:
            case DIALOG_ID_CANCEL_EDIT_UP:
                // If we're editing, remove the fragment, otherwise, close the app
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.task_details_container);
                if (fragment != null) {
                    // We are editing
                    getSupportFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commit();
                    if (mTwoPane) {
                        // In landscape, quit only if back button was used
                        if (dialogId == DIALOG_ID_CANCEL_EDIT) {
                            finish();
                        }
                    } else {
                        // Hide the edit container in single pane mode
                        // and make sure the left-hand container is visible
                        View mainFragment = findViewById(R.id.fragment);
                        View addEditLayout = findViewById(R.id.task_details_container);
                        // We're just removed the editing fragment, so hide the frame
                        addEditLayout.setVisibility(View.GONE);
                        // and make sure the MainActivityFragment is visible
                        mainFragment.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Not editing, so quit regardless of orientation
                    finish();
                }
                break;
        }
    }

    @Override
    public void onDialogCancelled(int dialogId) {
        Log.d(TAG, "onDialogCancelled: called");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: called");
        FragmentManager fragmentManager = getSupportFragmentManager();
        AddEditActivityFragment fragment = (AddEditActivityFragment) fragmentManager.findFragmentById(R.id.task_details_container);
        if ((fragment == null) || fragment.canClose()) {
            super.onBackPressed();
        } else {
            // Show dialogue to get confirmation to quit editing
            showConfirmationDialog(DIALOG_ID_CANCEL_EDIT);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void showConfirmationDialog(int dialogId) {
        // Show dialogue to get confirmation to quit editing
        AppDialog dialog = new AppDialog();
        Bundle args = new Bundle();
        args.putInt(AppDialog.DIALOG_ID, dialogId);
        args.putString(AppDialog.DIALOG_MESSAGE, getString(R.string.cancelEditDiag_message));
        args.putInt(AppDialog.DIALOG_POSITIVE_RID, R.string.cancelEditDiag_positive_caption);
        args.putInt(AppDialog.DIALOG_NEGATIVE_RID, R.string.cancelEditDiag_negative_caption);

        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), null);
    }
}