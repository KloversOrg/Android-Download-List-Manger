package com.lxy.downloadsimple;

import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.golshadi.majid.core.DownloadManagerPro;
import com.golshadi.majid.core.enums.TaskStates;
import com.golshadi.majid.report.ReportStructure;
import com.golshadi.majid.report.listener.DownloadManagerListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayoutManager manager;
    private Adapter adapter;
    private String url = "http://downpack.baidu.com/appsearch_AndroidPhone_1012271a.apk";
    private FloatingActionButton floatButton;
    private DownloadManagerPro dm;
    public static DecimalFormat mDecimalFormat = new DecimalFormat("#0.00");
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos = -1;
            int size = adapter.datas.size();
            for (int i = 0; i < size; i++) {
                if (adapter.datas.get(i).getId() == msg.arg1)
                    pos = i;
            }
            if (pos == -1)
                return;
            int firstPos = manager.findFirstVisibleItemPosition();
            View child = recyclerView.getChildAt(pos - firstPos);
            if (child == null) {
                return;
            }
            TextView total_size = (TextView) child.findViewById(R.id.textView3);
            TextView down_size = (TextView) child.findViewById(R.id.textView5);
            ProgressBar pb = (ProgressBar) child.findViewById(R.id.progressBar);
            TextView file_name = (TextView) child.findViewById(R.id.textView2);
            TextView state = (TextView) child.findViewById(R.id.textView);
            switch (msg.what) {
                case TaskStates.INIT:
                    state.setText("准备完成");
                    ReportStructure structure = dm.singleDownloadStatus(msg.what);
                    total_size.setText(String.valueOf(structure.getFileSize()));
                    break;
                case TaskStates.DOWNLOAD_FINISHED:
                    state.setText("下载成功,正在合成");
                    break;
                case TaskStates.DOWNLOADING:
                    Bundle bundle = msg.getData();
                    double precent = bundle.getDouble("percent");
                    long down_length = bundle.getLong("down_length");
                    int pro = (int) precent;
                    if (pb.getProgress() != pro)
                        pb.setProgress(pro);

                    down_size.setText(mDecimalFormat.format(down_length / 1024.00 / 1024.00));
                    total_size.setText(mDecimalFormat.format(down_length / precent / 1024.00 / 1024.00 * 100) + "M");
                    state.setText(mDecimalFormat.format(precent) + "%");
                    break;
                case TaskStates.END:
                    state.setText("下载完成");
                    down_size.setText(total_size.getText().toString());
                    pb.setProgress(100);
                    break;
                case TaskStates.PAUSED:
                    state.setText("下载暂停");
                    break;
                case TaskStates.READY:
                    state.setText("正在开始...");

                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        manager = new LinearLayoutManager(this);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        floatButton = (FloatingActionButton) findViewById(R.id.floatButton);
        recyclerView.setLayoutManager(manager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        floatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    adapter.notifyItemInserted(0);//添加任务
                    dm.startDownload(dm.addTask("yuan", url, false, false));
                    adapter.datas = dm.downloadTasksInSameState(7);
                    adapter.notifyItemRangeChanged(0,adapter.datas.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        dm = new DownloadManagerPro(this);
        dm.init(Environment.getExternalStorageDirectory().getPath(), 3, new DownLoadListener());
//        View child = recyclerView.getChildAt(1-manager.findFirstVisibleItemPosition()+1);
//        public static final int INIT        = 0;
//        public static final int READY       = 1;
//        public static final int DOWNLOADING = 2;
//        public static final int PAUSED      = 3;
//        public static final int DOWNLOAD_FINISHED = 4;
//        public static final int END         = 5;
//        TaskState.INIT: task intruduce for library and gave you token back but it didn't started yet.
//        TaskState.READY: download task data fetch from its URL and it's ready to start.
//        TaskState.DOWNLOADING: download task in downloading process.
//        TaskState.PAUSED: download task in puase state. If in middle of downloading process internet disconnected; task goes to puase state and you can start it later
//        TaskState.DOWNLOAD_FINISHED: download task downloaded completely but their chunks did not rebuild.
//        TaskState.END: after rebuild download task chunks, task goes to this state and notified developer with OnDownloadCompleted(long taskToken) interface
        List<ReportStructure> list = dm.downloadTasksInSameState(7);//获取所有记录

        if (list == null)
            list = new ArrayList<>();
        adapter = new Adapter(this, list, dm);
        recyclerView.setAdapter(adapter);

    }

    class DownLoadListener implements DownloadManagerListener {

        @Override
        public void OnDownloadStarted(long taskId) {
            Message msg = new Message();
            msg.what = TaskStates.READY;
            msg.arg1 = (int) taskId;
            mHandler.sendMessage(msg);
            Log.d("ssss" + taskId, "OnDownloadStarted");
        }

        @Override
        public void OnDownloadPaused(long taskId) {
            Message msg = new Message();
            msg.what = TaskStates.PAUSED;
            msg.arg1 = (int) taskId;
            mHandler.sendMessage(msg);
            Log.d("ssss" + taskId, "OnDownloadPaused");

        }

        @Override
        public void onDownloadProcess(long taskId, double percent, long downloadedLength) {
            Message msg = new Message();
            msg.what = TaskStates.DOWNLOADING;
            msg.arg1 = (int) taskId;
            Bundle bundle = new Bundle();
            bundle.putDouble("percent", percent);
            bundle.putLong("down_length", downloadedLength);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            Log.d("ssss" + taskId, "onDownloadProcess");

        }

        @Override
        public void OnDownloadFinished(long taskId) {
            Message msg = new Message();
            msg.what = TaskStates.DOWNLOAD_FINISHED;
            msg.arg1 = (int) taskId;
            mHandler.sendMessage(msg);
            Log.d("ssss" + taskId, "OnDownloadFinished");


        }

        @Override
        public void OnDownloadRebuildStart(long taskId) {
            Log.d("ssss" + taskId, "OnDownloadRebuildStart");

        }

        @Override
        public void OnDownloadRebuildFinished(long taskId) {
            Log.d("ssss" + taskId, "OnDownloadRebuildFinished");

        }

        @Override
        public void OnDownloadCompleted(long taskId) {
            Message msg = new Message();
            msg.what = TaskStates.END;
            msg.arg1 = (int) taskId;
            mHandler.sendMessage(msg);
            Log.d("ssss" + taskId, "OnDownloadCompleted");

        }

        @Override
        public void connectionLost(long taskId) {
            Log.d("ssss" + taskId, "connectionLost");

        }

    }
}
