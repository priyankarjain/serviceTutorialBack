package com.example.priya.servicetutorial;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


/**
 * Created by priya on 1/16/2017.
 */

public class RosterFragment extends Fragment{
    private RecyclerView recyclerView;
    private Adapter adapter;
    private SendMessageListener sendMessageListener;
    private ContactsData contactsData;
    private ChatDataSource dataSource;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            sendMessageListener = (SendMessageListener) context;
        }catch (ClassCastException c){
            throw new ClassCastException(context.toString()+"must");
        }
    }

    public void setDataSource(ChatDataSource dataSource){
        this.dataSource = dataSource;
    }

    public void setContactsData(ContactsData contactsData){
        this.contactsData = contactsData;
    }

    public void notifyAboutData(){
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }
    }

    public void addContacts(int prevCount,int newContacts){
        int adaptSize = adapter.getItemCount();
        adapter.notifyItemRangeInserted(prevCount,newContacts);
        recyclerView.scrollToPosition(adaptSize);
    }

    public void removeContact(int position){
        adapter.notifyItemRemoved(position);
    }

    public void availabilityChanged(int position){
        adapter.notifyItemChanged(position);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.roster_fragment,container,false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.chat_contacts);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        adapter = new MyAdapter();
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> payloads) {
                return true;
            }
        };
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(itemDecoration);
        Log.v("Fragment","Created");
        return rootView;
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tv_name;
            ImageView iv_presence;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_name = (TextView) itemView.findViewById(R.id.Name);
                iv_presence = (ImageView) itemView.findViewById(R.id.presence);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if(position!=RecyclerView.NO_POSITION){
                    String source = contactsData.contacts.get(position);
                    Log.v("Source",source);
                    sendMessageListener.onCreateChat(source);
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    ChatFragment chatFragment = new ChatFragment();
                    chatFragment.setSource(source);
                    chatFragment.setChatDataSource(dataSource);
                    fm.beginTransaction().
                            replace(R.id.activity_main,chatFragment,"chat:"+source)
                            .addToBackStack("chat:"+source).commit();
                }
            }
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row,parent,false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position, List<Object> payloads) {
            if(!payloads.isEmpty()){
                boolean available = (boolean) payloads.get(0);
                Log.v("Listened",available+"");
                if(available){
                    holder.iv_presence.setImageDrawable(getResources().getDrawable(R.drawable.online));
                }else{
                    holder.iv_presence.setImageDrawable(getResources().getDrawable(R.drawable.offline));
                }
            }else{
                super.onBindViewHolder(holder, position, payloads);
            }
        }

        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
            holder.tv_name.setText(contactsData.contacts.get(position).split("@")[0]);
            boolean available = contactsData.presences.get(contactsData.contacts.get(position));
            if(available){
                holder.iv_presence.setImageDrawable(getResources().getDrawable(R.drawable.online));
            }else{
                holder.iv_presence.setImageDrawable(getResources().getDrawable(R.drawable.offline));
            }
        }

        @Override
        public int getItemCount() {
            return contactsData.contacts.size();
        }
    }
}
