package com.tagtoo.android;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;


public class MessagesAdapter  extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private Context mContext;

    private ArrayList<MainActivity.SavedMessage> messages;

    public MessagesAdapter(Context context, ArrayList<MainActivity.SavedMessage> list){
        this.mContext = context;
        messages = list;
    }

    public void setMessages(ArrayList<MainActivity.SavedMessage> list){
        this.messages = list;
    }

    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_savedmessage, viewGroup, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.MessageViewHolder holder, int position) {
        holder.display(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messagetw;
        private final TextView info;
        private final ImageButton deleteButton;

        private MainActivity.SavedMessage currentPair;

        public MessageViewHolder(final View itemView){
            super(itemView);
            messagetw    = itemView.findViewById(R.id.message);
            info         = itemView.findViewById(R.id.info);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    remove(getAdapterPosition());
                }
            });

        }

        public void display(MainActivity.SavedMessage savedMessage){
            currentPair = savedMessage;
            messagetw.setText(savedMessage.content);
            info.setText(savedMessage.dateSaved);
        }

        public void remove(int position){
            messages.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, messages.size());
            if(mContext instanceof MainActivity) {
                ((MainActivity) mContext).saveMessages(messages);
                ((MainActivity) mContext).setFragment(new HomeTabFragment());
            }

        }
    }


}
