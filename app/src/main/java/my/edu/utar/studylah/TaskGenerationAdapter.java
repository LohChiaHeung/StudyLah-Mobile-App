package my.edu.utar.studylah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class TaskGenerationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ShelfActivity context;
    private final List<Object> items;
    private final Set<PdfEntity> selectedPdfs;
    private final Set<Integer> expandedFolders = new HashSet<>();

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_PDF = 1;

    public TaskGenerationAdapter(ShelfActivity context, List<Object> items, Set<PdfEntity> selectedPdfs) {
        this.context = context;
        this.items = items;
        this.selectedPdfs = selectedPdfs;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof FolderEntity) {
            return TYPE_FOLDER;
        } else {
            return TYPE_PDF;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOLDER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_simple, parent, false);
            return new FolderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_checkbox, parent, false);
            return new PdfViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FolderViewHolder) {
            ((FolderViewHolder) holder).bind((FolderEntity) items.get(position));
        } else if (holder instanceof PdfViewHolder) {
            ((PdfViewHolder) holder).bind((PdfEntity) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        ImageView folderIcon, dropdownArrow;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderName);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            dropdownArrow = itemView.findViewById(R.id.dropdownArrow);
        }

        void bind(FolderEntity folder) {
            folderName.setText(folder.folderName);

            if (expandedFolders.contains(folder.id)) {
                dropdownArrow.setImageResource(R.drawable.ic_arrow_up);
            } else {
                dropdownArrow.setImageResource(R.drawable.ic_arrow_down);
            }

            itemView.setOnClickListener(v -> {
                if (expandedFolders.contains(folder.id)) {
                    collapseFolder(folder, getAdapterPosition());
                } else {
                    expandFolder(folder, getAdapterPosition());
                }
                notifyItemChanged(getAdapterPosition());
            });
        }
    }

    class PdfViewHolder extends RecyclerView.ViewHolder {
        CheckBox pdfCheckBox;

        PdfViewHolder(View itemView) {
            super(itemView);
            pdfCheckBox = itemView.findViewById(R.id.pdfCheckBox);
        }

        void bind(PdfEntity pdf) {
            pdfCheckBox.setText(pdf.getName());
            pdfCheckBox.setChecked(selectedPdfs.contains(pdf));

            pdfCheckBox.setOnCheckedChangeListener(null);
            pdfCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPdfs.add(pdf);
                } else {
                    selectedPdfs.remove(pdf);
                }
            });
        }
    }

    private void expandFolder(FolderEntity folder, int position) {
        List<Object> children = new ArrayList<>();
        children.addAll(getChildFolders(folder.id));
        children.addAll(getChildPdfs(folder.id));

        items.addAll(position + 1, children);
        expandedFolders.add(folder.id);
        notifyItemRangeInserted(position + 1, children.size());
    }

    private void collapseFolder(FolderEntity parentFolder, int position) {
        int i = position + 1;
        List<Object> toRemove = new ArrayList<>();

        while (i < items.size()) {
            Object item = items.get(i);

            if (item instanceof FolderEntity) {
                FolderEntity folder = (FolderEntity) item;
                if (folder.parentFolderId != null && folder.parentFolderId.equals(parentFolder.id)) {
                    // Recursive collapse inside this folder too
                    if (expandedFolders.contains(folder.id)) {
                        collapseFolder(folder, i);
                    }
                    toRemove.add(folder);
                    i++;
                } else {
                    break;
                }
            } else if (item instanceof PdfEntity) {
                PdfEntity pdf = (PdfEntity) item;
                if (pdf.getParentFolderId() != null && pdf.getParentFolderId().equals(parentFolder.id)) {
                    toRemove.add(pdf);
                    i++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        items.removeAll(toRemove);
        expandedFolders.remove(parentFolder.id);
        notifyItemRangeRemoved(position + 1, toRemove.size());
    }

    private List<FolderEntity> getChildFolders(int folderId) {
        return context.getChildFoldersFromDb(folderId);
    }

    private List<PdfEntity> getChildPdfs(int folderId) {
        return context.getChildPdfsFromDb(folderId);
    }
}
