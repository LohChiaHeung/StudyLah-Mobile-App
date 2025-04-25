package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;

import androidx.recyclerview.widget.RecyclerView;

import java.io.FileOutputStream;
import java.io.InputStream;
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
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FolderViewHolder viewHolder = (FolderViewHolder) holder;

        if (position < folders.size()) {
            // FOLDER
            FolderEntity folder = folders.get(position);
            viewHolder.folderName.setText(folder.folderName);
            viewHolder.folderIcon.setImageResource(R.drawable.folder);
            viewHolder.itemView.setOnClickListener(v -> onFolderClick.accept(folder));
            viewHolder.itemView.setOnLongClickListener(v -> {
                showDeleteFolderDialog(v.getContext(), folder);
                return true;
            });

        } else {
            // PDF
            PdfEntity pdf = pdfs.get(position - folders.size());
            viewHolder.folderName.setText(pdf.getName());

            try {
                Uri uri = Uri.parse(pdf.getUri());
                File file = copyFileFromUri(holder.itemView.getContext(), uri);
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
                PdfRenderer.Page page = pdfRenderer.openPage(0);

                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                viewHolder.folderIcon.setImageBitmap(bitmap);

                page.close();
                pdfRenderer.close();
                fileDescriptor.close();
            } catch (Exception e) {
                viewHolder.folderIcon.setImageResource(R.drawable.pdf_icon); // fallback icon
            }

            viewHolder.itemView.setOnClickListener(v -> onPdfClick.accept(pdf));
            viewHolder.itemView.setOnLongClickListener(v -> {
                showDeletePdfDialog(v.getContext(), pdf);
                return true;
            });
        }
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        ImageView folderIcon;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderName);
            folderIcon = itemView.findViewById(R.id.folderIcon);
        }
    }

    private File copyFileFromUri(Context context, Uri uri) throws Exception {
        InputStream input = context.getContentResolver().openInputStream(uri);
        File outputFile = new File(context.getCacheDir(), "preview_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream output = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
        }

        input.close();
        output.close();
        return outputFile;
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


