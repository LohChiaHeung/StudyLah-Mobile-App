package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class FolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<FolderEntity> folders;
    private List<PdfEntity> pdfs;
    private Consumer<FolderEntity> onFolderClick;

    private Consumer<PdfEntity> onPdfClick;

    public FolderAdapter(List<FolderEntity> folders, List<PdfEntity> pdfs,
                         Consumer<FolderEntity> onFolderClick,
                         Consumer<PdfEntity> onPdfClick) {
        this.folders = folders;
        this.pdfs = pdfs;
        this.onFolderClick = onFolderClick;
        this.onPdfClick = onPdfClick;
    }

    @Override
    public int getItemCount() {
        return folders.size() + pdfs.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position < folders.size() ? 0 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TextView tv = (TextView) holder.itemView;
        if (position < folders.size()) {
            FolderEntity folder = folders.get(position);
            tv.setText("[Folder] " + folder.folderName);
            tv.setOnClickListener(v -> onFolderClick.accept(folder));
            tv.setOnLongClickListener(v -> {
                showDeleteFolderDialog(tv.getContext(), folder);
                return true;
            });

        } else {
            PdfEntity pdf = pdfs.get(position - folders.size());
            tv.setText(pdf.getName());
            tv.setOnClickListener(v -> onPdfClick.accept(pdf));
            tv.setOnLongClickListener(v -> {
                showDeletePdfDialog(tv.getContext(), pdf);
                return true;
            });
            // 📌 New click
        }
    }

    public void updateData(List<FolderEntity> folders, List<PdfEntity> pdfs) {
        this.folders = folders;
        this.pdfs = pdfs;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View view) {
            super(view);
        }
    }

    private void showDeleteFolderDialog(Context context, FolderEntity folder) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Folder")
                .setMessage("Delete folder and all PDFs inside?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (context instanceof ShelfActivity) {
                        ((ShelfActivity) context).deleteFolder(folder);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeletePdfDialog(Context context, PdfEntity pdf) {
        new AlertDialog.Builder(context)
                .setTitle("Delete PDF")
                .setMessage("Are you sure you want to delete this PDF?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (context instanceof ShelfActivity) {
                        ((ShelfActivity) context).deletePdf(pdf);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}


