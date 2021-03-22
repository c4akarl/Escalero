/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2021  Karl Schreiner, c4akarl@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.androidheads.vienna.escalero

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView

// custom match UI ( ListView, dialogmatch.xml )
class MatchListAdapter (private var activity: Activity, private var items: ArrayList<MatchList>) :  BaseAdapter(){
    private class ViewHolder(row: View?) {
        var miSelected: CheckBox? = null
        var miPlayers: TextView? = null
        var miStatus: TextView? = null
        var miTime: TextView? = null
        var miVariant: TextView? = null
        init {
            this.miSelected = row?.findViewById(R.id.miSelected)
            this.miPlayers = row?.findViewById(R.id.miPlayers)
            this.miStatus = row?.findViewById(R.id.miStatus)
            this.miTime = row?.findViewById(R.id.miTime)
            this.miVariant = row?.findViewById(R.id.miVariant)
        }
    }
    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.match_item, null)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val match = items[position]
        viewHolder.miSelected?.isChecked = match.selected
        viewHolder.miPlayers?.text = match.players
        viewHolder.miStatus?.text = match.status
        viewHolder.miTime?.text = match.time
        viewHolder.miVariant?.text = match.variant

        return view
    }
    override fun getItem(i: Int): MatchList {
        return items[i]
    }
    override fun getItemId(i: Int): Long {
        return i.toLong()
    }
    override fun getCount(): Int {
        return items.size
    }
}