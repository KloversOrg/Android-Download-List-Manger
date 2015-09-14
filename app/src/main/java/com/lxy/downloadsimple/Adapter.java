package com.lxy.downloadsimple;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.golshadi.majid.core.DownloadManagerPro;
import com.golshadi.majid.core.enums.TaskStates;
import com.golshadi.majid.report.ReportStructure;

import java.io.IOException;
import java.util.List;

/**
 * Created by homelajiang on 2015/9/10 0010.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.viewHolder> {

    private final Context context;
    private final DownloadManagerPro dm;
    public List<ReportStructure> datas;

    Adapter(Context context, List<ReportStructure> datas, DownloadManagerPro dm) {
        this.dm = dm;
        this.context = context;
        this.datas = datas;
    }

    @Override
    public viewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new viewHolder(View.inflate(parent.getContext(), R.layout.item_download, null));
    }

    @Override
    public void onBindViewHolder(final viewHolder holder, final int position) {
        final ReportStructure structure = dm.downloadTasksInSameState(7).get(position);
//        final ReportStructure structure = datas.get(position);

        if (structure == null)
            return;
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
        String state = "未知";
        String downLoadSize = "0";
        String totalSize = MainActivity.mDecimalFormat.format(structure.getFileSize()/1024/1024)+"M";

        switch (structure.getState()) {
            case 0:
                state = "准备完成";
                break;
            case 1:
                state ="正在开始...";
                break;
            case 2:
                state ="正在下载";
                downLoadSize=MainActivity.mDecimalFormat.format(structure.getDownloadLength()/1024/1024);
                holder.pb.setProgress((int) structure.getPercent());
                break;
            case 3:
                state ="暂停";
                downLoadSize=MainActivity.mDecimalFormat.format(structure.getDownloadLength()/1024/1024);
                holder.pb.setProgress((int) structure.getPercent());
                break;
            case 4:
                state ="下载成功,正在合成";
                downLoadSize=MainActivity.mDecimalFormat.format(structure.getFileSize()/1024/1024);
                holder.pb.setProgress(100);
                break;
            case 5:
                state ="下载完成";
                downLoadSize=MainActivity.mDecimalFormat.format(structure.getFileSize()/1024/1024);
                holder.pb.setProgress(100);
                break;
            default:
                break;
        }
        holder.state.setText(state);
        holder.file_name.setText(structure.getName() + "." + structure.getType());
        holder.down_size.setText(downLoadSize);
        holder.total_size.setText(totalSize);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportStructure structure1 = dm.singleDownloadStatus(structure.getId());
                int state = structure1.getState();
                if (state == TaskStates.READY || state == TaskStates.PAUSED || state == TaskStates.INIT)
                    try {
                        dm.startDownload(structure1.getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                if (state == TaskStates.DOWNLOADING)
                    dm.pauseDownload(structure1.getId());

                if (state == TaskStates.END) {
                    Toast.makeText(context, structure1.getSaveAddress(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //删除任务和源文件
                notifyItemRemoved(position);
                dm.delete(datas.get(position).getId(),true);//删除下载记录和文件
//                datas.remove(position);//更新数据
                datas = dm.downloadTasksInSameState(7);
                notifyItemRangeChanged(position,datas.size());
                return true;
            }
        });
    }

    public void update(List<ReportStructure> datas) {
        this.datas = datas;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        private final TextView down_size;
        private final TextView total_size;
        private final ProgressBar pb;
        private final TextView state;
        private final TextView file_name;

        public viewHolder(final View itemView) {
            super(itemView);
            file_name = (TextView) itemView.findViewById(R.id.textView2);
            state = (TextView) itemView.findViewById(R.id.textView);
            total_size = (TextView) itemView.findViewById(R.id.textView3);
            down_size = (TextView) itemView.findViewById(R.id.textView5);
            pb = (ProgressBar) itemView.findViewById(R.id.progressBar);
        }
    }
}
