package my.edu.utar.studylah;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<PdfEntity> searchResults;
    private final OnPdfClickListener clickListener;

    public interface OnPdfClickListener {
        void onPdfClick(PdfEntity pdf);
    }

    public SearchResultAdapter(List<PdfEntity> searchResults, OnPdfClickListener clickListener) {
        this.searchResults = searchResults;
        this.clickListener = clickListener;
    }

    public void updateResults(List<PdfEntity> newResults) {
        this.searchResults = newResults;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextSize(16f);
        tv.setPadding(32, 24, 32, 24);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PdfEntity pdf = searchResults.get(position);
        ((TextView) holder.itemView).setText("📄 " + pdf.getName());
        holder.itemView.setOnClickListener(v -> clickListener.onPdfClick(pdf));
    }

    @Override
    public int getItemCount() {
        return searchResults != null ? searchResults.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) { super(itemView); }
    }
}

