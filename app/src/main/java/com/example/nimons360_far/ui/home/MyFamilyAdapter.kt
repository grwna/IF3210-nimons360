package com.example.nimons360_far.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.databinding.ItemHomeMyFamilyBinding

class MyFamilyAdapter(
    private var families: List<Family>,
    private val onItemClick: (Family) -> Unit
) : RecyclerView.Adapter<MyFamilyAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHomeMyFamilyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(family: Family) {
            binding.tvFamilyName.text = family.name
            binding.tvMemberCount.text = "${family.members?.size ?: 0} members"
            
            // icon
            binding.ivFamilyIcon.load(family.iconUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_myplaces)
            }

            // populate avatars
            MemberAvatarHelper.populateAvatarStack(
                binding.root.context,
                binding.avatarContainer,
                family.members
            )

            binding.root.setOnClickListener { onItemClick(family) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeMyFamilyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(families[position])
    }

    override fun getItemCount() = families.size

    fun updateFamilies(newFamilies: List<Family>) {
        families = newFamilies
        notifyDataSetChanged()
    }
}
