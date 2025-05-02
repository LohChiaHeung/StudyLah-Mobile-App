package my.edu.utar.studylah;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
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
    private Context context;

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
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

            // Handle long click for folders
            viewHolder.itemView.setOnLongClickListener(v -> {
                showFolderOptionsMenu(v, v.getContext(), folder);
                return true;  // Long click handled
            });

        } else {
            // PDF
            PdfEntity pdf = pdfs.get(position - folders.size());
            viewHolder.folderName.setText(pdf.getName());

            // Try to load PDF thumbnail
            try {
                loadPdfThumbnail(pdf, viewHolder.folderIcon);
            } catch (Exception e) {
                // Fallback to default PDF icon if thumbnail generation fails
                viewHolder.folderIcon.setImageResource(R.drawable.pdf_icon);
                e.printStackTrace();
            }

            viewHolder.itemView.setOnClickListener(v -> onPdfClick.accept(pdf));

            // Handle long click for PDFs
            viewHolder.itemView.setOnLongClickListener(v -> {
                showPdfOptionsMenu(v, v.getContext(), pdf);
                return true;  // Long click handled
            });
        }
    }

    // Method to load PDF thumbnail
    private void loadPdfThumbnail(PdfEntity pdf, ImageView imageView) {
        try {
            Uri pdfUri = Uri.parse(pdf.getUri());

            // Create a temporary file from the Uri
            File pdfFile = copyFileFromUri(context, pdfUri);

            // Open the PDF file
            ParcelFileDescriptor fileDescriptor =
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);

            // Create a PdfRenderer
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);

            // Get the first page
            if (renderer.getPageCount() > 0) {
                PdfRenderer.Page page = renderer.openPage(0);

                // Create a bitmap with the page dimensions
                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888);

                // Render the page to the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                // Set the bitmap to the ImageView
                imageView.setImageBitmap(bitmap);

                // Close the page and renderer
                page.close();
                renderer.close();
            } else {
                // Fallback for empty PDFs
                imageView.setImageResource(R.drawable.pdf_icon);
            }

            fileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to default icon on error
            imageView.setImageResource(R.drawable.pdf_icon);
        }
    }

    // Show popup menu for folder options
    private void showFolderOptionsMenu(View view, Context context, FolderEntity folder) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.folder_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_rename) {
                showEditFolderNameDialog(context, folder);
                return true;
            } else if (id == R.id.menu_delete) {
                showDeleteFolderDialog(context, folder);
                return true;
            }
            return false;
        });

        popup.show();
    }

    // Show popup menu for PDF options
    private void showPdfOptionsMenu(View view, Context context, PdfEntity pdf) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.pdf_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_rename) {
                showEditPdfNameDialog(context, pdf);
                return true;
            } else if (id == R.id.menu_delete) {
                showDeletePdfDialog(context, pdf);
                return true;
            }
            return false;
        });

        popup.show();
    }

    // Show Edit Folder Name Dialog
    private void showEditFolderNameDialog(Context context, FolderEntity folder) {
        final EditText input = new EditText(context);
        input.setText(folder.folderName);
        input.setSelection(folder.folderName.length());
        input.setHint("Enter new folder name");

        new AlertDialog.Builder(context)
                .setTitle("Edit Folder Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        folder.folderName = newName;
                        if (context instanceof ShelfActivity) {
                            ((ShelfActivity) context).updateFolderName(folder); // Update folder name in ShelfActivity
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Show Edit PDF Name Dialog
    private void showEditPdfNameDialog(Context context, PdfEntity pdf) {
        final EditText input = new EditText(context);
        input.setText(pdf.getName());
        input.setSelection(pdf.getName().length());
        input.setHint("Enter new PDF name");

        new AlertDialog.Builder(context)
                .setTitle("Edit PDF Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        pdf.setName(newName);
                        if (context instanceof ShelfActivity) {
                            ((ShelfActivity) context).updatePdfName(pdf); // Update PDF name in ShelfActivity
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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