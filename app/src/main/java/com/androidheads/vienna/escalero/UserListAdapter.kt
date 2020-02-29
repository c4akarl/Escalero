package com.androidheads.vienna.escalero

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


class UserListAdapter (private var context: Context, private var items: ArrayList<UserList>) :  BaseAdapter(){
    private class ViewHolder(row: View?) {
        var liInfo: TextView? = null
        var liPlayerName: TextView? = null
        var liScore: TextView? = null
        init {
            this.liInfo = row?.findViewById(R.id.liPlace)
            this.liPlayerName = row?.findViewById(R.id.liPlayer)
            this.liScore = row?.findViewById(R.id.liScore)
        }
    }

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.leaderboard_item, null)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val lb = items[position]
        viewHolder.liInfo?.text = lb.info

        viewHolder.liPlayerName?.text = lb.playerName
        if (lb.score >= 0)
            viewHolder.liScore?.text = lb.score.toString()
        else
            viewHolder.liScore?.text = ""

        return view
    }

    override fun getItem(i: Int): UserList {
        return items[i]
    }
    override fun getItemId(i: Int): Long {
        return i.toLong()
    }
    override fun getCount(): Int {
        return items.size
    }

    companion object {
        const val TAG = "UserListAdapter"
    }

}