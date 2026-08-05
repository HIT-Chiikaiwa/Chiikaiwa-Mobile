package com.example.myapplication.ui.home.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ScheduleAdapter(
    private var bookings: List<BookingDto> = emptyList(),
    private val onItemClick: ((BookingDto) -> Unit)? = null
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    fun submitList(newList: List<BookingDto>) {
        bookings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class ScheduleViewHolder(private val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: BookingDto) {
            val timeText = formatTimeRange(booking.scheduledAt, booking.durationMinutes ?: 60)
            binding.tvTime.text = timeText

            val title = booking.subject ?: "Cuộc hẹn"
            val location = booking.locationName
            val desc = if (!location.isNullOrEmpty()) "$title • $location" else title
            binding.tvTaskDescription.text = desc

            binding.imgItemIcon.setImageResource(R.drawable.ic_schedule_bean)

            binding.root.setOnClickListener {
                onItemClick?.invoke(booking)
            }
        }

        private fun formatTimeRange(scheduledAt: String?, durationMinutes: Int): String {
            if (scheduledAt.isNullOrEmpty()) return "Chưa xếp giờ"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(scheduledAt) ?: return scheduledAt
                val calendar = Calendar.getInstance().apply { time = date }

                val startHour = calendar.get(Calendar.HOUR_OF_DAY)
                val startMinute = calendar.get(Calendar.MINUTE)

                calendar.add(Calendar.MINUTE, durationMinutes)
                val endHour = calendar.get(Calendar.HOUR_OF_DAY)
                val endMinute = calendar.get(Calendar.MINUTE)

                String.format(Locale.getDefault(), "%02dh%02d - %02dh%02d", startHour, startMinute, endHour, endMinute)
            } catch (e: Exception) {
                scheduledAt
            }
        }
    }
}
